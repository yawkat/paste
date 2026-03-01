package at.yawk.paste.server;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Sends page view events to Umami analytics server-side.
 * Fire-and-forget: failures are logged and silently ignored.
 */
@Slf4j
@Singleton
public class UmamiClient {
    private final @Nullable String apiUrl;
    private final @Nullable String websiteId;
    private final @Nullable HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Inject
    public UmamiClient(Config config, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        if (config.getUmamiUrl() != null && config.getUmamiWebsiteId() != null) {
            this.apiUrl = config.getUmamiUrl().replaceAll("/+$", "") + "/api/send";
            this.websiteId = config.getUmamiWebsiteId();
            this.httpClient = HttpClient.newHttpClient();
            log.info("Umami analytics enabled: {}", this.apiUrl);
        } else {
            this.apiUrl = null;
            this.websiteId = null;
            this.httpClient = null;
            log.info("Umami analytics disabled (umamiUrl or umamiWebsiteId not configured)");
        }
    }

    /**
     * Track a paste view. Extracts visitor info from the exchange headers.
     *
     * @param exchange the HTTP exchange for the current request
     * @param pasteId  the paste ID being accessed
     */
    public void trackView(HttpServerExchange exchange, String pasteId) {
        if (httpClient == null) {
            return;
        }

        String userAgent = getHeader(exchange, "User-Agent");
        if (userAgent == null) {
            userAgent = "Unknown";
        }

        var event = new UmamiEvent(
                "event",
                new UmamiEvent.Payload(
                        websiteId,
                        exchange.getRelativePath(),
                        exchange.getHostName(),
                        getHeaderOrEmpty(exchange, "Accept-Language"),
                        getHeaderOrEmpty(exchange, "Referer")
                )
        );

        String json = objectMapper.writeValueAsString(event);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent)
                .POST(HttpRequest.BodyPublishers.ofString(json));

        // Forward visitor IP so Umami sees the real client, not our server IP.
        // Prefer X-Forwarded-For (standard for proxied requests), fall back to X-Real-Ip.
        String forwardedFor = getHeader(exchange, "X-Forwarded-For");
        if (forwardedFor != null) {
            requestBuilder.header("X-Forwarded-For", forwardedFor);
        } else {
            String realIp = getHeader(exchange, "X-Real-Ip");
            if (realIp != null) {
                requestBuilder.header("X-Forwarded-For", realIp);
            }
        }

        httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.discarding())
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        log.warn("Failed to send Umami event for {}: {}", pasteId, throwable.getMessage());
                    } else if (response.statusCode() >= 400) {
                        log.warn("Umami returned {} for {}", response.statusCode(), pasteId);
                    }
                });
    }

    private static @Nullable String getHeader(HttpServerExchange exchange, String name) {
        HeaderValues values = exchange.getRequestHeaders().get(name);
        return values != null ? values.getFirst() : null;
    }

    private static String getHeaderOrEmpty(HttpServerExchange exchange, String name) {
        String value = getHeader(exchange, name);
        return value != null ? value : "";
    }
}
