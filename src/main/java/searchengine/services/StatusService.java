package searchengine.services;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class StatusService {
    private static final int SLEEP_DURATION = 1000;
    private volatile boolean isIndexingRunning;
    private volatile boolean isIndexingStoppedByUser;
    private volatile boolean isIndexingStoppedByNewIndexing;
    private final AtomicInteger additionalTaskCount;
    private final AtomicInteger taskCount;

    public StatusService() {
        isIndexingRunning = false;
        isIndexingStoppedByUser = false;
        isIndexingStoppedByNewIndexing = false;
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
        isIndexingStoppedByNewIndexing = false;
        additionalTaskCount.set(0);
        taskCount.set(0);
    }

    public void startIndexing() throws InterruptedException {
        isIndexingStoppedByNewIndexing = true;
        while (additionalTaskCount.get() != 0) {
            Thread.sleep(SLEEP_DURATION);
        }
        isIndexingStoppedByNewIndexing = false;
        isIndexingRunning = true;
        isIndexingStoppedByUser = false;
    }

    public void stopIndexing() {
        isIndexingRunning = false;
        isIndexingStoppedByUser = false;
    }

    public boolean isIndexingStopped() {
        return isIndexingStoppedByUser || isIndexingStoppedByNewIndexing;
    }
}