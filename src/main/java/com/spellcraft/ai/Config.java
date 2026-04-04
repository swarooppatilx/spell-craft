package com.spellcraft.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Logger LOGGER = LoggerFactory.getLogger("Config");
    private static Config instance;
    private String apiProvider;
    private String geminiApiKey;
    private String ollamaApiUrl;
    private String ollamaModel;

    private Config() {}

    public static Config getInstance() {
        return instance;
    }

    public static void load() {
        try {
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("spellcraft.json");
            Gson gson = new Gson();
            
            if (!Files.exists(configPath)) {
                LOGGER.warn("Config file not found at " + configPath + ", creating default config...");
                instance = new Config();
                instance.apiProvider = "gemini";
                instance.geminiApiKey = "";
                instance.ollamaApiUrl = "http://localhost:11434";
                instance.ollamaModel = "llama3";
                String defaultConfig = """
                    {
                      "api_provider": "gemini",
                      "gemini_api_key": "YOUR_API_KEY_HERE",
                      "ollama_api_url": "http://localhost:11434",
                      "ollama_model": "llama3"
                    }
                    """;
                Files.writeString(configPath, defaultConfig);
                return;
            }
            
            Reader reader = Files.newBufferedReader(configPath);
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            instance = new Config();
            instance.apiProvider = json.has("api_provider") ? json.get("api_provider").getAsString() : "gemini";
            instance.geminiApiKey = json.has("gemini_api_key") ? json.get("gemini_api_key").getAsString() : "";
            instance.ollamaApiUrl = json.has("ollama_api_url") ? json.get("ollama_api_url").getAsString() : "http://localhost:11434";
            instance.ollamaModel = json.has("ollama_model") ? json.get("ollama_model").getAsString() : "llama3";
            
            reader.close();
            LOGGER.info("Config loaded from {} - using {} provider", configPath, instance.apiProvider);
        } catch (Exception e) {
            LOGGER.error("Error loading config", e);
            instance = new Config();
            instance.apiProvider = "ollama";
            instance.geminiApiKey = "";
            instance.ollamaApiUrl = "http://localhost:11434";
            instance.ollamaModel = "minimax-m2.5:cloud";
        }
    }

    public String getApiProvider() {
        return apiProvider;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public String getOllamaApiUrl() {
        return ollamaApiUrl;
    }

    public String getOllamaModel() {
        return ollamaModel;
    }

    public boolean isValid() {
        if (apiProvider == null || apiProvider.isEmpty()) {
            return false;
        }
        
        if ("ollama".equalsIgnoreCase(apiProvider)) {
            return ollamaApiUrl != null && !ollamaApiUrl.isEmpty();
        }
        
        return geminiApiKey != null && !geminiApiKey.isEmpty() && !geminiApiKey.equals("YOUR_API_KEY_HERE");
    }

    public boolean useOllama() {
        return "ollama".equalsIgnoreCase(apiProvider);
    }
}
