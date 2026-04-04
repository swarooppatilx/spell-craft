package com.example.ai;

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
{"thought": "...", "actions": [{"action": "chat|give|spawn|effect|set_world|cast_spell", "params": {...}}]}

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
# Minecraft Command Translator - Output ONLY JSON with "thought" and "actions"
- chat: {"message": "text"}
- give: {"item_id": "id", "amount": 1}
- spawn: {"entity_id": "id", "amount": 1}
- effect: {"effect_id": "id", "duration": 30, "amplifier": 0}
- set_world: {"property": "time|weather", "value": "day|night|clear|rain"}
- cast_spell: {"raw_command": "minecraft command"}

Examples:
"spawn wolf" -> {"thought": "spawn", "actions": [{"action": "spawn", "params": {"entity_id": "minecraft:wolf"}}]}
"give diamond" -> {"thought": "give", "actions": [{"action": "give", "params": {"item_id": "minecraft:diamond_sword"}}]}
"make night" -> {"thought": "time", "actions": [{"action": "set_world", "params": {"property": "time", "value": "night"}}]}

""";
    }

    public String getPosition() {
        return position;
    }
}
