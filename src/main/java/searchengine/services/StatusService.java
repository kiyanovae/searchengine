package searchengine.services;

public interface StatusService {
    boolean isIndexingRunning();

    void setIndexingRunning(boolean indexingRunning);

    boolean isIndexingStoppedByUser();

    void setIndexingStoppedByUser(boolean indexingStoppedByUser);

    boolean isAdditionalTasksStoppedByIndexing();

    void setAdditionalTasksStoppedByIndexing(boolean additionalTasksStoppedByIndexing);

    void incrementAdditionalTaskCount();

    void decrementAdditionalTaskCount();

    int getAdditionalTaskCount();

    void incrementTaskCount();

    void decrementTaskCount();

    int getTaskCount();
}