package com.rentease.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * The only class that talks to Semaphore (NFR022), so the SMS provider can be replaced easily.
 * The API key comes from the environment and is never sent to the browser (NFR010).
 */
@Component
public class SemaphoreClient {

    private static final Logger log = LoggerFactory.getLogger(SemaphoreClient.class);

    public record Result(boolean success, String detail) {
    }

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String apiKey;
    private final String senderName;
    private final String url;

    public SemaphoreClient(@Value("${app.sms.semaphore.api-key:}") String apiKey,
                           @Value("${app.sms.semaphore.sender-name:}") String senderName,
                           @Value("${app.sms.semaphore.url:https://api.semaphore.co/api/v4/messages}") String url) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.senderName = senderName == null ? "" : senderName.trim();
        this.url = url;
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    /** @param number Philippine number in international format, for example 639171234567 */
    public Result send(String number, String message) {
        if (!isConfigured()) {
            return new Result(false, "SMS is not configured (set SEMAPHORE_API_KEY)");
        }
        StringBuilder form = new StringBuilder()
                .append("apikey=").append(enc(apiKey))
                .append("&number=").append(enc(number))
                .append("&message=").append(enc(message));
        if (!senderName.isEmpty()) {
            form.append("&sendername=").append(enc(senderName));
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new Result(true, "Accepted by Semaphore");
            }
            log.warn("Semaphore rejected a message (HTTP {})", response.statusCode());
            return new Result(false, "Semaphore returned HTTP " + response.statusCode());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new Result(false, "Interrupted while sending");
        } catch (Exception ex) {
            log.warn("Could not reach Semaphore: {}", ex.getClass().getSimpleName());
            return new Result(false, "Could not reach Semaphore");
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
