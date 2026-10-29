package com.sand_devil_calc;

public record Champion(
        String name,
        ChampionKind kind,
        boolean enabled,
        int hitsPerCycle,
        double damagePerHit,
        double accuracy
) {
    public Champion {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Champion name must not be blank.");
        }
        if (kind == null) {
            throw new IllegalArgumentException("Champion kind is required.");
        }
        if (hitsPerCycle < 0) {
            throw new IllegalArgumentException("Hit count must not be negative.");
        }
        if (!Double.isFinite(damagePerHit) || damagePerHit < 0) {
            throw new IllegalArgumentException("Damage per hit must be finite and non-negative.");
        }
        if (!Double.isFinite(accuracy) || accuracy < 0) {
            throw new IllegalArgumentException("Accuracy must be finite and non-negative.");
        }
    }
}
