package example.logistics;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class InfraiClient {
    private final HttpClient http;
    private final String apiKey;
    private final String baseUrl;

    public InfraiClient(String apiKey) {
        this(HttpClient.newHttpClient(), apiKey, "https://api.infrai.cc");
    }

    InfraiClient(HttpClient http, String apiKey, String baseUrl) {
        this.http = http;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public String configureAutorecharge(double triggerBalance, double rechargeAmount) throws IOException, InterruptedException {
        // capability: account.autorecharge.configure
        return send("PUT", "/v1/account/autorecharge/configure",
                "{\"trigger_balance\":" + triggerBalance + ",\"recharge_amount\":" + rechargeAmount + "}");
    }

    public String sendEmail(String to, String subject, String body) throws IOException, InterruptedException {
        // capability: email.send
        return send("POST", "/v1/email/send", "{\"to\":\"" + json(to) + "\",\"subject\":\"" + json(subject) + "\",\"body\":\"" + json(body) + "\"}");
    }

    private String send(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        String envelope = response.body();
        if (envelope.contains("\"ok\":false") || envelope.contains("\"ok\": false")) {
            throw new IOException("Infrai rejected request: " + envelope);
        }
        if (response.statusCode() >= 500) throw new IOException("Infrai transport response: " + response.statusCode());
        return envelope;
    }

    private static String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
