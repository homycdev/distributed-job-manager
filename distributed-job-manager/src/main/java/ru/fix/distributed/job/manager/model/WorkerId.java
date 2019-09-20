package ru.fix.distributed.job.manager.model;

import java.util.Objects;

public class WorkerId {

    private String id;

    public WorkerId(String id) {
        Objects.requireNonNull(id);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkerId that = (WorkerId) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "WorkerItem{" +
                "id='" + id + '\'' +
                '}';
    }
}
