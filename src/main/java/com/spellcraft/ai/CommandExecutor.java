package com.spellcraft.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

public class CommandExecutor {
    private static final Set<String> CHEAT_COMMANDS = Set.of(
        "give", "summon", "setblock", "fill", "replaceitem", "effect",
        "weather", "time", "gamemode", "spawnpoint", "difficulty",
        "clear", "recipe", "tag", "team", "scoreboard", "trigger",
        "worldborder", "title", "bossbar", "data", "function",
        "kill", "particle", "spreadplayers", "testfor", "testforblock",
        "testforblocks", "clone", "execute", "fillbiome"
    );

    public record CommandResult(String command, boolean success, String output, boolean requiresCheats) {}

    public CommandResult execute(String command, ServerPlayer player) {
        if (command == null || command.isBlank()) {
            return new CommandResult("", false, "No command to execute", false);
        }

        boolean needsCheats = requiresCheats(command);
        ServerLevel level = player.level();
        
        try {
            net.minecraft.commands.CommandSourceStack source = player.createCommandSourceStack()
                    .withSuppressedOutput()
                    .withCallback((success, callback) -> {});
            
            var dispatcher = level.getServer().getCommands().getDispatcher();
            var parsed = dispatcher.parse(command, source);
            
            if (parsed.getContext().getNodes().isEmpty()) {
                return new CommandResult(command, false, "Unknown command", needsCheats);
            }
            
            boolean success = dispatcher.execute(parsed) > 0;
            
            String output = success ? "Command executed" : "Command failed or no effect";
            
            return new CommandResult(command, success, output, needsCheats);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = "Unknown error";
            }
            if (errorMsg.contains("Unknown")) {
                return new CommandResult(command, false, "Unknown command: " + command, needsCheats);
            }
            return new CommandResult(command, false, errorMsg, needsCheats);
        }
    }

    public boolean requiresCheats(String command) {
        if (command == null) return false;
        
        String lower = command.toLowerCase().trim();
        
        for (String cheat : CHEAT_COMMANDS) {
            if (lower.startsWith(cheat) || lower.contains(" " + cheat) || lower.contains("/" + cheat)) {
                return true;
            }
        }
        
        if (lower.startsWith("execute ") && (lower.contains("@e") || lower.contains("/summon") || lower.contains("/setblock"))) {
            return true;
        }
        
        return false;
    }
}