package com.spellcraft.ai;

import java.util.ArrayList;
import java.util.List;

public class StructureMemory {
    private static final int MAX_STRUCTURES = 20;

    public record Structure(
        String name,
        String material,
        String description,
        int x,
        int y,
        int z,
        int width,
        int height,
        int depth
    ) {}

    private final List<Structure> structures = new ArrayList<>();

    public StructureMemory() {
    }

    public void addStructure(String name, String material, String description, int x, int y, int z, int width, int height, int depth) {
        if (structures.size() >= MAX_STRUCTURES) {
            structures.remove(0);
        }
        structures.add(new Structure(name, material, description, x, y, z, width, height, depth));
    }

    public void updateStructure(String name, String newMaterial, String newDescription) {
        for (int i = 0; i < structures.size(); i++) {
            Structure s = structures.get(i);
            if (s.name().equalsIgnoreCase(name)) {
                String material = newMaterial != null ? newMaterial : s.material();
                String description = newDescription != null ? newDescription : s.description();
                structures.set(i, new Structure(name, material, description, s.x(), s.y(), s.z(), s.width(), s.height(), s.depth()));
                return;
            }
        }
    }

    public List<Structure> getStructures() {
        return List.copyOf(structures);
    }

    public String getStructuresPrompt() {
        if (structures.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# KNOWN STRUCTURES\n");
        sb.append("These are structures you previously built in this session.\n");
        sb.append("Use these to resolve references like 'the house', 'the wall', etc.\n");
        sb.append("Coordinates are ABSOLUTE world positions.\n\n");

        for (Structure s : structures) {
            sb.append("- ").append(s.name())
                .append(": ").append(s.material())
                .append(", ").append(s.description())
                .append(", size: ").append(s.width()).append("x").append(s.height()).append("x").append(s.depth())
                .append(", at: x=").append(s.x()).append(" y=").append(s.y()).append(" z=").append(s.z())
                .append("\n");
        }
        sb.append("\n");

        return sb.toString();
    }

    public void clear() {
        structures.clear();
    }
}
