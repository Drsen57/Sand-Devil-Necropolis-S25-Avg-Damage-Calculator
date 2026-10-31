package com.sand_devil_calc;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
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
    private static final double FENAX_HIT_DAMAGE_VARIATION = 0.10;
    private static final double FENAX_STRONG_AFFINITY_BONUS_CHANCE = 0.30;
    private static final double FENAX_STRONG_AFFINITY_BONUS = 0.30;
    private static final double MIKAGE_INCREASE_ATTACK = 0.50;
    private static final double MIKAGE_INCREASE_CRIT_DAMAGE = 0.30;
    private static final double RUTHLESS_AMBUSH_MULTIPLIER = 1.08;
    private static final double OPPORTUNIST_MULTIPLIER = 1.12;
    private static final double BRING_IT_DOWN_MULTIPLIER = 1.06;
    private static final double SINGLE_OUT_MULTIPLIER = 1.08;
    private static final double SINGLE_OUT_HP_THRESHOLD = 0.40;
    private static final double QUICKSTRIKE_MULTIPLIER = 1.10;
    private static final double DUNGEON_DELVER_MULTIPLIER = 1.10;
    private static final double ZEAL_SET_MULTIPLIER = 1.12;
    private static final double BOSS_MAX_HP = 3_082_910;
    private static final double FENAX_MASTERY_MULTIPLIER =
            RUTHLESS_AMBUSH_MULTIPLIER
                    * OPPORTUNIST_MULTIPLIER
                    * BRING_IT_DOWN_MULTIPLIER;

    private final DamageModel damageModel;
    private final boolean includeOverkillDamage;

    public Simulator(DamageModel damageModel) {
        this(damageModel, true);
    }

    public Simulator(DamageModel damageModel, boolean includeOverkillDamage) {
        if (damageModel == null) {
            throw new IllegalArgumentException("Damage model is required.");
        }
        this.damageModel = damageModel;
        this.includeOverkillDamage = includeOverkillDamage;
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
        double baseFenaxDamagePerCycle = enabledChampions.stream()
                .filter(champion -> champion.kind() == ChampionKind.FENAX)
                .mapToDouble(champion -> fenaxBaseDamagePerCycle(
                        champion, isMikageAllyAttacker(team)
                ))
                .sum();
        Map<TeamPosition, DamageAccumulator> fenaxDamageByPosition =
                new EnumMap<>(TeamPosition.class);
        for (TeamPosition position : TeamPosition.values()) {
            Champion champion = team.at(position);
            if (champion.enabled() && champion.kind() == ChampionKind.FENAX) {
                fenaxDamageByPosition.put(
                        position,
                        new DamageAccumulator(
                                champion.name(),
                                fenaxBaseDamagePerCycle(
                                        champion, isMikageAllyAttacker(team)
                                )
                        )
                );
            }
        }

        DamageAccumulator totalFenaxDamage = new DamageAccumulator(
                "All Fenaxes",
                baseFenaxDamagePerCycle
        );
        double averageDebuffDamageMultiplier = 0;
        double sumSquaredDebuffMultiplierDeviations = 0;
        long totalDebuffs = 0;
        long successfulTrials = 0;
        long sleepFailedTrials = 0;
        long sleepResistFailedTrials = 0;
        long affinityFailedTrials = 0;
        long damageSampleTrials = 0;
        long bossKilledTrials = 0;
        long damageFailedTrials = 0;
        long survivedTurnThreeTrials = 0;
        double worstTurnThreeMissPercent = 0;
        int worstTurnThreeDebuffsLanded = 0;
        int worstTurnThreeActiveDebuffs = 0;
        double worstTurnFourMissPercent = 0;
        int worstTurnFourDebuffsLanded = 0;
        int worstTurnFourActiveDebuffs = 0;
        Map<Integer, Long> bossKillsByTurn = new HashMap<>(Map.of(1, 0L, 3, 0L, 4, 0L));
        Map<Integer, Long> debuffCounts = new HashMap<>();

        for (long trial = 0; trial < trials; trial++) {
            TrialState state = runTrial(team, setup, options, random);
            if (state.sleepFailed) {
                sleepFailedTrials++;
                if (state.sleepResisted) {
                    sleepResistFailedTrials++;
                }
                continue;
            }
            if (!state.affinityFailed && state.turnThreeCheckpoint
                    && state.turnThreeRemainingHp > 0) {
                survivedTurnThreeTrials++;
                if (state.turnThreeDamage > 0) {
                    double missPercent =
                            state.turnThreeRemainingHp / state.turnThreeDamage * 100;
                    if (missPercent > worstTurnThreeMissPercent) {
                        worstTurnThreeMissPercent = missPercent;
                        worstTurnThreeDebuffsLanded = state.debuffsLanded;
                        worstTurnThreeActiveDebuffs = state.turnThreeActiveDebuffs;
                    }
                }
            }
            if (!state.affinityFailed && !state.bossKilled && state.fenaxDamage > 0) {
                double missPercent = state.bossHpRemaining / state.fenaxDamage * 100;
                if (missPercent > worstTurnFourMissPercent) {
                    worstTurnFourMissPercent = missPercent;
                    worstTurnFourDebuffsLanded = state.debuffsLanded;
                    worstTurnFourActiveDebuffs = state.activeDebuffCount();
                }
            }

            damageSampleTrials++;
            if (state.bossKilled) {
                bossKilledTrials++;
                bossKillsByTurn.merge(state.bossKilledTurn, 1L, Long::sum);
            } else if (!state.affinityFailed) {
                damageFailedTrials++;
            }
            if (state.affinityFailed) {
                affinityFailedTrials++;
            } else {
                successfulTrials++;
                totalDebuffs += state.debuffsLanded;
                debuffCounts.merge(state.debuffsLanded, 1L, Long::sum);

                double trialDebuffMultiplier = calculateDebuffDamageMultiplier(state, setup);
                double deltaFromDebuffAverage =
                        trialDebuffMultiplier - averageDebuffDamageMultiplier;
                averageDebuffDamageMultiplier += deltaFromDebuffAverage / successfulTrials;
                sumSquaredDebuffMultiplierDeviations += deltaFromDebuffAverage
                        * (trialDebuffMultiplier - averageDebuffDamageMultiplier);
            }
            double trialFenaxDamage = state.affinityFailed ? 0 : state.fenaxDamage;
            totalFenaxDamage.add(trialFenaxDamage);
            for (Map.Entry<TeamPosition, DamageAccumulator> entry : fenaxDamageByPosition.entrySet()) {
                double championDamage = state.affinityFailed
                        ? 0
                        : state.fenaxDamageByPosition.getOrDefault(entry.getKey(), 0.0);
                entry.getValue().add(championDamage);
            }
        }

        Map<Integer, Double> probabilities = new HashMap<>();
        for (Map.Entry<Integer, Long> entry : debuffCounts.entrySet()) {
            probabilities.put(
                    entry.getKey(),
                    successfulTrials == 0 ? 0 : (double) entry.getValue() / successfulTrials
            );
        }

        Map<TeamPosition, SimulationResult.FenaxDamageStatistics> fenaxStatistics =
                new EnumMap<>(TeamPosition.class);
        for (Map.Entry<TeamPosition, DamageAccumulator> entry : fenaxDamageByPosition.entrySet()) {
            fenaxStatistics.put(entry.getKey(), entry.getValue().toStatistics());
        }

        return new SimulationResult(
                setup.name(),
                trials,
                successfulTrials,
                sleepFailedTrials,
                sleepResistFailedTrials,
                affinityFailedTrials,
                damageSampleTrials,
                bossKilledTrials,
                damageFailedTrials,
                bossKillsByTurn,
                survivedTurnThreeTrials,
                worstTurnThreeMissPercent,
                worstTurnThreeDebuffsLanded,
                worstTurnThreeActiveDebuffs,
                worstTurnFourMissPercent,
                worstTurnFourDebuffsLanded,
                worstTurnFourActiveDebuffs,
                baseFenaxDamagePerCycle,
                totalFenaxDamage.average(),
                totalFenaxDamage.standardDeviation(),
                averageDebuffDamageMultiplier,
                successfulTrials == 0
                        ? 0 : Math.sqrt(sumSquaredDebuffMultiplierDeviations / successfulTrials),
                successfulTrials == 0 ? 0 : (double) totalDebuffs / successfulTrials,
                probabilities,
                fenaxStatistics,
                Map.of(),
                null
        );
    }

    public SimulationResult runWithDamageBands(
            long trials,
            Team team,
            GearSetup setup,
            SimulationOptions options,
            long seed
    ) {
        SimulationResult result = run(
                trials,
                team,
                setup,
                options,
                new Random(seed)
        );
        Map<Integer, DamageBandAccumulator> accumulators = new HashMap<>();
        DamageBandAccumulator allTrialsAccumulator = new DamageBandAccumulator();
        accumulators.put(1, new DamageBandAccumulator());
        accumulators.put(2, new DamageBandAccumulator());
        accumulators.put(3, new DamageBandAccumulator());
        accumulators.put(-1, allTrialsAccumulator);
        Random replayRandom = new Random(seed);
        double averageDamage = result.averageFenaxDamagePerCycle();
        double standardDeviation = result.fenaxDamageStandardDeviation();
        List<SimulationResult.DamageTrialExample> damageTrials = new ArrayList<>();
        SimulationResult.DamageTrialExample meanTrial = null;
        double meanTrialDistance = Double.POSITIVE_INFINITY;

        for (long trial = 0; trial < trials; trial++) {
            TrialState state = runTrial(team, setup, options, replayRandom);
            if (state.sleepFailed) {
                continue;
            }

            double trialDamage = state.affinityFailed ? 0 : state.fenaxDamage;
            SimulationResult.DamageTrialExample example =
                    damageTrialExample(state, trial + 1, trialDamage);
            damageTrials.add(example);
            double distanceFromMean = Math.abs(trialDamage - averageDamage);
            if (distanceFromMean < meanTrialDistance) {
                meanTrial = example;
                meanTrialDistance = distanceFromMean;
            }
            allTrialsAccumulator.add(state);
            for (int standardDeviations = 1; standardDeviations <= 3; standardDeviations++) {
                double bandWidth = standardDeviations * standardDeviation;
                if (trialDamage >= averageDamage - bandWidth
                        && trialDamage <= averageDamage + bandWidth) {
                    accumulators.get(standardDeviations).add(state);
                }
            }
        }

        Map<Integer, SimulationResult.DamageBandStatistics> statistics = new HashMap<>();
        for (Map.Entry<Integer, DamageBandAccumulator> entry : accumulators.entrySet()) {
            statistics.put(entry.getKey(), entry.getValue().toStatistics());
        }
        damageTrials.sort(
                Comparator.comparingDouble(SimulationResult.DamageTrialExample::totalFenaxDamage)
        );
        SimulationResult.DamageTrialExample medianTrial = damageTrials.isEmpty()
                ? null
                : damageTrials.get((damageTrials.size() - 1) / 2);
        return result.withDamageBandStatistics(
                statistics,
                new SimulationResult.RepresentativeDamageTrials(medianTrial, meanTrial)
        );
    }

    private static SimulationResult.DamageTrialExample damageTrialExample(
            TrialState state,
            long trialNumber,
            double totalFenaxDamage
    ) {
        return new SimulationResult.DamageTrialExample(
                trialNumber,
                totalFenaxDamage,
                state.turnOneDamage,
                state.turnThreeDamage,
                state.turnThreeRemainingHp,
                state.bossHpRemaining,
                state.debuffsLanded,
                state.turnThreeActiveDebuffs,
                state.activeDebuffCount(),
                state.bossKilledTurn,
                state.affinityFailed
        );
    }

    private TrialState runTrial(
            Team team,
            GearSetup setup,
            SimulationOptions options,
            Random random
    ) {
        TrialState state = new TrialState();
        boolean mikageBuffsActive = isMikageAllyAttacker(team);

        // Process the full team sequence on turn one; only Fenax acts again below.
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
                if (champion.kind() == ChampionKind.FENAX) {
                    applyFenaxStrike(champion, position, state, mikageBuffsActive, random);
                    if (state.bossHpRemaining <= 0) {
                        break;
                    }
                }

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
            if (state.bossHpRemaining <= 0) {
                break;
            }
        }
        state.turnOneDamage = state.fenaxDamage;

        if (!state.sleepFailed && !state.affinityFailed && state.bossHpRemaining > 0) {
            state.currentTurn = 2;
            state.advanceTurn();
            List<TeamPosition> fenaxPositions = List.of(
                    TeamPosition.DAMAGE_DEALER_ONE,
                    TeamPosition.DAMAGE_DEALER_TWO
            );
            for (int index = 0; index < fenaxPositions.size(); index++) {
                TeamPosition position = fenaxPositions.get(index);
                if (index == 0) {
                    state.currentTurn = 3;
                } else {
                    state.currentTurn = 4;
                }
                Champion champion = team.at(position);
                if (champion.enabled() && champion.kind() == ChampionKind.FENAX) {
                    for (int hit = 0; hit < champion.hitsPerCycle(); hit++) {
                        applyFenaxStrike(champion, position, state, mikageBuffsActive, random);
                        if (state.bossHpRemaining <= 0) {
                            break;
                        }
                    }
                }
                if (index == 0) {
                    state.turnThreeCheckpoint = true;
                    state.turnThreeRemainingHp = state.bossHpRemaining;
                    state.turnThreeDamage = state.fenaxDamage;
                    state.turnThreeActiveDebuffs = state.activeDebuffCount();
                }
                if (state.bossHpRemaining <= 0) {
                    break;
                }
            }
        }
        state.bossKilled = state.bossHpRemaining <= 0;
        return state;
    }

    private void applyFenaxStrike(
            Champion champion,
            TeamPosition position,
            TrialState state,
            boolean mikageBuffsActive,
            Random random
    ) {
        double debuffMultiplier = damageModel.damageMultiplier(state.activeDebuffCount());
        double hitDamage = calculateFenaxHitDamage(
                fenaxDamagePerHit(champion, mikageBuffsActive)
                        * fenaxGemstoneMultiplier(champion, state.currentTurn == 1)
                        * fenaxGearMultiplier(champion)
                        * fenaxStrikeMasteryMultiplier(state)
                        * debuffMultiplier,
                random
        );
        double recordedDamage = includeOverkillDamage
                ? hitDamage
                : Math.min(hitDamage, state.bossHpRemaining);
        state.fenaxDamage += recordedDamage;
        state.fenaxDamageByPosition.merge(position, recordedDamage, Double::sum);
        state.bossHpRemaining = Math.max(0, state.bossHpRemaining - hitDamage);
        if (state.bossHpRemaining <= 0) {
            state.bossKilledTurn = state.currentTurn;
        }
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
                if (hit == 0) {
                    boolean sleepLanded = options.assumeHellhoundSleepLands()
                            || trySkillDebuff(1.0, champion, options, random);
                    if (!sleepLanded) {
                        state.sleepResisted = true;
                    } else if (state.applySleep(position)) {
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
                    state.applyWeaken(position);
                }
            }
            case DOROTHY -> {
                if (hit == champion.hitsPerCycle() - 1
                        && trySkillDebuff(1.0, champion, options, random)) {
                    state.applyWeaken(position);
                }
                applySmite(champion, position, options, state, random);
            }
            case WYSTERI_VINEGUARD -> {
                double weakenChance = options.wysteriWeakenChance(position);
                if (random.nextDouble() < weakenChance
                        && !isResisted(champion, options, random)) {
                    if (!state.weakenLanded) {
                        state.applyWeaken(position);
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
            state.applyOneTurnDebuff(position);
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

    private static double calculateFenaxHitDamage(double baseDamage, Random random) {
        double damageVariation = (1.0 - FENAX_HIT_DAMAGE_VARIATION)
                + random.nextDouble() * 2.0 * FENAX_HIT_DAMAGE_VARIATION;
        double strongAffinityBonus = random.nextDouble() < FENAX_STRONG_AFFINITY_BONUS_CHANCE
                ? 1.0 + FENAX_STRONG_AFFINITY_BONUS
                : 1.0;
        return baseDamage * damageVariation * strongAffinityBonus;
    }

    private static double fenaxDamagePerHit(Champion champion, boolean mithralaBuffsActive) {
        double bookedDamageMultiplier = 1.0 + champion.bookedDamageBonus();
        if (!mithralaBuffsActive || champion.attack() == 0) {
            return champion.damagePerHit() * bookedDamageMultiplier;
        }
        double baseCritMultiplier = 1.0 + champion.critDamage();
        double buffedCritMultiplier =
                1.0 + champion.critDamage() + MIKAGE_INCREASE_CRIT_DAMAGE;
        return champion.damagePerHit()
                * (1.0 + MIKAGE_INCREASE_ATTACK)
                * buffedCritMultiplier / baseCritMultiplier
                * bookedDamageMultiplier;
    }

    private static boolean isMikageAllyAttacker(Team team) {
        return team.at(TeamPosition.ALLY_ATTACKER).kind() == ChampionKind.MIKAGE;
    }

    private static double fenaxStrikeMasteryMultiplier(TrialState state) {
        double multiplier = FENAX_MASTERY_MULTIPLIER;
        if (state.bossHpRemaining / BOSS_MAX_HP < SINGLE_OUT_HP_THRESHOLD) {
            multiplier *= SINGLE_OUT_MULTIPLIER;
        }
        return multiplier;
    }

    private static double fenaxGemstoneMultiplier(Champion champion, boolean quickstrikeApplies) {
        return (quickstrikeApplies
                ? Math.pow(QUICKSTRIKE_MULTIPLIER, champion.quickstrikeStones())
                : 1.0)
                * Math.pow(DUNGEON_DELVER_MULTIPLIER, champion.dungeonDelverStones());
    }

    private static double fenaxBaseDamagePerCycle(Champion champion, boolean mikageBuffsActive) {
        double damagePerHit = fenaxDamagePerHit(champion, mikageBuffsActive)
                * Math.pow(DUNGEON_DELVER_MULTIPLIER, champion.dungeonDelverStones())
                * fenaxGearMultiplier(champion)
                * FENAX_MASTERY_MULTIPLIER;
        double firstTurnDamage = champion.hitsPerCycle()
                * Math.pow(QUICKSTRIKE_MULTIPLIER, champion.quickstrikeStones());
        double laterTurnDamage = champion.hitsPerCycle();
        return damagePerHit * (firstTurnDamage + laterTurnDamage);
    }

    private static double fenaxGearMultiplier(Champion champion) {
        return champion.zealSet() ? ZEAL_SET_MULTIPLIER : 1.0;
    }

    private static final class DamageAccumulator {
        private final String championName;
        private final double baseDamagePerCycle;
        private long samples;
        private double average;
        private double sumSquaredDeviations;

        private DamageAccumulator(String championName, double baseDamagePerCycle) {
            this.championName = championName;
            this.baseDamagePerCycle = baseDamagePerCycle;
        }

        private void add(double damage) {
            samples++;
            double delta = damage - average;
            average += delta / samples;
            sumSquaredDeviations += delta * (damage - average);
        }

        private double average() {
            return average;
        }

        private double standardDeviation() {
            return samples == 0 ? 0 : Math.sqrt(sumSquaredDeviations / samples);
        }

        private SimulationResult.FenaxDamageStatistics toStatistics() {
            return new SimulationResult.FenaxDamageStatistics(
                    championName,
                    baseDamagePerCycle,
                    average(),
                    standardDeviation()
            );
        }
    }

    private static final class DamageBandAccumulator {
        private final Map<Integer, Long> bossKillsByTurn = new HashMap<>(Map.of(
                1, 0L,
                3, 0L,
                4, 0L
        ));
        private long trialCount;
        private long affinityFailedTrials;
        private long damageFailedTrials;
        private long turnOneWinDistanceSamples;
        private double totalTurnOneWinDistancePercent;
        private double worstTurnThreeMissPercent;
        private int worstTurnThreeDebuffsLanded;
        private int worstTurnThreeActiveDebuffs;
        private double worstTurnFourMissPercent;
        private int worstTurnFourDebuffsLanded;
        private int worstTurnFourActiveDebuffs;
        private final Map<Integer, DebuffCountAccumulator> byDebuffCount = new HashMap<>();

        private void add(TrialState state) {
            trialCount++;
            byDebuffCount.computeIfAbsent(
                    state.debuffsLanded,
                    ignored -> new DebuffCountAccumulator()
            ).add(state);
            if (state.affinityFailed) {
                affinityFailedTrials++;
                return;
            }
            if (state.turnOneDamage > 0) {
                turnOneWinDistanceSamples++;
                totalTurnOneWinDistancePercent += Math.max(
                        0,
                        (BOSS_MAX_HP - state.turnOneDamage) / state.turnOneDamage * 100
                );
            }
            if (state.turnThreeCheckpoint && state.turnThreeRemainingHp > 0
                    && state.turnThreeDamage > 0) {
                double missPercent =
                        state.turnThreeRemainingHp / state.turnThreeDamage * 100;
                if (missPercent > worstTurnThreeMissPercent) {
                    worstTurnThreeMissPercent = missPercent;
                    worstTurnThreeDebuffsLanded = state.debuffsLanded;
                    worstTurnThreeActiveDebuffs = state.turnThreeActiveDebuffs;
                }
            }
            if (state.bossKilled) {
                bossKillsByTurn.merge(state.bossKilledTurn, 1L, Long::sum);
                return;
            }

            damageFailedTrials++;
            if (state.fenaxDamage > 0) {
                double missPercent = state.bossHpRemaining / state.fenaxDamage * 100;
                if (missPercent > worstTurnFourMissPercent) {
                    worstTurnFourMissPercent = missPercent;
                    worstTurnFourDebuffsLanded = state.debuffsLanded;
                    worstTurnFourActiveDebuffs = state.activeDebuffCount();
                }
            }
        }

        private SimulationResult.DamageBandStatistics toStatistics() {
            Map<Integer, SimulationResult.DebuffCountStatistics> breakdown = new HashMap<>();
            for (Map.Entry<Integer, DebuffCountAccumulator> entry : byDebuffCount.entrySet()) {
                breakdown.put(entry.getKey(), entry.getValue().toStatistics());
            }
            return new SimulationResult.DamageBandStatistics(
                    trialCount,
                    affinityFailedTrials,
                    damageFailedTrials,
                    turnOneWinDistanceSamples,
                    turnOneWinDistanceSamples == 0
                            ? 0
                            : totalTurnOneWinDistancePercent / turnOneWinDistanceSamples,
                    bossKillsByTurn,
                    worstTurnThreeMissPercent,
                    worstTurnThreeDebuffsLanded,
                    worstTurnThreeActiveDebuffs,
                    worstTurnFourMissPercent,
                    worstTurnFourDebuffsLanded,
                    worstTurnFourActiveDebuffs,
                    breakdown
            );
        }
    }

    private static final class DebuffCountAccumulator {
        private final Map<Integer, Long> bossKillsByTurn = new HashMap<>(Map.of(
                1, 0L,
                3, 0L,
                4, 0L
        ));
        private long trialCount;
        private long affinityFailedTrials;
        private long damageFailedTrials;
        private long turnOneWinDistanceSamples;
        private double totalTurnOneWinDistancePercent;
        private double worstTurnThreeMissPercent;
        private int worstTurnThreeActiveDebuffs;
        private double worstTurnFourMissPercent;
        private int worstTurnFourActiveDebuffs;

        private void add(TrialState state) {
            trialCount++;
            if (state.affinityFailed) {
                affinityFailedTrials++;
                return;
            }
            if (state.turnOneDamage > 0) {
                turnOneWinDistanceSamples++;
                totalTurnOneWinDistancePercent += Math.max(
                        0,
                        (BOSS_MAX_HP - state.turnOneDamage) / state.turnOneDamage * 100
                );
            }
            if (state.turnThreeCheckpoint && state.turnThreeRemainingHp > 0
                    && state.turnThreeDamage > 0) {
                double missPercent =
                        state.turnThreeRemainingHp / state.turnThreeDamage * 100;
                if (missPercent > worstTurnThreeMissPercent) {
                    worstTurnThreeMissPercent = missPercent;
                    worstTurnThreeActiveDebuffs = state.turnThreeActiveDebuffs;
                }
            }
            if (state.bossKilled) {
                bossKillsByTurn.merge(state.bossKilledTurn, 1L, Long::sum);
                return;
            }

            damageFailedTrials++;
            if (state.fenaxDamage > 0) {
                double missPercent = state.bossHpRemaining / state.fenaxDamage * 100;
                if (missPercent > worstTurnFourMissPercent) {
                    worstTurnFourMissPercent = missPercent;
                    worstTurnFourActiveDebuffs = state.activeDebuffCount();
                }
            }
        }

        private SimulationResult.DebuffCountStatistics toStatistics() {
            return new SimulationResult.DebuffCountStatistics(
                    trialCount,
                    affinityFailedTrials,
                    damageFailedTrials,
                    turnOneWinDistanceSamples,
                    turnOneWinDistanceSamples == 0
                            ? 0
                            : totalTurnOneWinDistancePercent / turnOneWinDistanceSamples,
                    bossKillsByTurn,
                    worstTurnThreeMissPercent,
                    worstTurnThreeActiveDebuffs,
                    worstTurnFourMissPercent,
                    worstTurnFourActiveDebuffs
            );
        }
    }

    private double calculateDebuffDamageMultiplier(TrialState state, GearSetup setup) {
        double feralDamageBonus = 0;
        for (Map.Entry<TeamPosition, Integer> entry : state.debuffsByPosition.entrySet()) {
            if (setup.gearFor(entry.getKey()) == GearSet.FERAL) {
                feralDamageBonus += FERAL_DAMAGE_PER_DEBUFF * entry.getValue();
            }
        }

        double weakenMultiplier = state.weakenLanded
                ? 1.0 + WEAKEN_DAMAGE_BONUS
                : 1.0;
        return damageModel.damageMultiplier(state.debuffsLanded)
                * (1.0 + feralDamageBonus)
                * weakenMultiplier;
    }

    private static final class TrialState {
        private final Map<TeamPosition, Integer> debuffsByPosition = new EnumMap<>(TeamPosition.class);
        private final Map<TeamPosition, Double> fenaxDamageByPosition =
                new EnumMap<>(TeamPosition.class);
        private final List<Integer> activeDebuffTurns = new ArrayList<>();
        private double fenaxDamage;
        private double turnOneDamage;
        private double bossHpRemaining = BOSS_MAX_HP;
        private int debuffsLanded;
        private int currentTurn = 1;
        private int bossKilledTurn;
        private double turnThreeRemainingHp;
        private double turnThreeDamage;
        private int turnThreeActiveDebuffs;
        private boolean sleepLanded;
        private boolean sleepResisted;
        private boolean weakenLanded;
        private boolean smiteLanded;
        private boolean sleepFailed;
        private boolean affinityFailed;
        private boolean bossKilled;
        private boolean turnThreeCheckpoint;

        private void applyDebuff(TeamPosition position) {
            applyDebuff(position, 2);
        }

        private void applyOneTurnDebuff(TeamPosition position) {
            applyDebuff(position, 1);
        }

        private void applyWeaken(TeamPosition position) {
            applyDebuff(position, 2);
            weakenLanded = true;
        }

        private void applyDebuff(TeamPosition position, int turns) {
            debuffsLanded++;
            debuffsByPosition.merge(position, 1, Integer::sum);
            activeDebuffTurns.add(turns);
        }

        private int activeDebuffCount() {
            return activeDebuffTurns.size();
        }

        private void advanceTurn() {
            activeDebuffTurns.replaceAll(turns -> turns - 1);
            activeDebuffTurns.removeIf(turns -> turns <= 0);
        }

        private boolean applySleep(TeamPosition position) {
            if (!sleepLanded) {
                applyOneTurnDebuff(position);
                sleepLanded = true;
                return true;
            }
            return false;
        }
    }
}
