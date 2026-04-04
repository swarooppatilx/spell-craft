package com.spellcraft.ai;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public class ReflexHandler {
    private final ServerPlayer player;
    private final CommandExecutor executor;
    private int tickCounter = 0;
    private boolean reflexTriggered = false;

    public ReflexHandler(ServerPlayer player) {
        this.player = player;
        this.executor = new CommandExecutor();
    }

    public boolean checkReflexes() {
        tickCounter++;
        reflexTriggered = false;

        if (tickCounter < 20) return false;
        tickCounter = 0;

        if (checkFire()) return true;
        if (checkDrowning()) return true;
        if (checkLowHealth()) return true;
        if (checkFalling()) return true;

        return false;
    }

    private boolean checkFire() {
        if (player.isOnFire()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6[Reflex] Extinguishing fire!"));
            executor.execute("execute at @s run setblock ~ ~ ~ water", player);
            executor.execute("effect @s fire_resistance 5 0", player);
            reflexTriggered = true;
            return true;
        }
        return false;
    }

    private boolean checkDrowning() {
        if (player.isUnderWater() && player.getAirSupply() < 300) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6[Reflex] Giving water breathing!"));
            executor.execute("effect @s water_breathing 30 0", player);
            reflexTriggered = true;
            return true;
        }
        return false;
    }

    private boolean checkLowHealth() {
        if (player.getHealth() < 5) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6[Reflex] Low health! Healing..."));
            executor.execute("effect @s regeneration 10 2", player);
            executor.execute("effect @s instant_health 1 0", player);
            reflexTriggered = true;
            return true;
        }
        return false;
    }

    private boolean checkFalling() {
        if (player.fallDistance > 10) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6[Reflex] Soft landing!"));
            executor.execute("effect @s slow_falling 5 0", player);
            executor.execute("effect @s resistance 3 2", player);
            reflexTriggered = true;
            return true;
        }
        return false;
    }

    public boolean wasTriggered() {
        return reflexTriggered;
    }
}
