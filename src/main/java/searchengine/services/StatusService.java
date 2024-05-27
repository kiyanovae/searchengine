package searchengine.services;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class StatusService {
    private volatile boolean isIndexingRunning;
    private volatile boolean isIndexingStoppedByUser;
    private volatile boolean isAdditionalTasksStoppedByIndexing;
    private final AtomicInteger additionalTaskCount;
    private final AtomicInteger taskCount;

    public StatusService() {
        isIndexingRunning = false;
        isIndexingStoppedByUser = false;
        isAdditionalTasksStoppedByIndexing = false;
        additionalTaskCount = new AtomicInteger();
        taskCount = new AtomicInteger();
    }

    public boolean isIndexingRunning() {
        return isIndexingRunning;
    }

    public void setIndexingRunning(boolean indexingRunning) {
        this.isIndexingRunning = indexingRunning;
    }

    public boolean isIndexingStoppedByUser() {
        return isIndexingStoppedByUser;
    }

    public void setIndexingStoppedByUser(boolean indexingStoppedByUser) {
        this.isIndexingStoppedByUser = indexingStoppedByUser;
    }

    public boolean isAdditionalTasksStoppedByIndexing() {
        return isAdditionalTasksStoppedByIndexing;
    }

    public void setAdditionalTasksStoppedByIndexing(boolean additionalTasksStoppedByIndexing) {
        this.isAdditionalTasksStoppedByIndexing = additionalTasksStoppedByIndexing;
    }

    public void incrementAdditionalTaskCount() {
        additionalTaskCount.incrementAndGet();
    }

    public void decrementAdditionalTaskCount() {
        additionalTaskCount.decrementAndGet();
    }

    public int getAdditionalTaskCount() {
        return additionalTaskCount.get();
    }

    public void incrementTaskCount() {
        taskCount.incrementAndGet();
    }

    public void decrementTaskCount() {
        taskCount.decrementAndGet();
    }

    public int getTaskCount() {
        return taskCount.get();
    }

    public void seDefault() {
        isIndexingRunning = false;
        isIndexingStoppedByUser = false;
        isAdditionalTasksStoppedByIndexing = false;
        additionalTaskCount.set(0);
        taskCount.set(0);
    }
}