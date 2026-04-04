package com.example.ai;

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
    private static final int INITIAL_TIMEOUT_SECONDS = 60;
    private static final int CONNECT_TIMEOUT_SECONDS = 15;
    
    private final HttpClient httpClient;
    private final String endpoint;
    private final String model;
    private final Gson gson;

    public OllamaApiClient(String endpoint, String model) {
        this.endpoint = endpoint;
        this.model = model;
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
        requestBody.addProperty("model", model);
        
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        
        requestBody.add("messages", messages);
        requestBody.addProperty("stream", false);

        String url = endpoint + "/api/chat";
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
                    throw new Exception("Ollama API error: " + response.statusCode() + " - " + response.body());
                }

                JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
                JsonObject messageResponse = responseJson.getAsJsonObject("message");
                
                if (messageResponse == null) {
                    throw new Exception("No response from Ollama API");
                }

                return messageResponse.get("content").getAsString();
                
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
