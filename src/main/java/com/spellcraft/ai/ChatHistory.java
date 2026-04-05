package com.spellcraft.ai;

import java.util.ArrayDeque;
import java.util.Deque;

public class ChatHistory {
    private static final int MAX_MESSAGES = 20;

    public record ChatMessage(String role, String content) {}

    private final Deque<ChatMessage> messages = new ArrayDeque<>();

    public void addMessage(String role, String content) {
        if (messages.size() >= MAX_MESSAGES) {
            messages.pollFirst();
        }
        messages.addLast(new ChatMessage(role, content));
    }

    public void addExchange(String userQuery, String assistantSummary) {
        addMessage("user", userQuery);
        addMessage("assistant", assistantSummary);
    }

    public String getHistoryPrompt() {
        if (messages.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# CHAT HISTORY\n");
        sb.append("Recent conversation (oldest to newest):\n");

        var list = new java.util.ArrayList<>(messages);
        for (int i = 0; i < list.size(); i++) {
            ChatMessage msg = list.get(i);
            sb.append(msg.role().equals("user") ? "User" : "AI").append(": ").append(msg.content()).append("\n");
        }
        sb.append("\n");

        return sb.toString();
    }

    public void clear() {
        messages.clear();
    }
}
