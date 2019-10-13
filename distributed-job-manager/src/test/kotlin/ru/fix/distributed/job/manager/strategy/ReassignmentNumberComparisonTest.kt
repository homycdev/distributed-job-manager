package ru.fix.distributed.job.manager.strategy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.platform.commons.logging.Logger
import org.junit.platform.commons.logging.LoggerFactory
import ru.fix.distributed.job.manager.model.AssignmentState
import ru.fix.distributed.job.manager.model.JobId
import ru.fix.distributed.job.manager.model.WorkItem
import ru.fix.distributed.job.manager.model.WorkerId

internal class ReassignmentNumberComparisonTest {
    private var evenlySpread: EvenlySpreadAssignmentStrategy? = null
    private var rendezvous: RendezvousHashAssignmentStrategy? = null

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ReassignmentNumberComparisonTest::class.java)
    }

    @BeforeEach
    fun setUp() {
        evenlySpread = EvenlySpreadAssignmentStrategy()
        rendezvous = RendezvousHashAssignmentStrategy()
    }

    private class Results(
            internal val evenlySpreadReassignmentNumber: Int,
            internal val rendezvousReassignmentNumber: Int
    )

    @Test
    fun balanceItemsOfSingleJobBetweenTwoWorkers() {
        val workPool: JobScope.() -> Unit = {
            "job-0"(
                    "work-item-0",
                    "work-item-1",
                    "work-item-2",
                    "work-item-3"
            )
        }
        val available = assignmentState {
            "worker-0"(workPool)
            "worker-1"(workPool)
        }

        val previous = assignmentState {
            "worker-0"{
                "job-0"(
                        "work-item-0",
                        "work-item-1",
                        "work-item-2"
                )
            }
            "worker-1"{
                "job-0"(
                        "work-item-3"
                )
            }
        }

        val results = reassignmentResults(available, previous)
        assertEquals(1, results.evenlySpreadReassignmentNumber)
        assertEquals(2, results.rendezvousReassignmentNumber)
    }

    @Test
    fun balanceItemsOfSingleJobBetweenThreeWorkers() {
        val workPool: JobScope.() -> Unit = {
            "job-0"(
                    "work-item-0",
                    "work-item-1",
                    "work-item-2",
                    "work-item-3"
            )
        }
        val available = assignmentState {
            "worker-0"(workPool)
            "worker-1"(workPool)
            "worker-2"(workPool)
        }

        val previous = assignmentState {
            "worker-0"{
                "job-0"(
                        "work-item-0",
                        "work-item-1",
                        "work-item-2"
                )
            }
            "worker-1"{
                "job-0"(
                        "work-item-3"
                )
            }
        }

        val results = reassignmentResults(available, previous)
        assertEquals(1, results.evenlySpreadReassignmentNumber)
        assertEquals(2, results.rendezvousReassignmentNumber)
    }

    @Test
    fun balanceAlreadyBalancedItems() {
        val workPool: JobScope.() -> Unit = {
            "job-0"(
                    "work-item-0",
                    "work-item-1",
                    "work-item-2",
                    "work-item-3",
                    "work-item-4",
                    "work-item-5"
            )
        }
        val available = assignmentState {
            "worker-0"(workPool)
            "worker-1"(workPool)
            "worker-2"(workPool)
        }

        val previous = assignmentState {
            "worker-0"{
                "job-0"(
                        "work-item-0",
                        "work-item-1"
                )
            }
            "worker-1"{
                "job-0"(
                        "work-item-2",
                        "work-item-3"
                )
            }
            "worker-2"{
                "job-0"(
                        "work-item-4",
                        "work-item-5"
                )
            }
        }

        val results = reassignmentResults(available, previous)
        assertEquals(0, results.evenlySpreadReassignmentNumber)
        assertEquals(2, results.rendezvousReassignmentNumber)
    }

    @Test
    fun balanceItemsWhenWorkItemsOfJobNotBalanced() {
        val workPool: JobScope.() -> Unit = {
            "job-0"(
                    "work-item-0",
                    "work-item-1"
            )
            "job-1"(
                    "work-item-2",
                    "work-item-3",
                    "work-item-4",
                    "work-item-5"
            )
        }
        val available = assignmentState {
            "worker-0"(workPool)
            "worker-1"(workPool)
            "worker-2"(workPool)
        }

        val previous = assignmentState {
            "worker-0"{
                "job-0"(
                        "work-item-0",
                        "work-item-1"
                )
            }
            "worker-1"{
                "job-1"(
                        "work-item-2",
                        "work-item-3"
                )
            }
            "worker-2"{
                "job-1"(
                        "work-item-4",
                        "work-item-5"
                )
            }
        }

        val results = reassignmentResults(available, previous)
        assertEquals(2, results.evenlySpreadReassignmentNumber)
        assertEquals(3, results.rendezvousReassignmentNumber)
    }

    @Test
    fun balanceItemsOfThreeJobsWhenNewWorkerStarted() {
        val workPool: JobScope.() -> Unit = {
            "job-0"(
                    "work-item-0"
            )
            "job-1"(
                    "work-item-1",
                    "work-item-2",
                    "work-item-3",
                    "work-item-4",
                    "work-item-5",
                    "work-item-6"
            )
            "job-2"(
                    "work-item-7",
                    "work-item-8"
            )
        }
        val available = assignmentState {
            "worker-0"(workPool)
            "worker-1"(workPool)
            "worker-2"(workPool)
        }

        val previous = assignmentState {
            "worker-0"{
                "job-1"(
                        "work-item-1",
                        "work-item-2",
                        "work-item-3"
                )
                "job-2"(
                        "work-item-7"
                )
            }
            "worker-1"{
                "job-0"(
                        "work-item-0"
                )
                "job-1"(
                        "work-item-4",
                        "work-item-5",
                        "work-item-6"
                )
                "job-2"(
                        "work-item-8"
                )
            }
        }

        val results = reassignmentResults(available, previous)
        assertEquals(3, results.evenlySpreadReassignmentNumber)
        assertEquals(5, results.rendezvousReassignmentNumber)
    }

    @Test
    fun balanceItemsOfFourJobsWhenNewWorkerStarted() {
        val workPool: JobScope.() -> Unit = {
            "job-0"(
                    "work-item-0",
                    "work-item-1"
            )
            "job-1"(
                    "work-item-2",
                    "work-item-3"
            )
            "job-2"(
                    "work-item-4",
                    "work-item-5"
            )
            "job-3"(
                    "work-item-6",
                    "work-item-7"
            )
        }
        val available = assignmentState {
            "worker-0"(workPool)
            "worker-1"(workPool)
            "worker-2"(workPool)
        }

        val previous = assignmentState {
            "worker-0"{
                "job-0"(
                        "work-item-0"
                )
                "job-1"(
                        "work-item-2"
                )
                "job-2"(
                        "work-item-4"
                )
                "job-3"(
                        "work-item-6"
                )
            }
            "worker-1"{
                "job-0"(
                        "work-item-1"
                        )
                "job-1"(
                        "work-item-3"
                )
                "job-2"(
                        "work-item-5"
                )
                "job-3"(
                        "work-item-7"
                )
            }
        }

        val results = reassignmentResults(available, previous)
        assertEquals(2, results.evenlySpreadReassignmentNumber)
        assertEquals(6, results.rendezvousReassignmentNumber)
    }

    @Test
    fun balanceItemsOfFourJobsWhenWasAliveSingleWorkerAndNewWorkerStarted() {
        val workPool: JobScope.() -> Unit = {
            "job-0"(
                    "work-item-0",
                    "work-item-1"
            )
            "job-1"(
                    "work-item-2",
                    "work-item-3"
            )
            "job-2"(
                    "work-item-4",
                    "work-item-5"
            )
            "job-3"(
                    "work-item-6",
                    "work-item-7"
            )
        }
        val available = assignmentState {
            "worker-0"(workPool)
            "worker-1"(workPool)
        }

        val previous = assignmentState {
            "worker-0"(workPool)
        }

        val results = reassignmentResults(available, previous)
        assertEquals(4, results.evenlySpreadReassignmentNumber)
        assertEquals(5, results.rendezvousReassignmentNumber)
    }

    @Test
    fun balanceItemsOfFourJobsWhenWasAliveSingleWorkerAndNewFiveWorkersStarted() {
        val workPool: JobScope.() -> Unit = {
            "job-0"(
                    "work-item-0",
                    "work-item-1",
                    "work-item-2",
                    "work-item-3",
                    "work-item-4"
            )
            "job-1"(
                    "work-item-5",
                    "work-item-6",
                    "work-item-7",
                    "work-item-8",
                    "work-item-9"
            )
            "job-2"(
                    "work-item-10"
            )
            "job-3"(
                    "work-item-11",
                    "work-item-12"
            )
        }
        val available = assignmentState {
            "worker-0"(workPool)
            "worker-1"(workPool)
            "worker-2"(workPool)
            "worker-3"(workPool)
            "worker-4"(workPool)
            "worker-5"(workPool)
        }

        val previous = assignmentState {
            "worker-0"(workPool)
        }

        val results = reassignmentResults(available, previous)
        assertEquals(10, results.evenlySpreadReassignmentNumber)
        assertEquals(10, results.rendezvousReassignmentNumber)
    }


    @Test
    fun balanceItemsOfSingleJobWhenWorkerDestroyed() {
        val workPool: JobScope.() -> Unit = {
            "job-1"(
                    "work-item-0",
                    "work-item-1",
                    "work-item-2",
                    "work-item-3",
                    "work-item-4",
                    "work-item-5"
            )
        }
        val available = assignmentState {
            "worker-0"(workPool)
            "worker-1"(workPool)
        }

        val previous = assignmentState {
            "worker-0"{
                "job-1"(
                        "work-item-0",
                        "work-item-1"
                )
            }
            "worker-1"{
                "job-1"(
                        "work-item-2",
                        "work-item-3"
                )
            }
            "worker-2"{
                "job-1"(
                        "work-item-4",
                        "work-item-5"
                )
            }
        }

        val results = reassignmentResults(available, previous)
        assertEquals(2, results.evenlySpreadReassignmentNumber)
        assertEquals(4, results.rendezvousReassignmentNumber)
    }

    @Test
    fun balanceItemsOfSomeJobWhenWasAliveThreeWorkersAndOneWorkerDestroyed() {
        val workPool: JobScope.() -> Unit = {
            "job-0"(
                    "work-item-0",
                    "work-item-1",
                    "work-item-2"
            )
            "job-1"(
                    "work-item-3",
                    "work-item-4",
                    "work-item-5",
                    "work-item-6"
            )
            "job-2" (
                    "work-item-7"
            )
            "job-3"(
                    "work-item-8",
                    "work-item-9",
                    "work-item-10"
            )
        }
        val available = assignmentState {
            "worker-0"(workPool)
            "worker-1"(workPool)
        }

        val previous = assignmentState {
            "worker-0" {
                "job-0"(
                        "work-item-0"
                )
                "job-1"(
                        "work-item-3",
                        "work-item-6"
                )
                "job-3"(
                        "work-item-9"
                )
            }
            "worker-1" {
                "job-0"(
                        "work-item-1"
                )
                "job-1"(
                        "work-item-4"
                )
                "job-2" (
                        "work-item-7"
                )
                "job-3"(
                        "work-item-9"
                )
            }
            "worker-2" {
                "job-0"(
                        "work-item-2"
                )
                "job-1"(
                        "work-item-5"
                )
                "job-3"(
                        "work-item-8"
                )
            }
        }

        val results = reassignmentResults(available, previous)
        assertEquals(4, results.evenlySpreadReassignmentNumber)
        assertEquals(10, results.rendezvousReassignmentNumber)
    }

    private fun reassignmentResults(available: AssignmentState, previous: AssignmentState): Results {
        val availability = generateAvailability(available)
        val itemsToAssign = generateItemsToAssign(available)

        logger.info {
            Print.Builder()
                    .availability(availability)
                    .itemsToAssign(itemsToAssign)
                    .previousAssignment(previous)
                    .build().toString()
        }

        val newAssignmentEvenlySpread = evenlySpread!!.reassignAndBalance(
                availability,
                previous,
                AssignmentState(),
                itemsToAssign
        )
        val newAssignmentRendezvous = rendezvous!!.reassignAndBalance(
                availability,
                previous,
                AssignmentState(),
                generateItemsToAssign(available)
        )
        logger.info {
            Print.Builder()
                    .evenlySpreadNewAssignment(newAssignmentEvenlySpread)
                    .rendezvousNewAssignment(newAssignmentRendezvous)
                    .build().toString()
        }
        return Results(
                calculateReassignments(previous, newAssignmentEvenlySpread),
                calculateReassignments(previous, newAssignmentRendezvous)
        )
    }
}
