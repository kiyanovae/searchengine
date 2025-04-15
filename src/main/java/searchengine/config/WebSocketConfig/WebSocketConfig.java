package searchengine.config.WebSocketConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import searchengine.services.impl.StatisticsService;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final StatisticsService statisticsService;
    @Autowired
    public WebSocketConfig(@Lazy StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(statisticsWebSocketHandler(), "/statistics-updates")
                .setAllowedOrigins("*");
    }
    @Bean
    public StatisticsWebSocketHandler statisticsWebSocketHandler() {
        return new StatisticsWebSocketHandler(statisticsService);
    }
}
