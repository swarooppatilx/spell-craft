package com.example.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GeminiApiClient implements ApiClient {
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";
    private static final int MAX_RETRIES = 2;
    private static final int INITIAL_TIMEOUT_SECONDS = 30;
    private static final int CONNECT_TIMEOUT_SECONDS = 15;
    
    private final HttpClient httpClient;
    private final String apiKey;
    private final Gson gson;

    public GeminiApiClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                .build();
        this.gson = new Gson();
    }

    @Override
    public String translateToCommand(String userInput, WorldState worldState) throws Exception {
        String template = WorldState.getPromptTemplate();
        String prompt = template + userInput + "\n\nOutput:";

        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        
        requestBody.add("contents", contents);
        
        String url = API_URL + "?key=" + apiKey;
        String requestJson = gson.toJson(requestBody);
        
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            int timeout = attempt == 0 ? INITIAL_TIMEOUT_SECONDS : INITIAL_TIMEOUT_SECONDS * (attempt + 1);
            
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                        .timeout(Duration.ofSeconds(timeout))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new Exception("API error: " + response.statusCode() + " - " + response.body());
                }

                JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
                JsonArray candidates = responseJson.getAsJsonArray("candidates");
                
                if (candidates == null || candidates.isEmpty()) {
                    throw new Exception("No response from API");
                }

                JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                JsonObject contentResponse = firstCandidate.getAsJsonObject("content");
                JsonArray partsResponse = contentResponse.getAsJsonArray("parts");
                
                if (partsResponse == null || partsResponse.isEmpty()) {
                    throw new Exception("No text in response");
                }

                return partsResponse.get(0).getAsJsonObject().get("text").getAsString();
                
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