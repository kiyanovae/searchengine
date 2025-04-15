package searchengine.utils;

import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

@Component
public class UrlUtils {

    public static String getDomainFromUrl(String url) {
        return url.replaceAll("^(https?://)?(www\\.)?", "").split("/")[0];
    }

    public static String getPathFromUrlPage(String urlPage){
        return urlPage.replaceAll("^(https?://)?(www\\.)?[^/]+", "")
                .split("\\?")[0].split("#")[0];
    }

    public static String removeTrailingSlash(String urlWithSlash) {
        return urlWithSlash.replaceAll("/+$", "");
    }

    public static String removeProtocolAndSubdomain(String url){       // "https://www.skillbox.ru/ss/ff/ff")) ---- // https://www.skillbox.ru
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            URL parsedUrl = new URL(url);
            return parsedUrl.getProtocol() + "://" + parsedUrl.getHost();

        } catch (MalformedURLException e) {
            System.err.println("Invalid URL: " + url);
            return url;
        }
    }
}
