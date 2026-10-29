package com.sand_devil_calc;

import java.util.Set;

public record SimulationOptions(
        boolean failRunWithoutSleep,
        boolean assumeHellhoundSleepLands,
        double bossResistance,
        Set<TeamPosition> fearsomePresencePositions,
        Set<TeamPosition> sniperMasteryPositions,
        boolean hellhoundHarvestDespairEnabled,
        boolean hellhoundHarvestDespairSniperEnabled,
        boolean sporeburstRodEnabled,
        int sporeburstRodStars,
        boolean smiteEnabled,
        int smiteStars
) {
    public SimulationOptions {
        if (!Double.isFinite(bossResistance) || bossResistance < 0) {
            throw new IllegalArgumentException("Boss resistance must be finite and non-negative.");
        }
        if (fearsomePresencePositions == null || sniperMasteryPositions == null) {
            throw new IllegalArgumentException("Mastery positions are required.");
        }
        fearsomePresencePositions = Set.copyOf(fearsomePresencePositions);
        sniperMasteryPositions = Set.copyOf(sniperMasteryPositions);
        validateStars("Sporeburst Rod", sporeburstRodStars);
        validateStars("Smite", smiteStars);
    }

    private static void validateStars(String label, int stars) {
        if (stars < 1 || stars > 6) {
            throw new IllegalArgumentException(label + " stars must be between 1 and 6.");
        }
    }

    public double sporeburstRodChance() {
        return 0.10 + 0.05 * (sporeburstRodStars - 1);
    }

    public double smiteChance() {
        return switch (smiteStars) {
            case 1, 2 -> 0.15;
            case 3, 4 -> 0.30;
            case 5 -> 0.60;
            case 6 -> 1.0;
            default -> throw new IllegalStateException("Validated Smite stars are out of range.");
        };
    }

    public boolean fearsomePresenceEnabled(TeamPosition position) {
        return fearsomePresencePositions.contains(position);
    }

    public boolean sniperMasteryEnabled(TeamPosition position) {
        return sniperMasteryPositions.contains(position);
    }
}
