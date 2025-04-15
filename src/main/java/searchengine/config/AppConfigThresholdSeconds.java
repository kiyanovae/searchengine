package searchengine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AppConfigThresholdSeconds {
    @Value("${app.time-threshold-seconds}")
    private long timeThresholdSeconds;

    public long getTimeThresholdSeconds(){
        return timeThresholdSeconds;
    }
}
