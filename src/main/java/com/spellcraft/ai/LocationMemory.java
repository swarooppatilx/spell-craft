package com.spellcraft.ai;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public class LocationMemory {
    private final ServerPlayer player;
    private final Map<String, BlockPos> savedLocations = new HashMap<>();
    
    public LocationMemory(ServerPlayer player) {
        this.player = player;
    }

    public void saveLocation(String name, BlockPos pos) {
        savedLocations.put(name.toLowerCase(), pos);
        sendMessage("§a[Memory] Saved '" + name + "' at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
    }

    public void saveLocation(String name) {
        BlockPos pos = player.blockPosition();
        saveLocation(name, pos);
    }

    public BlockPos getLocation(String name) {
        return savedLocations.get(name.toLowerCase());
    }

    public boolean hasLocation(String name) {
        return savedLocations.containsKey(name.toLowerCase());
    }

    public void forgetLocation(String name) {
        if (savedLocations.remove(name.toLowerCase()) != null) {
            sendMessage("§7[Memory] Forgot '" + name + "'");
        }
    }

    public void clearAll() {
        savedLocations.clear();
        sendMessage("§7[Memory] Cleared all locations");
    }

    public String getLocationsPrompt() {
        if (savedLocations.isEmpty()) return "";
        
        StringBuilder sb = new StringBuilder("\nSAVED_LOCATIONS:\n");
        for (Map.Entry<String, BlockPos> entry : savedLocations.entrySet()) {
            BlockPos pos = entry.getValue();
            sb.append("- ").append(entry.getKey()).append(": [")
              .append(pos.getX()).append(", ")
              .append(pos.getY()).append(", ")
              .append(pos.getZ()).append("]\n");
        }
        return sb.toString();
    }

    public String listLocations() {
        if (savedLocations.isEmpty()) {
            return "§7[Memory] No saved locations";
        }
        StringBuilder sb = new StringBuilder("§a[Saved Locations]: ");
        for (String name : savedLocations.keySet()) {
            sb.append(name).append(", ");
        }
        return sb.toString().replaceAll(", $", "");
    }

    private void sendMessage(String msg) {
        player.sendSystemMessage(Component.literal(msg));
    }
}
