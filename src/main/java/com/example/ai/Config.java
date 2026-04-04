package com.example.ai;

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
    private String geminiApiKey;
    private String ollamaEndpoint;
    private String ollamaModel;

    private Config() {}

    public static Config getInstance() {
        return instance;
    }

    public static void load() {
        try {
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("modid.json");
            Gson gson = new Gson();
            
            if (!Files.exists(configPath)) {
                LOGGER.warn("Config file not found at " + configPath + ", creating default config...");
            instance = new Config();
            instance.geminiApiKey = "";
            instance.ollamaEndpoint = "";
            instance.ollamaModel = "";
            String defaultConfig = """
                    {
                      "gemini_api_key": "YOUR_API_KEY_HERE",
                      "ollama_endpoint": "http://localhost:11434",
                      "ollama_model": "llama3"
                    }
                    """;
                Files.writeString(configPath, defaultConfig);
                return;
            }
            
            Reader reader = Files.newBufferedReader(configPath);
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            instance = new Config();
            instance.geminiApiKey = json.has("gemini_api_key") ? json.get("gemini_api_key").getAsString() : "";
            instance.ollamaEndpoint = json.has("ollama_endpoint") ? json.get("ollama_endpoint").getAsString() : "http://localhost:11434";
            instance.ollamaModel = json.has("ollama_model") ? json.get("ollama_model").getAsString() : "llama3";
            
            reader.close();
            LOGGER.info("Config loaded from " + configPath);
        } catch (Exception e) {
            LOGGER.error("Error loading config", e);
            instance = new Config();
            instance.geminiApiKey = "";
            instance.ollamaEndpoint = "http://localhost:11434";
            instance.ollamaModel = "llama3";
        }
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public String getOllamaEndpoint() {
        return ollamaEndpoint;
    }

    public String getOllamaModel() {
        return ollamaModel;
    }

    public boolean isGeminiConfigured() {
        return geminiApiKey != null && !geminiApiKey.isEmpty() && !geminiApiKey.equals("YOUR_API_KEY_HERE");
    }

    public boolean isOllamaConfigured() {
        return ollamaEndpoint != null && !ollamaEndpoint.isEmpty() &&
               ollamaModel != null && !ollamaModel.isEmpty();
    }

    public boolean isValid() {
        return isGeminiConfigured() || isOllamaConfigured();
    }

    public boolean useOllama() {
        return isOllamaConfigured() && !isGeminiConfigured();
    }
}
