package com.sand_devil_calc;

public record Champion(
        String name,
        ChampionKind kind,
        boolean enabled,
        int hitsPerCycle,
        double damagePerHit,
        double accuracy,
        double attack,
        double critDamage,
        int quickstrikeStones,
        int dungeonDelverStones,
        boolean zealSet,
        double bookedDamageBonus
) {
    public Champion(
            String name,
            ChampionKind kind,
            boolean enabled,
            int hitsPerCycle,
            double damagePerHit,
            double accuracy
    ) {
        this(name, kind, enabled, hitsPerCycle, damagePerHit, accuracy, 0, 0, 0, 0, false, 0);
    }

    public Champion(
            String name,
            ChampionKind kind,
            boolean enabled,
            int hitsPerCycle,
            double damagePerHit,
            double accuracy,
            double attack,
            double critDamage
    ) {
        this(name, kind, enabled, hitsPerCycle, damagePerHit, accuracy,
                attack, critDamage, 0, 0, false, 0);
    }

    public Champion(
            String name,
            ChampionKind kind,
            boolean enabled,
            int hitsPerCycle,
            double damagePerHit,
            double accuracy,
            double attack,
            double critDamage,
            int quickstrikeStones,
            int dungeonDelverStones
    ) {
        this(
                name,
                kind,
                enabled,
                hitsPerCycle,
                damagePerHit,
                accuracy,
                attack,
                critDamage,
                quickstrikeStones,
                dungeonDelverStones,
                false,
                0
        );
    }

    public Champion(
            String name,
            ChampionKind kind,
            boolean enabled,
            int hitsPerCycle,
            double damagePerHit,
            double accuracy,
            double attack,
            double critDamage,
            int quickstrikeStones,
            int dungeonDelverStones,
            boolean zealSet
    ) {
        this(
                name,
                kind,
                enabled,
                hitsPerCycle,
                damagePerHit,
                accuracy,
                attack,
                critDamage,
                quickstrikeStones,
                dungeonDelverStones,
                zealSet,
                0
        );
    }

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
        if (!Double.isFinite(attack) || attack < 0) {
            throw new IllegalArgumentException("Attack must be finite and non-negative.");
        }
        if (!Double.isFinite(critDamage) || critDamage < 0) {
            throw new IllegalArgumentException("Crit Damage must be finite and non-negative.");
        }
        if (!Double.isFinite(bookedDamageBonus) || bookedDamageBonus < 0) {
            throw new IllegalArgumentException("Booked damage bonus must be finite and non-negative.");
        }
        if ((attack == 0) != (critDamage == 0)) {
            throw new IllegalArgumentException(
                    "Attack and Crit Damage must either both be provided or both be omitted."
            );
        }
        if ((attack > 0 || critDamage > 0) && kind != ChampionKind.FENAX) {
            throw new IllegalArgumentException("Explicit Attack and Crit Damage are currently only modeled for Fenax.");
        }
        if (quickstrikeStones < 0 || dungeonDelverStones < 0
                || quickstrikeStones + dungeonDelverStones > 3) {
            throw new IllegalArgumentException(
                    "Fenax can equip at most three Quickstrike and Dungeon Delver gemstones combined."
            );
        }
        if ((quickstrikeStones > 0 || dungeonDelverStones > 0) && kind != ChampionKind.FENAX) {
            throw new IllegalArgumentException("Quickstrike and Dungeon Delver are only modeled for Fenax.");
        }
        if (bookedDamageBonus > 0 && kind != ChampionKind.FENAX) {
            throw new IllegalArgumentException("Booked damage bonuses are only modeled for Fenax.");
        }
    }
}
