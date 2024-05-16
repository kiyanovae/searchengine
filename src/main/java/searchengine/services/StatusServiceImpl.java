package searchengine.services;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class StatusServiceImpl implements StatusService {
    private volatile boolean isIndexingRunning;
    private volatile boolean isIndexingStoppedByUser;
    private volatile boolean isAdditionalTasksStoppedByIndexing;
    private final AtomicInteger additionalTaskCount;
    private final AtomicInteger taskCount;

    public StatusServiceImpl() {
        isIndexingRunning = false;
        isIndexingStoppedByUser = false;
        isAdditionalTasksStoppedByIndexing = false;
        additionalTaskCount = new AtomicInteger();
        taskCount = new AtomicInteger();
    }

    @Override
    public boolean isIndexingRunning() {
        return isIndexingRunning;
    }

    @Override
    public void setIndexingRunning(boolean indexingRunning) {
        this.isIndexingRunning = indexingRunning;
    }

    @Override
    public boolean isIndexingStoppedByUser() {
        return isIndexingStoppedByUser;
    }

    @Override
    public void setIndexingStoppedByUser(boolean indexingStoppedByUser) {
        this.isIndexingStoppedByUser = indexingStoppedByUser;
    }

    @Override
    public boolean isAdditionalTasksStoppedByIndexing() {
        return isAdditionalTasksStoppedByIndexing;
    }

    @Override
    public void setAdditionalTasksStoppedByIndexing(boolean additionalTasksStoppedByIndexing) {
        this.isAdditionalTasksStoppedByIndexing = additionalTasksStoppedByIndexing;
    }

    @Override
    public void incrementAdditionalTaskCount() {
        additionalTaskCount.incrementAndGet();
    }

    @Override
    public void decrementAdditionalTaskCount() {
        additionalTaskCount.decrementAndGet();
    }

    @Override
    public int getAdditionalTaskCount() {
        return additionalTaskCount.get();
    }

    @Override
    public void incrementTaskCount() {
        taskCount.incrementAndGet();
    }

    @Override
    public void decrementTaskCount() {
        taskCount.decrementAndGet();
    }

    @Override
    public int getTaskCount() {
        return taskCount.get();
    }
}