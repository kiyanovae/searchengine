package searchengine.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.apiResponces.ErrorResponse;
import searchengine.apiResponces.Response;
import searchengine.config.ServerConfig;

@RestController
public class IndexingController {
    public static final String INDEXING_IS_ALREADY_DOING = "Индексация на этом сервере запрещена";
    public static final String INDEXING_IS_RUNNING = "Индексация уже запущена";
    public static final String INDEXING_NOT_STARTED = "Индексация не была запущена";

    private ServerConfig serverConfig;

    public IndexingController(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @GetMapping("/startIndexing")
    public Response startIndexing() {
        if (serverConfig.isIndexingAvailable()) {
            return new ErrorResponse(INDEXING_IS_ALREADY_DOING);
        }
    }

}
