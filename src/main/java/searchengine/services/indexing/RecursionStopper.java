package searchengine.services.indexing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.config.AppConfigThresholdSeconds;

import java.sql.Timestamp;
import java.time.Instant;

@Service
public class RecursionStopper {

   private final AppConfigThresholdSeconds appConfigThresholdSeconds;
    private final JdbcTemplate jdbcTemplate;

    public RecursionStopper(AppConfigThresholdSeconds appConfigThresholdSeconds,
                            JdbcTemplate jdbcTemplate) {
        this.appConfigThresholdSeconds = appConfigThresholdSeconds;
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean shouldStopRecursion(){
        String sql = "SELECT MAX(status_time)from site";
        Timestamp lastUpdate = jdbcTemplate.queryForObject(sql, Timestamp.class);

        if (lastUpdate == null){
            return false;
        }

        return lastUpdate
                .toInstant()
                .isBefore(Instant.now().minusSeconds(appConfigThresholdSeconds.getTimeThresholdSeconds()));
    }
}
