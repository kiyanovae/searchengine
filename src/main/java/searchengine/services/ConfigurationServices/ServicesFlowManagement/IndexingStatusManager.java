package searchengine.services.ConfigurationServices.ServicesFlowManagement;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class IndexingStatusManager {
    private final AtomicBoolean stopIndexing = new AtomicBoolean(false);
    private final AtomicBoolean isIndexingRunning = new AtomicBoolean(false);
    private final AtomicBoolean stoppingByTheUser = new AtomicBoolean(false);


    public boolean startIndexing() {
        return isIndexingRunning.compareAndSet(false, true);
    }
    public boolean isIndexingRunning() {
        return isIndexingRunning.get();
    }
    public void stoppingByTheUser(){
        stoppingByTheUser.set(true);
    }
    public boolean isStoppingByTheUser(){
        return stoppingByTheUser.get();
    }
    public void stopIndexing() {
        stopIndexing.set(true);
        isIndexingRunning.set(true);
    }
    public void resetStopIndexing(){
        stopIndexing.set(false);
        isIndexingRunning.set(false);
    }
    public boolean shouldIsStopIndexing() {
        return stopIndexing.get();
    }
}
