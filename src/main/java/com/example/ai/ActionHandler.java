package com.example.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class ActionHandler {
    private final ServerPlayer player;
    private final CommandExecutor executor;
    private final GoalManager goalManager;
    private final LocationMemory locationMemory;

    public ActionHandler(ServerPlayer player, GoalManager goalManager, LocationMemory locationMemory) {
        this.player = player;
        this.executor = new CommandExecutor();
        this.goalManager = goalManager;
        this.locationMemory = locationMemory;
    }

    public record ActionResult(String message, boolean success) {}

    public List<ActionResult> executeActions(String response) {
        List<ActionResult> results = new ArrayList<>();
        
        try {
            String jsonStr = extractJson(response);
            JsonObject root = JsonParser.parseString(jsonStr).getAsJsonObject();
            
            if (root.has("thought")) {
                String thought = root.get("thought").getAsString();
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7" + thought));
            }
            
            if (root.has("actions") && root.get("actions").isJsonArray()) {
                JsonArray actions = root.getAsJsonArray("actions");
                
                for (var actionElem : actions) {
                    JsonObject action = actionElem.getAsJsonObject();
                    String actionType = action.get("action").getAsString();
                    JsonObject params = action.getAsJsonObject("params");
                    
                    ActionResult result = executeAction(actionType, params);
                    results.add(result);
                }
            }
            
        } catch (Exception e) {
            results.add(new ActionResult("§cFailed to parse response: " + e.getMessage(), false));
        }
        
        return results;
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start == -1 || end == -1) {
            return "{\"error\": \"No JSON found\"}";
        }
        return response.substring(start, end + 1);
    }

    private ActionResult executeAction(String actionType, JsonObject params) {
        try {
            return switch (actionType) {
                case "chat" -> {
                    String message = params.get("message").getAsString();
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§b" + message));
                    yield new ActionResult("Sent: " + message, true);
                }
                case "give" -> {
                    String itemId = normalizeId(params.get("item_id").getAsString(), "item");
                    int amount = params.has("amount") ? params.get("amount").getAsInt() : 1;
                    String command = "give @p " + itemId + " " + amount;
                    var result = executor.execute(command, player);
                    yield new ActionResult(result.output(), result.success());
                }
                case "spawn" -> {
                    String entityId = normalizeId(params.get("entity_id").getAsString(), "entity");
                    int amount = params.has("amount") ? params.get("amount").getAsInt() : 1;
                    String command = "execute at @p run summon " + entityId + " ~ ~ ~";
                    var result = executor.execute(command, player);
                    yield new ActionResult(result.output(), result.success());
                }
                case "effect" -> {
                    String effectId = normalizeId(params.get("effect_id").getAsString(), "effect");
                    int duration = params.has("duration") ? params.get("duration").getAsInt() : 30;
                    int amplifier = params.has("amplifier") ? params.get("amplifier").getAsInt() : 0;
                    String command = "effect give @p " + effectId + " " + duration + " " + amplifier;
                    var result = executor.execute(command, player);
                    yield new ActionResult(result.output(), result.success());
                }
                case "set_world" -> {
                    String property = params.get("property").getAsString();
                    String value = params.get("value").getAsString();
                    String command = property + " set " + value;
                    var result = executor.execute(command, player);
                    yield new ActionResult(result.output(), result.success());
                }
                case "cast_spell" -> {
                    String rawCommand = params.get("raw_command").getAsString();
                    var result = executor.execute(rawCommand, player);
                    yield new ActionResult(result.output(), result.success());
                }
                case "set_goal" -> {
                    String goal = params.get("goal").getAsString();
                    String plan = params.has("plan") ? params.get("plan").getAsString() : "";
                    goalManager.setGoal(goal, plan);
                    yield new ActionResult("Goal set: " + goal, true);
                }
                case "complete_step" -> {
                    goalManager.completeCurrentStep();
                    yield new ActionResult("Step completed", true);
                }
                case "clear_goal" -> {
                    goalManager.clearGoal();
                    yield new ActionResult("Goal cleared", true);
                }
                case "remember_location" -> {
                    String name = params.get("name").getAsString();
                    locationMemory.saveLocation(name);
                    yield new ActionResult("Location saved: " + name, true);
                }
                case "teleport_to" -> {
                    String location = params.get("location").getAsString();
                    var pos = locationMemory.getLocation(location);
                    if (pos != null) {
                        String command = "tp @p " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
                        var result = executor.execute(command, player);
                        yield new ActionResult(result.output(), result.success());
                    } else {
                        yield new ActionResult("§cUnknown location: " + location, false);
                    }
                }
                default -> new ActionResult("§cUnknown action: " + actionType, false);
            };
        } catch (Exception e) {
            return new ActionResult("§cError: " + e.getMessage(), false);
        }
    }

    private String normalizeId(String id, String type) {
        String lower = id.toLowerCase();
        if (lower.startsWith("minecraft:")) {
            return lower.substring(10);
        }
        
        return switch (type) {
            case "item" -> normalizeItem(lower);
            case "entity" -> normalizeEntity(lower);
            case "effect" -> normalizeEffect(lower);
            default -> lower;
        };
    }

    private String normalizeItem(String item) {
        String normalized = item.replace(" ", "_").toLowerCase();
        return switch (normalized) {
            case "diamond_sword" -> "diamond_sword";
            case "iron_sword" -> "iron_sword";
            case "netherite_sword" -> "netherite_sword";
            case "diamond_pickaxe" -> "diamond_pickaxe";
            case "iron_pickaxe" -> "iron_pickaxe";
            case "name_tag", "nametag" -> "name_tag";
            case "golden_apple", "gold_apple" -> "golden_apple";
            case "enchanted_golden_apple" -> "enchanted_golden_apple";
            case "cooked_beef", "steak" -> "cooked_beef";
            case "lava" -> "lava_bucket";
            case "water" -> "water_bucket";
            case "milk" -> "milk_bucket";
            case "oak_log" -> "oak_log";
            case "oak_planks" -> "oak_planks";
            case "grass_block" -> "grass_block";
            case "cobblestone" -> "cobblestone";
            case "diamond_block" -> "diamond_block";
            case "iron_block" -> "iron_block";
            case "gold_block" -> "gold_block";
            case "emerald_block" -> "emerald_block";
            case "redstone_block" -> "redstone_block";
            case "coal_block" -> "coal_block";
            case "redstone_torch" -> "redstone_torch";
            case "soul_torch" -> "soul_torch";
            case "soul_lantern" -> "soul_lantern";
            case "netherite_ingot" -> "netherite_ingot";
            case "lapis_lazuli" -> "lapis_lazuli";
            default -> normalized;
        };
    }

    private String normalizeEntity(String entity) {
        String normalized = entity.replace(" ", "_").toLowerCase();
        return switch (normalized) {
            case "wolf", "dog" -> "wolf";
            case "cat", "ocelot" -> "cat";
            case "pigman", "zombie_pigman" -> "zombified_piglin";
            case "bear", "polar_bear" -> "polar_bear";
            case "panda" -> "panda";
            case "chicken", "bird" -> "chicken";
            case "parrot" -> "parrot";
            case "horse" -> "horse";
            case "donkey" -> "donkey";
            case "mule" -> "mule";
            case "llama" -> "llama";
            case "cow" -> "cow";
            case "pig" -> "pig";
            case "sheep" -> "sheep";
            case "rabbit" -> "rabbit";
            case "fox" -> "fox";
            case "zombie" -> "zombie";
            case "skeleton" -> "skeleton";
            case "creeper" -> "creeper";
            case "spider" -> "spider";
            case "enderman", "ender_man" -> "enderman";
            case "pillager" -> "pillager";
            case "vex" -> "vex";
            case "evoker" -> "evoker";
            case "vindicator" -> "vindicator";
            case "ravager" -> "ravager";
            case "iron_golem" -> "iron_golem";
            case "snow_golem", "snowman" -> "snow_golem";
            case "villager" -> "villager";
            case "wandering_trader" -> "wandering_trader";
            case "ender_dragon", "dragon" -> "ender_dragon";
            case "shulker" -> "shulker";
            case "blaze" -> "blaze";
            case "ghast" -> "ghast";
            case "magma_cube" -> "magma_cube";
            case "slime" -> "slime";
            case "piglin" -> "piglin";
            case "hoglin" -> "hoglin";
            case "wither_skeleton" -> "wither_skeleton";
            case "phantom" -> "phantom";
            case "dolphin" -> "dolphin";
            case "axolotl" -> "axolotl";
            case "turtle" -> "turtle";
            case "cod", "salmon", "tropical_fish", "pufferfish" -> normalized;
            case "shark" -> "dolphin";
            case "fish" -> "cod";
            case "bat" -> "bat";
            case "warden" -> "warden";
            default -> normalized;
        };
    }

    private String normalizeEffect(String effect) {
        String normalized = effect.replace(" ", "_").toLowerCase();
        return switch (normalized) {
            case "speed" -> "speed";
            case "slowness" -> "slowness";
            case "haste" -> "haste";
            case "mining_fatigue", "fatigue" -> "mining_fatigue";
            case "strength" -> "strength";
            case "jump_boost", "jump" -> "jump_boost";
            case "regeneration" -> "regeneration";
            case "resistance" -> "resistance";
            case "fire_resistance" -> "fire_resistance";
            case "water_breathing" -> "water_breathing";
            case "invisibility" -> "invisibility";
            case "blindness" -> "blindness";
            case "night_vision" -> "night_vision";
            case "glowing" -> "glowing";
            case "levitation" -> "levitation";
            case "luck" -> "luck";
            case "unluck" -> "unluck";
            case "slow_falling" -> "slow_falling";
            case "conduit_power" -> "conduit_power";
            case "dolphins_grace" -> "dolphins_grace";
            case "bad_omen" -> "bad_omen";
            case "hero_of_the_village" -> "hero_of_the_village";
            default -> normalized;
        };
    }
}
