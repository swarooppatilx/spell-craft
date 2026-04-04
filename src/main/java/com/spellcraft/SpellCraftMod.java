package com.spellcraft;

import com.spellcraft.ai.ApiClient;
import com.spellcraft.ai.ActionHandler;
import com.spellcraft.ai.Config;
import com.spellcraft.ai.GeminiApiClient;
import com.spellcraft.ai.GoalManager;
import com.spellcraft.ai.LocationMemory;
import com.spellcraft.ai.ReflexHandler;
import com.spellcraft.ai.WorldState;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SpellCraftMod implements ModInitializer {
    public static final String MOD_ID = "spellcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final ConcurrentHashMap<ServerPlayer, PlayerContext> playerContexts = new ConcurrentHashMap<>();

    private static class PlayerContext {
        ReflexHandler reflexHandler;
        GoalManager goalManager;
        LocationMemory locationMemory;
        boolean aiRunning = false;

        PlayerContext(ServerPlayer player) {
            this.reflexHandler = new ReflexHandler(player);
            this.goalManager = new GoalManager(player);
            this.locationMemory = new LocationMemory(player);
        }
    }

    private static PlayerContext getOrCreateContext(ServerPlayer player) {
        return playerContexts.computeIfAbsent(player, PlayerContext::new);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Hello Fabric world!");

        Config.load();

        if (Config.getInstance().isValid()) {
            LOGGER.info("Using Gemini API");
        } else {
            LOGGER.warn("No Gemini API key set. Edit config/spellcraft.json to add your key.");
        }

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PlayerContext ctx = getOrCreateContext(player);

                if (ctx.reflexHandler.checkReflexes()) {
                    continue;
                }

                ctx.goalManager.updatePosition();

                if (ctx.goalManager.shouldAutoTrigger() && !ctx.aiRunning) {
                    triggerAutoGoalAI(player);
                }
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("ai")
                .requires(source -> source.getEntity() != null)
                .then(Commands.argument("query", StringArgumentType.greedyString())
                    .executes(context -> {
                        String query = StringArgumentType.getString(context, "query");
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        executeAICommand(query, player);
                        return 1;
                    })
                )
            );

            dispatcher.register(Commands.literal("ai-goal")
                .requires(source -> source.getEntity() != null)
                .then(Commands.argument("action", StringArgumentType.word())
                    .executes(context -> {
                        String action = StringArgumentType.getString(context, "action");
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        handleGoalCommand(action, player);
                        return 1;
                    })
                )
            );

            dispatcher.register(Commands.literal("ai-memory")
                .requires(source -> source.getEntity() != null)
                .then(Commands.argument("action", StringArgumentType.word())
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(context -> {
                            String action = StringArgumentType.getString(context, "action");
                            String name = StringArgumentType.getString(context, "name");
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            handleMemoryCommand(action, name, player);
                            return 1;
                        })
                    )
                )
            );
        });
    }

    private static void triggerAutoGoalAI(ServerPlayer player) {
        PlayerContext ctx = getOrCreateContext(player);
        String goalPrompt = ctx.goalManager.getGoalPrompt();
        if (goalPrompt.isEmpty()) return;

        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7[Goal] Thinking..."));

        WorldState worldState = new WorldState(player);
        ApiClient apiClient = new GeminiApiClient(Config.getInstance().getGeminiApiKey());

        CompletableFuture.runAsync(() -> {
            try {
                ctx.aiRunning = true;
                String prompt = WorldState.getPromptTemplate() + goalPrompt + "\nQUERY: What is the next action?\nOutput:";
                String response = apiClient.translateToCommand(prompt, worldState);

                ActionHandler handler = new ActionHandler(player, ctx.goalManager, ctx.locationMemory);
                handler.executeActions(response);
            } catch (Exception e) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cError: " + e.getMessage()));
            } finally {
                ctx.aiRunning = false;
            }
        });
    }

    private static void handleGoalCommand(String action, ServerPlayer player) {
        PlayerContext ctx = getOrCreateContext(player);

        switch (action.toLowerCase()) {
            case "clear" -> ctx.goalManager.clearGoal();
            case "status" -> {
                if (ctx.goalManager.hasActiveGoal()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§e[Goal] Current: " + ctx.goalManager.getCurrentGoal()));
                } else {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§7[Goal] No active goal"));
                }
            }
            default -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§cUnknown goal command. Use: /ai-goal clear|status"));
        }
    }

    private static void handleMemoryCommand(String action, String name, ServerPlayer player) {
        PlayerContext ctx = getOrCreateContext(player);

        switch (action.toLowerCase()) {
            case "save" -> ctx.locationMemory.saveLocation(name);
            case "list" -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                ctx.locationMemory.listLocations()));
            case "forget" -> ctx.locationMemory.forgetLocation(name);
            default -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§cUnknown memory command. Use: /ai-memory save|list|forget <name>"));
        }
    }

    private void executeAICommand(String query, ServerPlayer player) {
        if (player == null) {
            LOGGER.warn("Player is null, cannot execute /ai command");
            return;
        }

        if (!Config.getInstance().isValid()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§cError: No Gemini API key. Edit config/spellcraft.json"));
            return;
        }

        PlayerContext ctx = getOrCreateContext(player);

        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7Thinking... " + query));

        WorldState worldState = new WorldState(player);
        GoalManager goalManager = ctx.goalManager;
        LocationMemory locationMemory = ctx.locationMemory;
        ActionHandler actionHandler = new ActionHandler(player, goalManager, locationMemory);
        ApiClient apiClient = new GeminiApiClient(Config.getInstance().getGeminiApiKey());

        String goalPrompt = goalManager.getGoalPrompt();
        String locationPrompt = locationMemory.getLocationsPrompt();
        String fullPrompt = WorldState.getPromptTemplate() + goalPrompt + locationPrompt + query + "\n\nOutput:";

        CompletableFuture.runAsync(() -> {
            try {
                ctx.aiRunning = true;
                String response = apiClient.translateToCommand(fullPrompt, worldState);
                LOGGER.info("AI response: " + response);

                var results = actionHandler.executeActions(response);

                if (results.isEmpty()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cCould not understand: " + query));
                } else {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aDone!"));
                }
            } catch (Exception e) {
                LOGGER.error("Error executing /ai command", e);
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("429")) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cRate limit hit. Wait a moment..."));
                } else {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cError: " + errorMsg));
                }
            } finally {
                ctx.aiRunning = false;
            }
        });
    }
}
