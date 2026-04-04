package com.spellcraft.ai;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class GoalManager {
    private final ServerPlayer player;
    private final CommandExecutor executor;
    
    private String currentGoal = null;
    private Deque<String> goalSteps = new ArrayDeque<>();
    private int tickCounter = 0;
    private int lastX, lastY, lastZ;
    private int stuckCounter = 0;
    private long lastActionTime = 0;
    
    private static final int AUTO_TRIGGER_TICKS = 600;
    private static final int STUCK_THRESHOLD = 200;
    
    public GoalManager(ServerPlayer player) {
        this.player = player;
        this.executor = new CommandExecutor();
        this.lastX = player.getBlockX();
        this.lastY = player.getBlockY();
        this.lastZ = player.getBlockZ();
    }

    public boolean hasActiveGoal() {
        return currentGoal != null && !currentGoal.isEmpty();
    }

    public void setGoal(String goal, String planText) {
        this.currentGoal = goal;
        parsePlanSteps(planText);
        this.lastActionTime = System.currentTimeMillis();
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[Goal] Set: " + goal));
    }

    private void parsePlanSteps(String planText) {
        goalSteps.clear();
        if (planText == null || planText.isEmpty()) return;
        
        String[] parts = planText.split("[,\\n]");
        for (String step : parts) {
            String trimmed = step.trim();
            if (!trimmed.isEmpty()) {
                goalSteps.add(trimmed);
            }
        }
    }

    public String getNextStep() {
        return goalSteps.peek();
    }

    public void completeCurrentStep() {
        if (!goalSteps.isEmpty()) {
            String completed = goalSteps.poll();
            if (goalSteps.isEmpty()) {
                currentGoal = null;
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[Goal] Completed!"));
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e[Goal] Step done. " + goalSteps.size() + " remaining."));
            }
        }
        lastActionTime = System.currentTimeMillis();
    }

    public String getGoalPrompt() {
        if (!hasActiveGoal()) return "";
        
        String progress = getProgressText();
        String stuckWarning = getStuckWarning();
        
        return String.format("""
CURRENT_GOAL: "%s"
PROGRESS: %s
NEXT_STEP: %s
%s""",
            currentGoal,
            progress,
            getNextStep() != null ? getNextStep() : "Complete the goal",
            stuckWarning
        );
    }

    private String getProgressText() {
        int total = goalSteps.size() + (getNextStep() != null ? 1 : 0);
        return "\"" + (goalSteps.size() + 1) + " steps remaining\"";
    }

    private String getStuckWarning() {
        int currentX = player.getBlockX();
        int currentY = player.getBlockY();
        int currentZ = player.getBlockZ();
        
        boolean moved = Math.abs(currentX - lastX) > 1 || 
                       Math.abs(currentY - lastY) > 1 || 
                       Math.abs(currentZ - lastZ) > 1;
        
        if (!moved) {
            stuckCounter++;
        } else {
            stuckCounter = 0;
            lastX = currentX;
            lastY = currentY;
            lastZ = currentZ;
        }
        
        if (stuckCounter > STUCK_THRESHOLD) {
            stuckCounter = 0;
            return "\nSYSTEM: You haven't moved in 10 seconds. You may be stuck. Try a different approach.\n";
        }
        
        return "";
    }

    public boolean shouldAutoTrigger() {
        if (!hasActiveGoal()) return false;
        
        tickCounter++;
        if (tickCounter >= AUTO_TRIGGER_TICKS) {
            tickCounter = 0;
            return true;
        }
        return false;
    }

    public void updatePosition() {
        tickCounter++;
        
        int cx = player.getBlockX();
        int cy = player.getBlockY();
        int cz = player.getBlockZ();
        
        if (cx != lastX || cy != lastY || cz != lastZ) {
            stuckCounter = 0;
            lastX = cx;
            lastY = cy;
            lastZ = cz;
        }
        
        if (hasActiveGoal() && stuckCounter > STUCK_THRESHOLD && tickCounter % 20 == 0) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[System] No movement detected. You may be stuck. Try /ai clear goal to cancel."));
        }
    }

    public void clearGoal() {
        currentGoal = null;
        goalSteps.clear();
        stuckCounter = 0;
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7[Goal] Cleared."));
    }

    public String getCurrentGoal() {
        return currentGoal;
    }
}
