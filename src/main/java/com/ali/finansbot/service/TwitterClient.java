package com.ali.finansbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class TwitterClient {

    private final String apiKey;
    private final String apiSecret;
    private final String accessToken;
    private final String accessTokenSecret;
    private final WebClient webClient;

    public TwitterClient(
            @Value("${twitter.api-key}") String apiKey,
            @Value("${twitter.api-secret}") String apiSecret,
            @Value("${twitter.access-token}") String accessToken,
            @Value("${twitter.access-token-secret}") String accessTokenSecret) {
        this.apiKey = safe(apiKey);
        this.apiSecret = safe(apiSecret);
        this.accessToken = safe(accessToken);
        this.accessTokenSecret = safe(accessTokenSecret);

        this.webClient = WebClient.builder()
                .baseUrl("https://api.twitter.com")
                .build();
    }

    public void postTweet(String text) {
        try {
            // Yeni API endpoint (V2)
            final String url = "https://api.twitter.com/2/tweets";
            final String nonce = UUID.randomUUID().toString().replace("-", "");
            final long now = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond();
            final String timestamp = String.valueOf(now);

            // OAuth 1.0a parametreleri
            Map<String, String> oauth = new LinkedHashMap<>();
            oauth.put("oauth_consumer_key", apiKey);
            oauth.put("oauth_nonce", nonce);
            oauth.put("oauth_signature_method", "HMAC-SHA1");
            oauth.put("oauth_timestamp", timestamp);
            oauth.put("oauth_token", accessToken);
            oauth.put("oauth_version", "1.0");

            // Request body parametresi (status artık 'text')
            Map<String, String> bodyParams = new LinkedHashMap<>();
            bodyParams.put("text", text);

            // === BASE STRING oluştur ===
            List<AbstractMap.SimpleEntry<String, String>> all = new ArrayList<>();
            oauth.forEach((k, v) -> all.add(new AbstractMap.SimpleEntry<>(k, v)));
            // not: body parametresi base string'e dahil EDİLMEZ, çünkü V2 JSON payload
            // gönderiyor
            List<AbstractMap.SimpleEntry<String, String>> encoded = new ArrayList<>();
            for (var e : all) {
                encoded.add(new AbstractMap.SimpleEntry<>(encode(e.getKey()), encode(e.getValue())));
            }
            encoded.sort((a, b) -> {
                int c = a.getKey().compareTo(b.getKey());
                return (c != 0) ? c : a.getValue().compareTo(b.getValue());
            });

            StringBuilder norm = new StringBuilder();
            for (int i = 0; i < encoded.size(); i++) {
                var e = encoded.get(i);
                norm.append(e.getKey()).append("=").append(e.getValue());
                if (i < encoded.size() - 1)
                    norm.append("&");
            }

            String baseString = "POST&" + encode(url) + "&" + encode(norm.toString());
            String signingKey = encode(apiSecret) + "&" + encode(accessTokenSecret);
            String signature = hmacSha1(baseString, signingKey);
            oauth.put("oauth_signature", signature);

            String authHeader = buildSortedAuthHeader(oauth);

            Map<String, Object> payload = Map.of("text", text);

            webClient.post()
                    .uri("/2/tweets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", authHeader)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("[Twitter] ✅ Tweet sent successfully");

        } catch (Exception e) {
            System.out.println("[Twitter] ❌ Tweet gönderilemedi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String buildSortedAuthHeader(Map<String, String> oauth) {
        TreeMap<String, String> sorted = new TreeMap<>(oauth);
        StringBuilder h = new StringBuilder("OAuth ");
        Iterator<Map.Entry<String, String>> it = sorted.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            h.append(encode(e.getKey())).append("=\"").append(encode(e.getValue())).append("\"");
            if (it.hasNext())
                h.append(", ");
        }
        return h.toString();
    }

    private static String hmacSha1(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private static String encode(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }

    private static String safe(String s) {
        return s == null ? null : s.trim();
    }
}
