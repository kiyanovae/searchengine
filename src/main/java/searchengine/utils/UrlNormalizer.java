package searchengine.utils;

import java.net.URI;
import java.net.URISyntaxException;

public class UrlNormalizer {
    public static String normalizeUrl(String url) {

        try {
            URI uri = new URI(url).normalize();
            String normalizedUrl = uri.getScheme() + "://" + uri.getHost() + (uri.getPath() != null ? uri.getPath() : "");
            return normalizedUrl.endsWith("/") ? normalizedUrl.substring(0, normalizedUrl.length() - 1) : normalizedUrl;
        } catch (URISyntaxException e) {
            return "Ошибка нормализации ссылки: " + url;
        }
    }
}
