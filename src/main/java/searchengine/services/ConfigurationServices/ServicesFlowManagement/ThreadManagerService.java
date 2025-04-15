package searchengine.services.ConfigurationServices.ServicesFlowManagement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Service
public class ThreadManagerService
{
    private static final Logger logger = LoggerFactory.getLogger(ThreadManagerService.class);
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

    public boolean isTerminated() {
        return forkJoinPool.isTerminated();
    }


    public void stopAllThreads() {

        if (forkJoinPool.isShutdown()){
            logger.warn("Попытка остановить уже завершенный пул потоков.");
            return;
        }

        forkJoinPool.shutdown();
        try {
            if (!forkJoinPool.awaitTermination(40, TimeUnit.SECONDS)) {
                logger.warn("Некоторые задачи не завершились в течение 40 секунд. Принудительное завершение.");
                forkJoinPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Процесс ожидания завершения потоков был прерван.", e);
            Thread.currentThread().interrupt();
        }
        logger.info("Все потоки корректно завершены.");
    }

    @PreDestroy
    public void preDestroy() {
        stopAllThreads();
    }
}
