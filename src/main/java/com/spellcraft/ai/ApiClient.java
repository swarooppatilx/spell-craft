package com.spellcraft.ai;

public interface ApiClient {
    String translateToCommand(String userInput, WorldState worldState) throws Exception;
}
