package com.sand_devil_calc;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Simulator {
    private static final double TOXIC_PROC_CHANCE = 0.75;
    private static final double TOXIC_SNIPER_PROC_CHANCE = 0.80;
    private static final double DAZE_PROC_CHANCE = 0.25;
    private static final double DAZE_FEARSOME_PROC_CHANCE = 0.30;
    private static final double HARVEST_DESPAIR_PROC_CHANCE = 0.60;
    private static final double HARVEST_DESPAIR_SNIPER_PROC_CHANCE = 0.65;
    private static final double FERAL_DAMAGE_PER_DEBUFF = 0.05;
    private static final double WEAKEN_DAMAGE_BONUS = 0.25;
    private static final double ATHEL_WEAK_AFFINITY_FAILURE_CHANCE = 0.35;

    private final DamageModel damageModel;

    public Simulator(DamageModel damageModel) {
        if (damageModel == null) {
            throw new IllegalArgumentException("Damage model is required.");
        }
        this.damageModel = damageModel;
    }

    public SimulationResult run(
            long trials,
            Team team,
            GearSetup setup,
            SimulationOptions options,
            Random random
    ) {
        if (trials <= 0) {
            throw new IllegalArgumentException("Trial count must be greater than zero.");
        }
        if (team == null || setup == null || options == null || random == null) {
            throw new IllegalArgumentException("Team, setup, options, and random source are required.");
        }
        if (team.at(TeamPosition.ALLY_ATTACKER).enabled()
                && team.at(TeamPosition.ALLY_ATTACKER).kind() == ChampionKind.MIKAGE) {
            throw new IllegalStateException(
                    "Mikage's ally-attack damage is not modeled yet; keep her disabled."
            );
        }
        Champion sleeper = team.at(TeamPosition.SLEEPER);
        if (options.failRunWithoutSleep()
                && sleeper.enabled()
                && sleeper.kind() != ChampionKind.HELLHOUND) {
            throw new IllegalArgumentException(
                    "Hellhound is the only modeled skill-based sleeper; assign him to the sleeper position."
            );
        }
        if (options.sporeburstRodEnabled()
                && team.inTurnOrder().stream().noneMatch(
                        champion -> champion.enabled() && champion.kind() == ChampionKind.MITHRALA
                )) {
            throw new IllegalArgumentException(
                    "Sporeburst Rod requires an enabled poisoner; Mithrala is the only modeled poisoner."
            );
        }
        for (TeamPosition position : TeamPosition.values()) {
            if (setup.gearFor(position) == GearSet.DAZE
                    && (position != TeamPosition.SLEEPER
                    || team.at(position).kind() != ChampionKind.HELLHOUND)) {
                throw new IllegalArgumentException(
                        "Daze is only modeled for Hellhound in the sleeper position."
                );
            }
        }

        List<Champion> enabledChampions = team.inTurnOrder().stream()
                .filter(Champion::enabled)
                .toList();
        double baseDamagePerCycle = enabledChampions.stream()
                .mapToDouble(champion -> champion.hitsPerCycle() * champion.damagePerHit())
                .sum();

        double averageDamage = 0;
        double sumSquaredDamageDeviations = 0;
        long totalDebuffs = 0;
        long successfulTrials = 0;
        long sleepFailedTrials = 0;
        long affinityFailedTrials = 0;
        long damageSampleTrials = 0;
        Map<Integer, Long> debuffCounts = new HashMap<>();

        for (long trial = 0; trial < trials; trial++) {
            TrialState state = runTrial(team, setup, options, random);
            if (state.sleepFailed) {
                sleepFailedTrials++;
                continue;
            }

            damageSampleTrials++;
            double trialDamage;
            if (state.affinityFailed) {
                affinityFailedTrials++;
                trialDamage = 0;
            } else {
                successfulTrials++;
                trialDamage = calculateDamage(baseDamagePerCycle, state, setup);
                totalDebuffs += state.debuffsLanded;
                debuffCounts.merge(state.debuffsLanded, 1L, Long::sum);
            }
            double deltaFromAverage = trialDamage - averageDamage;
            averageDamage += deltaFromAverage / damageSampleTrials;
            sumSquaredDamageDeviations += deltaFromAverage * (trialDamage - averageDamage);
        }

        Map<Integer, Double> probabilities = new HashMap<>();
        for (Map.Entry<Integer, Long> entry : debuffCounts.entrySet()) {
            probabilities.put(
                    entry.getKey(),
                    successfulTrials == 0 ? 0 : (double) entry.getValue() / successfulTrials
            );
        }

        return new SimulationResult(
                setup.name(),
                trials,
                successfulTrials,
                sleepFailedTrials,
                affinityFailedTrials,
                damageSampleTrials,
                baseDamagePerCycle,
                averageDamage,
                damageSampleTrials == 0 ? 0 : Math.sqrt(sumSquaredDamageDeviations / damageSampleTrials),
                successfulTrials == 0 ? 0 : (double) totalDebuffs / successfulTrials,
                probabilities
        );
    }

    private TrialState runTrial(
            Team team,
            GearSetup setup,
            SimulationOptions options,
            Random random
    ) {
        TrialState state = new TrialState();

        // This single pass is the first-turn sequence; there is no second team pass.
        for (TeamPosition position : TeamPosition.values()) {
            Champion champion = team.at(position);
            if (!champion.enabled()) {
                if (position == TeamPosition.SLEEPER && options.failRunWithoutSleep()) {
                    state.sleepFailed = true;
                    return state;
                }
                continue;
            }

            GearSet gearSet = setup.gearFor(position);
            for (int hit = 0; hit < champion.hitsPerCycle(); hit++) {
                if (champion.kind() == ChampionKind.ATHEL
                        && random.nextDouble() < ATHEL_WEAK_AFFINITY_FAILURE_CHANCE) {
                    state.affinityFailed = true;
                    return state;
                }

                applyChampionSkill(champion, position, hit, options, state, random);

                if (position == TeamPosition.SLEEPER && gearSet == GearSet.DAZE
                        && random.nextDouble() < dazeChance(options, position)) {
                    if (state.applySleep(position)) {
                        applyHarvestDespair(position, options, state, random);
                    }
                }

                if (position == TeamPosition.SLEEPER
                        && options.failRunWithoutSleep()
                        && !state.sleepLanded) {
                    if (hit == champion.hitsPerCycle() - 1) {
                        state.sleepFailed = true;
                        return state;
                    }
                    continue;
                }

                double toxicRoll = random.nextDouble();
                if (gearSet == GearSet.TOXIC) {
                    double toxicChance = options.sniperMasteryEnabled(position)
                            ? TOXIC_SNIPER_PROC_CHANCE
                            : TOXIC_PROC_CHANCE;
                    if (toxicRoll < toxicChance) {
                        state.applyDebuff(position);
                    }
                }
            }
            if (position == TeamPosition.SLEEPER
                    && options.failRunWithoutSleep()
                    && !state.sleepLanded) {
                state.sleepFailed = true;
                return state;
            }
        }

        return state;
    }

    private static void applyChampionSkill(
            Champion champion,
            TeamPosition position,
            int hit,
            SimulationOptions options,
            TrialState state,
            Random random
    ) {
        switch (champion.kind()) {
            case HELLHOUND -> {
                if (hit == 0 && (options.assumeHellhoundSleepLands()
                        || trySkillDebuff(1.0, champion, options, random))) {
                    if (state.applySleep(position)) {
                        applyHarvestDespair(position, options, state, random);
                    }
                }
            }
            case MITHRALA -> {
                if (trySkillDebuff(1.0, champion, options, random)) {
                    state.applyDebuff(position);
                    double rodRoll = random.nextDouble();
                    if (options.sporeburstRodEnabled()
                            && rodRoll < options.sporeburstRodChance()) {
                        state.applyDebuff(position);
                    }
                }
                applySmite(champion, position, options, state, random);
            }
            case ATHEL -> {
                if (hit == champion.hitsPerCycle() - 1
                        && trySkillDebuff(1.0, champion, options, random)) {
                    state.applyDebuff(position);
                    state.weakenLanded = true;
                }
            }
            case DOROTHY -> {
                if (hit == champion.hitsPerCycle() - 1
                        && trySkillDebuff(1.0, champion, options, random)) {
                    state.applyDebuff(position);
                    state.weakenLanded = true;
                }
                applySmite(champion, position, options, state, random);
            }
            case WYSTERI_VINEGUARD -> {
                double weakenChance = options.wysteriWeakenChance(position);
                if (random.nextDouble() < weakenChance
                        && !isResisted(champion, options, random)) {
                    if (!state.weakenLanded) {
                        state.applyDebuff(position);
                        state.weakenLanded = true;
                    }
                }
            }
            case MIKAGE, FENAX -> {
            }
        }
    }

    private static void applySmite(
            Champion champion,
            TeamPosition position,
            SimulationOptions options,
            TrialState state,
            Random random
    ) {
        double smiteRoll = random.nextDouble();
        boolean smiteLanded = options.smiteEnabled()
                && smiteRoll < options.smiteChance()
                && (options.smiteStars() == 6
                || !isResisted(champion, options, random))
                && !state.smiteLanded;
        if (smiteLanded) {
            state.applyDebuff(position);
            state.smiteLanded = true;
        }
    }

    private static void applyHarvestDespair(
            TeamPosition position,
            SimulationOptions options,
            TrialState state,
            Random random
    ) {
        if (!options.hellhoundHarvestDespairEnabled()) {
            return;
        }
        double chance = options.hellhoundHarvestDespairSniperEnabled()
                ? HARVEST_DESPAIR_SNIPER_PROC_CHANCE
                : HARVEST_DESPAIR_PROC_CHANCE;
        if (random.nextDouble() < chance) {
            state.applyDebuff(position);
        }
    }

    private static boolean trySkillDebuff(
            double procChance,
            Champion champion,
            SimulationOptions options,
            Random random
    ) {
        double chanceToLand = procChance * (
                1.0 - DebuffResistance.failureProbability(
                        champion.accuracy(),
                        options.bossResistance()
                )
        );
        return random.nextDouble() < chanceToLand;
    }

    private static boolean isResisted(
            Champion champion,
            SimulationOptions options,
            Random random
    ) {
        return random.nextDouble()
                < DebuffResistance.failureProbability(champion.accuracy(), options.bossResistance());
    }

    private static double dazeChance(SimulationOptions options, TeamPosition position) {
        return options.fearsomePresenceEnabled(position)
                ? DAZE_FEARSOME_PROC_CHANCE
                : DAZE_PROC_CHANCE;
    }

    private double calculateDamage(double baseDamagePerCycle, TrialState state, GearSetup setup) {
        double feralDamageBonus = 0;
        for (Map.Entry<TeamPosition, Integer> entry : state.debuffsByPosition.entrySet()) {
            if (setup.gearFor(entry.getKey()) == GearSet.FERAL) {
                feralDamageBonus += FERAL_DAMAGE_PER_DEBUFF * entry.getValue();
            }
        }

        double weakenMultiplier = state.weakenLanded
                ? 1.0 + WEAKEN_DAMAGE_BONUS
                : 1.0;
        return baseDamagePerCycle
                * damageModel.damageMultiplier(state.debuffsLanded)
                * (1.0 + feralDamageBonus)
                * weakenMultiplier;
    }

    private static final class TrialState {
        private final Map<TeamPosition, Integer> debuffsByPosition = new EnumMap<>(TeamPosition.class);
        private int debuffsLanded;
        private boolean sleepLanded;
        private boolean weakenLanded;
        private boolean smiteLanded;
        private boolean sleepFailed;
        private boolean affinityFailed;

        private void applyDebuff(TeamPosition position) {
            debuffsLanded++;
            debuffsByPosition.merge(position, 1, Integer::sum);
        }

        private boolean applySleep(TeamPosition position) {
            if (!sleepLanded) {
                applyDebuff(position);
                sleepLanded = true;
                return true;
            }
            return false;
        }
    }
}
