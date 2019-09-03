package ru.fix.distributed.job.manager;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.aggregating.profiler.PrefixedProfiler;
import ru.fix.aggregating.profiler.Profiler;
import ru.fix.distributed.job.manager.strategy.AssignmentStrategy;
import ru.fix.dynamic.property.api.DynamicProperty;

import java.util.Collection;

/**
 * <p>
 * How to use: <br>
 * Create single instance of {@link DistributedJobManager} for each server (JVM instances).
 * In {@link DistributedJobManager#DistributedJobManager(
 * String, CuratorFramework, String, Collection, AssignmentStrategy, Profiler, DynamicProperty, String)}
 * register list
 * of jobs that could be run on this server (JVM instance). {@link DistributedJobManager} will balance workload between
 * available servers for you.
 * </p>
 * <p>
 * Each node instance register as worker and provide resources to run Jobs. <br>
 * One of node will be selected as leader and became Manager. Manager controls job assignment. Node with Manager also
 * starts local Worker so Node can work as worker and as a manager.
 * <br>
 * Every worker provide unique id and register as child node at /workers <br>
 * Every worker register available jobs classes that it can run in /workers/worker-id/available/work-pooled/job-id <br>
 * All workers should register same SchedulableJobs.
 * Avery worker listen to /workers/id/assigned/work-pooled. New schedulable job will be added there by Manager. <br>
 * When new assigned job appears, worker acquire lock /jobs/work-pooled/job-id.lock and start launching it with given
 * delay.
 * When job disappears from worker assigned/work-pooled path, worker stop executing job and release job lock.
 * </p>
 * <p>
 * ZK node tree managed by {@link DistributedJobManager} described in {@link JobManagerPaths}
 * <pre>
 *
 * @author Kamil Asfandiyarov
 */
public class DistributedJobManager implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(DistributedJobManager.class);

    private final Worker worker;
    private final Manager manager;
    private String applicationId;

    private static class Timespan {
        long startTimestamp;
        long stopTimestamp;

        public Timespan start() {
            startTimestamp = System.currentTimeMillis();
            return this;
        }

        public Timespan stop() {
            stopTimestamp = System.currentTimeMillis();
            return this;
        }

        public long getTimespan() {
            return stopTimestamp - startTimestamp;
        }
    }

    @SuppressWarnings("squid:S3776")
    public DistributedJobManager(String applicationId,
                                 CuratorFramework curatorFramework,
                                 String rootPath,
                                 Collection<DistributedJob> repeatableJobs,
                                 AssignmentStrategy assignmentStrategy,
                                 Profiler profiler,
                                 DynamicProperty<Long> timeToWaitTermination) throws Exception {
        this(applicationId,
                curatorFramework,
                rootPath,
                repeatableJobs,
                assignmentStrategy,
                profiler,
                timeToWaitTermination,
                DynamicProperty.of(false));
    }

    @SuppressWarnings("squid:S3776")
    public DistributedJobManager(String applicationId,
                                 CuratorFramework curatorFramework,
                                 String rootPath,
                                 Collection<DistributedJob> distributedJobs,
                                 AssignmentStrategy assignmentStrategy,
                                 Profiler profiler,
                                 DynamicProperty<Long> timeToWaitTermination,
                                 DynamicProperty<Boolean> printTree) throws Exception {

        final Timespan djmInitTimespan = new Timespan().start();

        log.trace("Starting DistributedJobManager for applicationId {} with zk-path {}",
                applicationId, rootPath);

        initPaths(curatorFramework, rootPath);

        final Timespan managerInitTimespan = new Timespan().start();
        this.manager = new Manager(curatorFramework, rootPath, assignmentStrategy, applicationId, profiler, printTree);
        managerInitTimespan.stop();


        final Timespan workerInitTimespan = new Timespan().start();

        this.applicationId = applicationId;

        this.worker = new Worker(
                curatorFramework,
                applicationId,
                rootPath,
                distributedJobs,
                new PrefixedProfiler(profiler, "djm."),
                timeToWaitTermination,
                applicationId,
                printTree);

        workerInitTimespan.stop();


        final Timespan managerStartTimespan = new Timespan().start();
        this.manager.start();
        managerStartTimespan.stop();

        final Timespan workerStartTimespan = new Timespan().start();
        this.worker.start();

        workerStartTimespan.stop();

        djmInitTimespan.stop();

        log.info("DJM initialized in {}ms, Manager init in {}ms, started in {}ms, Worker init in {}ms, started in {}ms",
                djmInitTimespan.getTimespan(),
                managerInitTimespan.getTimespan(),
                managerStartTimespan.getTimespan(),
                workerInitTimespan.getTimespan(),
                workerStartTimespan.getTimespan());
    }

    private static void initPaths(CuratorFramework curatorFramework, String rootPath) throws Exception {
        JobManagerPaths paths = new JobManagerPaths(rootPath);
        if (curatorFramework.checkExists().forPath(paths.getWorkersPath()) == null) {
            curatorFramework.create().creatingParentsIfNeeded().forPath(paths.getWorkersPath());
        }
        if (curatorFramework.checkExists().forPath(paths.getWorkersAlivePath()) == null) {
            curatorFramework.create().creatingParentsIfNeeded().forPath(paths.getWorkersAlivePath());
        }
        if (curatorFramework.checkExists().forPath(paths.getRegistrationVersion()) == null) {
            curatorFramework.create().creatingParentsIfNeeded().forPath(paths.getRegistrationVersion());
        }
        if (curatorFramework.checkExists().forPath(paths.getAssignmentVersion()) == null) {
            curatorFramework.create().creatingParentsIfNeeded().forPath(paths.getAssignmentVersion());
        }
        if (curatorFramework.checkExists().forPath(paths.getWorkPooledLocksPath()) == null) {
            curatorFramework.create().creatingParentsIfNeeded().forPath(paths.getWorkPooledLocksPath());
        }
    }

    @Override
    public void close() throws Exception {
        log.info("Closing DJM with worker id {}", applicationId);

        Timespan djmClosing = new Timespan().start();

        Timespan workerClosing = new Timespan().start();
        worker.close();
        workerClosing.stop();

        Timespan managerClosing = new Timespan().start();
        manager.close();
        managerClosing.stop();

        djmClosing.stop();

        log.info("DJM closed in {}ms, Worker {} closed in {}ms, Manager closed in {}ms",
                djmClosing.getTimespan(),
                applicationId,
                workerClosing.getTimespan(),
                managerClosing.getTimespan());
    }
}
