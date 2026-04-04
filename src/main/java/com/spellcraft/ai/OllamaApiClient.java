package com.spellcraft.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OllamaApiClient implements ApiClient {
    private static final int MAX_RETRIES = 2;
    private static final int TIMEOUT_SECONDS = 60;
    
    private final HttpClient httpClient;
    private final String apiUrl;
    private final String model;
    private final Gson gson;

    public OllamaApiClient(String apiUrl, String model) {
        this.apiUrl = apiUrl;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.gson = new Gson();
    }

    @Override
    public String translateToCommand(String userInput, WorldState worldState) throws Exception {
        String template = WorldState.getPromptTemplate();
        String prompt = template + userInput + "\n\nOutput:";

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("prompt", prompt);
        requestBody.addProperty("stream", false);

        String url = apiUrl + "/api/generate";
        String requestJson = gson.toJson(requestBody);
        
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                        .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new Exception("API error: " + response.statusCode() + " - " + response.body());
                }

                JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
                
                if (responseJson.has("response")) {
                    return responseJson.get("response").getAsString();
                }
                
                throw new Exception("No response from API");
                
            } catch (Exception e) {
                lastException = e;
                if (e instanceof java.net.http.HttpTimeoutException || 
                    e.getMessage() != null && e.getMessage().contains("timeout")) {
                    if (attempt < MAX_RETRIES) {
                        continue;
                    }
                }
                throw e;
            }
        }
        
        throw lastException != null ? lastException : new Exception("Max retries exceeded");
    }
}
