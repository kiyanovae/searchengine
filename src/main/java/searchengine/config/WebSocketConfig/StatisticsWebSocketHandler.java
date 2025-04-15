package searchengine.config.WebSocketConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.impl.StatisticsService;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class StatisticsWebSocketHandler extends TextWebSocketHandler {
    private final StatisticsService statisticsService;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(StatisticsWebSocketHandler.class);
    private final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());

    public StatisticsWebSocketHandler(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session,
                                      org.springframework.web.socket.CloseStatus status) {
        sessions.remove(session);
    }

    public void sendStatisticsToAll() {
        StatisticsResponse response = statisticsService.getStatistics();
        String jsonResponse;
        try {
            jsonResponse = objectMapper.writeValueAsString(response);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(jsonResponse));
                }
            }
        } catch (IOException e) {
            System.out.println("ObjectMapper modules: " + objectMapper.getRegisteredModuleIds());
            logger.error("Ошибка при отправке статистики по WebSocket", e);        }
    }
}
