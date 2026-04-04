package com.spellcraft.ai;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class WorldState {
    private final String position;
    private final float health;
    private final int food;
    private final int xpLevel;
    private final String gameMode;
    private final String dimension;
    private final String timeOfDay;
    private final int dayCount;
    private final String weather;
    private final String biome;
    private final String blockAtCursor;
    private final String heldItem;
    private final String inventory;
    private final String nearbyEntities;

    public WorldState(ServerPlayer player) {
        this.position = String.format("x:%d, y:%d, z:%d", 
            player.getBlockX(), player.getBlockY(), player.getBlockZ());
        this.health = player.getHealth();
        this.food = player.getFoodData().getFoodLevel();
        this.xpLevel = player.experienceLevel;
        this.gameMode = player.gameMode.getGameModeForPlayer().getName();
        
        Level level = player.level();
        this.dimension = level.dimension().toString();
        
        long gameTime = level.getGameTime() % 24000;
        this.timeOfDay = getTimeDescription(gameTime);
        this.dayCount = (int)(level.getGameTime() / 24000);
        
        this.weather = level.isRaining() ? "raining" : "clear";
        
        this.biome = "unknown";
        
        HitResult hit = player.pick(10.0, 0.0f, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            var blockState = level.getBlockState(((BlockHitResult)hit).getBlockPos());
            this.blockAtCursor = blockState.getBlock().getName().getString();
        } else {
            this.blockAtCursor = "air";
        }
        
        var mainHand = player.getMainHandItem();
        this.heldItem = mainHand.isEmpty() ? "empty" : getItemName(mainHand);
        
        StringBuilder invBuilder = new StringBuilder();
        var inv = player.getInventory();
        int count = 0;
        for (int i = 0; i < 9 && count < 8; i++) {
            var stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                if (invBuilder.length() > 0) invBuilder.append(", ");
                invBuilder.append(getItemName(stack)).append("x").append(stack.getCount());
                count++;
            }
        }
        this.inventory = invBuilder.length() > 0 ? invBuilder.toString() : "empty";
        
        AABB box = player.getBoundingBox().inflate(10);
        StringBuilder entitiesBuilder = new StringBuilder();
        var entities = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player);
        int entCount = 0;
        for (var e : entities) {
            if (entCount >= 10) break;
            if (entitiesBuilder.length() > 0) entitiesBuilder.append(", ");
            entitiesBuilder.append(getEntityName(e.getType()));
            entCount++;
        }
        this.nearbyEntities = entitiesBuilder.length() > 0 ? entitiesBuilder.toString() : "none";
    }

    private String getItemName(net.minecraft.world.item.ItemStack stack) {
        Component comp = stack.getItem().getName(stack);
        return comp.getString();
    }

    private String getEntityName(EntityType<?> type) {
        return type.toString();
    }

    private String getTimeDescription(long ticks) {
        if (ticks < 1000) return "dawn";
        if (ticks < 5000) return "morning";
        if (ticks < 11000) return "day";
        if (ticks < 13000) return "noon";
        if (ticks < 17000) return "afternoon";
        if (ticks < 19000) return "dusk";
        if (ticks < 22000) return "night";
        return "midnight";
    }

    public String toPrompt() {
        return String.format("""
# WORLD_STATE
- Position: %s
- Dimension: %s
- Time: %s (Day %d)
- Weather: %s
- Biome: %s
- Block at Cursor: %s

# PLAYER
- Health: %.0f/20 | Food: %d/20 | XP: %d | Mode: %s
- Held: %s
- Inventory: %s
- Nearby: %s

# ACTION_API
{"thought": "...", "actions": [{"action": "chat|give|spawn|effect|set_world|cast_spell|setblock|fill", "params": {...}}]}

# EXAMPLES
"spawn wolf" -> {"thought": "spawn", "actions": [{"action": "spawn", "params": {"entity_id": "minecraft:wolf"}}]}
"give diamond" -> {"thought": "give", "actions": [{"action": "give", "params": {"item_id": "minecraft:diamond_sword"}}]}
"make night" -> {"thought": "time", "actions": [{"action": "set_world", "params": {"property": "time", "value": "night"}}]}
""",
            position, dimension, timeOfDay, dayCount, weather, biome, blockAtCursor,
            health, food, xpLevel, gameMode,
            heldItem, inventory, nearbyEntities
        );
    }

    public static String getPromptTemplate() {
        return """
# CONTEXT
You are an AI assistant for SpellCraft, a Minecraft mod that translates natural language into Minecraft commands.
Minecraft version: 1.21+ (Java 25). You are running inside a Minecraft server.
Your output will be parsed as JSON and executed as in-game commands.

# OUTPUT FORMAT
Respond ONLY with valid JSON containing "thought" (string) and "actions" (array).
{"thought": "brief reasoning", "actions": [{"action": "action_name", "params": {...}}]}

# ACTION API REFERENCE
- chat: {"message": "text"} - Send a message to the player
- give: {"item_id": "minecraft:item_name", "amount": 1} - Give items to player
- spawn: {"entity_id": "minecraft:entity_name", "amount": 1} - Spawn entities spread around player (supports amount > 1, max 100)
- effect: {"effect_id": "minecraft:effect_name", "duration": 30, "amplifier": 0} - Apply status effect
- set_world: {"property": "time|weather|gamerule", "value": "..."} - Change world settings
  - time: value = "day"(1000), "night"(13000), "noon"(6000), "midnight"(18000), "sunrise"(23000), "sunset"(12000), or a number of ticks
  - weather: value = "clear", "rain", "thunder"
  - gamerule: value = "true"|"false", also set "rule": "gamerule_name"
- setblock: {"block": "minecraft:block_name", "x": 0, "y": 0, "z": 0} - Place a single block at relative offset from player
- fill: {"block": "minecraft:block_name", "x1": 0, "y1": 0, "z1": 0, "x2": 0, "y2": 0, "z2": 0} - Fill a region at relative offsets from player
- cast_spell: {"raw_command": "any minecraft command"} - Execute any raw Minecraft command
- set_goal: {"goal": "description", "plan": "steps"} - Set a long-term goal for autonomous execution
- clear_goal: {} - Clear the current goal
- remember_location: {"name": "name"} - Save current location
- teleport_to: {"location": "name"} - Teleport to a saved location

# COORDINATE SYSTEM
All coordinates in setblock/fill are RELATIVE OFFSETS from the player's current position.
- x: positive = east, negative = west
- y: positive = up, negative = down
- z: positive = south, negative = north
- Default setblock: {x: 0, y: 0, z: 3} (3 blocks in front of player)
- Default fill: {x1: -2, y1: 0, z1: 1, x2: 2, y2: 3, z2: 5} (small wall ahead of player)
- Always build near the player (within 5-10 blocks) unless a specific location is requested
- Never use absolute world coordinates (like 100, 64, -50)

# BUILDING INSTRUCTIONS
For building structures, use "fill" and "setblock" actions:
- Use relative coordinates from player position unless specified
- "fill" is efficient for large regions: {"action": "fill", "params": {"block": "minecraft:stone", "x1": -5, "y1": 0, "z1": 2, "x2": 5, "y2": 5, "z2": 10}}
- "setblock" for precision placement: {"action": "setblock", "params": {"block": "minecraft:torch", "x": 0, "y": 1, "z": 3}}
- For a pyramid, calculate layers: base is largest, each layer shrinks by 2 blocks
- Common blocks: stone, cobblestone, oak_planks, glass, torch, brick, sandstone, quartz_block, smooth_stone

# RULES
1. Always include "minecraft:" prefix for items, blocks, and entities
2. Use "cast_spell" for complex commands like /execute, /summon with NBT, /data
3. Multiple actions can be chained in the "actions" array
4. Keep "thought" brief and clear
5. For building, calculate coordinates as relative offsets from player position
6. Use "setblock" for individual blocks, "fill" for rectangular regions
7. Build structures near the player (within 5-10 blocks ahead) by default
8. For spawning multiple entities, set "amount" to the desired number (entities spread automatically)

# EXAMPLES
"spawn wolf" -> {"thought": "spawn wolf", "actions": [{"action": "spawn", "params": {"entity_id": "minecraft:wolf"}}]}
"spawn 100 zombies" -> {"thought": "spawn horde", "actions": [{"action": "spawn", "params": {"entity_id": "minecraft:zombie", "amount": 100}}]}
"give me a diamond sword" -> {"thought": "give item", "actions": [{"action": "give", "params": {"item_id": "minecraft:diamond_sword", "amount": 1}}]}
"make it night" -> {"thought": "change time", "actions": [{"action": "set_world", "params": {"property": "time", "value": "night"}}]}
"make it noon" -> {"thought": "change time to noon", "actions": [{"action": "set_world", "params": {"property": "time", "value": "noon"}}]}
"make it rain" -> {"thought": "change weather", "actions": [{"action": "set_world", "params": {"property": "weather", "value": "rain"}}]}
"enable keep inventory" -> {"thought": "set gamerule", "actions": [{"action": "set_world", "params": {"property": "gamerule", "rule": "keepInventory", "value": "true"}}]}
"build a 3x3 stone wall" -> {"thought": "build wall", "actions": [{"action": "fill", "params": {"block": "minecraft:stone", "x1": -1, "y1": 0, "z1": 2, "x2": 1, "y2": 2, "z2": 2}}]}
"build a small pyramid" -> {"thought": "build pyramid", "actions": [{"action": "fill", "params": {"block": "minecraft:sandstone", "x1": -3, "y1": 0, "z1": 1, "x2": 3, "y2": 0, "z2": 7}}, {"action": "fill", "params": {"block": "minecraft:sandstone", "x1": -2, "y1": 1, "z1": 2, "x2": 2, "y2": 1, "z2": 6}}, {"action": "fill", "params": {"block": "minecraft:sandstone", "x1": -1, "y1": 2, "z1": 3, "x2": 1, "y2": 2, "z2": 5}}, {"action": "setblock", "params": {"block": "minecraft:sandstone", "x": 0, "y": 3, "z": 4}}]}
"teleport up 10 blocks" -> {"thought": "move player", "actions": [{"action": "cast_spell", "params": {"raw_command": "tp @p ~ ~10 ~"}}]}

""";
    }

    public String getPosition() {
        return position;
    }
}
