package com.sand_devil_calc;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Main {
    private static final long SIMULATION_COUNT = 1_000_000;
    private static final long RANDOM_SEED = 42;
    private static final double BOSS_RESISTANCE = 470;
    private static final double BOSS_MAX_HP = 3_082_910;
    private static final double DEFAULT_CHAMPION_ACCURACY = BOSS_RESISTANCE + 114;
    private static final double FENAX_A1_MULTIPLIER = 4.1;
    private static final double FENAX_PLUS_FOUR_ATTACK = 8_974;
    private static final double FENAX_PLUS_ZERO_ATTACK = 7_184;
    private static final double FENAX_BOOKED_DAMAGE_BONUS = 0.20;
    private static final List<FenaxBuildSetup> BUILD_SETUPS = List.of(
            new FenaxBuildSetup(
                    "Restored",
                    new FenaxBuild(375, 2, 1, false),
                    new FenaxBuild(366, 1, 2, true)
            )
    );
    private static final double MIKAGE_INCREASE_ATTACK = 0.50;
    private static final double MIKAGE_INCREASE_CRIT_DAMAGE = 0.30;
    private static final double RUTHLESS_AMBUSH_MULTIPLIER = 1.08;
    private static final double OPPORTUNIST_MULTIPLIER = 1.12;
    private static final double BRING_IT_DOWN_MULTIPLIER = 1.06;
    private static final double SINGLE_OUT_MULTIPLIER = 1.08;
    // private static final int BOSS_DEFENSE = 3_587; // Not modeled yet.
    // Athel is excluded from selectable configurations because weak affinity makes her unreliable.
    private static final ChampionKind DEBUFFER_KIND = ChampionKind.MITHRALA;
    private static final boolean FAIL_RUN_WITHOUT_SLEEP = true;
    private static final boolean ASSUME_HELLHOUND_SLEEP_LANDS = false;
    private static final Set<TeamPosition> FEARSOME_PRESENCE_POSITIONS = Set.of();
    private static final Set<TeamPosition> SNIPER_MASTERY_POSITIONS = Set.of(
            TeamPosition.SLEEPER,
            TeamPosition.DEBUFFER
    );
    private static final boolean HELLHOUND_HARVEST_DESPAIR_ENABLED = true;
    private static final boolean HELLHOUND_HARVEST_DESPAIR_SNIPER_ENABLED = false;
    private static final boolean MITHRALA_SPOREBURST_ROD_ENABLED =
            DEBUFFER_KIND == ChampionKind.MITHRALA;
    private static final int MITHRALA_SPOREBURST_ROD_STARS = 4;
    private static final boolean DEBUFFER_SMITE_ENABLED =
            DEBUFFER_KIND == ChampionKind.MITHRALA || DEBUFFER_KIND == ChampionKind.DOROTHY;
    private static final int DEBUFFER_SMITE_STARS = DEBUFFER_KIND == ChampionKind.DOROTHY ? 5 : 3;
    private static final int WYSTERI_WEAKEN_RUNES =
            DEBUFFER_KIND == ChampionKind.WYSTERI_VINEGUARD ? 1 : 0;

    private static final SimulationOptions OPTIONS = new SimulationOptions(
            FAIL_RUN_WITHOUT_SLEEP,
            ASSUME_HELLHOUND_SLEEP_LANDS,
            BOSS_RESISTANCE,
            FEARSOME_PRESENCE_POSITIONS,
            SNIPER_MASTERY_POSITIONS,
            HELLHOUND_HARVEST_DESPAIR_ENABLED,
            HELLHOUND_HARVEST_DESPAIR_SNIPER_ENABLED,
            MITHRALA_SPOREBURST_ROD_ENABLED,
            MITHRALA_SPOREBURST_ROD_STARS,
            DEBUFFER_SMITE_ENABLED,
            DEBUFFER_SMITE_STARS,
            WYSTERI_WEAKEN_RUNES
    );

    private static final DamageModel BOSS_DAMAGE_MODEL = new DamageModel(0.75, 0.15);

    public static void main(String[] args) {
        Team sampleTeam = createTeam(BUILD_SETUPS.get(0));
        List<Champion> enabledChampions = sampleTeam.inTurnOrder().stream()
                .filter(Champion::enabled)
                .toList();
        Simulator simulator = new Simulator(BOSS_DAMAGE_MODEL);

        System.out.println("Sand Devil Necropolis S25 Team Simulator");
        System.out.printf("Trials per setup: %,d%n", SIMULATION_COUNT);
        System.out.println(
                "IMPORTANT: Damage statistics include the full killing-hit damage, including "
                        + "overkill; the run still ends as soon as the boss is killed."
        );
        System.out.println("Team positions:");
        for (TeamPosition position : TeamPosition.values()) {
            Champion champion = sampleTeam.at(position);
            System.out.printf(
                    "  %s: %s (%s)%n",
                    position,
                    champion.name(),
                    position == TeamPosition.ALLY_ATTACKER
                            && champion.kind() == ChampionKind.MIKAGE
                            ? "buffs modeled; ally attack omitted"
                            : champion.enabled() ? "enabled" : "disabled"
            );
        }
        System.out.println("Debuffer: " + sampleTeam.at(TeamPosition.DEBUFFER).name());
        System.out.println("Boss resistance: " + BOSS_RESISTANCE);
        System.out.printf("Boss maximum HP: %s%n", formatFenaxDamage(BOSS_MAX_HP));
        System.out.printf(
                "Mikage ally-attack buffs: +%.0f%% Attack and +%.0f percentage points Crit Damage%n",
                MIKAGE_INCREASE_ATTACK * 100,
                MIKAGE_INCREASE_CRIT_DAMAGE * 100
        );
        System.out.println(
                "Turn 1: Mikage places Increase ATK and Increase Crit Damage on the team before "
                        + "the Fenaxes' combined ally attack; both Fenaxes' hits use these buffs."
        );
        System.out.println(
                "Fenax damage variation: +/-10% per hit; strong-affinity bonus: "
                        + "30% chance for +30% damage per hit"
        );
        System.out.println(
                "Fenax mastery multipliers: Ruthless Ambush +8%, Opportunist +12%, "
                        + "Bring It Down +6%; Single Out +8% below 40% boss HP; "
                        + "applied per strike"
        );
        System.out.println(
                "Quickstrike: +10% per gemstone on turn 1 only, for both hits of each Fenax; "
                        + "Dungeon Delver: +10% per gemstone multiplicatively on every hit"
        );
        System.out.println("Fenax books: +20% multiplicative damage for both Fenaxes.");
        System.out.println("Default champion accuracy: " + DEFAULT_CHAMPION_ACCURACY);
        System.out.println("Champion accuracy: " + enabledChampions.stream()
                .map(champion -> champion.name() + "=" + champion.accuracy())
                .toList());
        System.out.println("Fail run without sleep: " + FAIL_RUN_WITHOUT_SLEEP);
        System.out.println("Assume Hellhound sleep lands: " + ASSUME_HELLHOUND_SLEEP_LANDS);
        System.out.println("Fearsome Presence positions: " + FEARSOME_PRESENCE_POSITIONS);
        System.out.println("Toxic Sniper mastery positions: " + SNIPER_MASTERY_POSITIONS);
        System.out.println("Hellhound Harvest Despair: "
                + HELLHOUND_HARVEST_DESPAIR_ENABLED
                + " (" + (HELLHOUND_HARVEST_DESPAIR_SNIPER_ENABLED
                ? "65% with Sniper" : "60% base") + ")");
        System.out.println("Mithrala Sporeburst Rod: "
                + MITHRALA_SPOREBURST_ROD_ENABLED
                + " (" + MITHRALA_SPOREBURST_ROD_STARS + " stars)");
        System.out.println("Debuffer Smite: "
                + DEBUFFER_SMITE_ENABLED + " (" + DEBUFFER_SMITE_STARS
                + " stars; applies only to eligible debuffers)");
        System.out.printf(
                "Each build uses seed %d independently; damage-band results use actual trials "
                        + "within cumulative +/-1, +/-2, and +/-3 standard deviations.%n",
                RANDOM_SEED
        );
        for (FenaxBuildSetup build : BUILD_SETUPS) {
            Team team = createTeam(build);
            printBuildSetup(build);
            GearSetup setup = new GearSetup(
                    build.name() + " / Hellhound Toxic / "
                            + team.at(TeamPosition.DEBUFFER).name() + " Toxic",
                    Map.of(
                            TeamPosition.SLEEPER, GearSet.TOXIC,
                            TeamPosition.DEBUFFER, GearSet.TOXIC
                    )
            );
            SimulationResult result = simulator.runWithDamageBands(
                    SIMULATION_COUNT,
                    team,
                    setup,
                    OPTIONS,
                    RANDOM_SEED
            );
            SimulationResult cappedDamageResult = new Simulator(BOSS_DAMAGE_MODEL, false)
                    .runWithDamageBands(
                            SIMULATION_COUNT,
                            team,
                            setup,
                            OPTIONS,
                            RANDOM_SEED
                    );
            printResult(result, cappedDamageResult);
        }
    }

    private static Team createTeam(FenaxBuildSetup build) {
        return new Team(Map.of(
                TeamPosition.ALLY_ATTACKER,
                new Champion(
                        "Mikage",
                        ChampionKind.MIKAGE,
                        false,
                        1,
                        1.0,
                        DEFAULT_CHAMPION_ACCURACY
                ),
                TeamPosition.SLEEPER,
                new Champion(
                        "Hellhound",
                        ChampionKind.HELLHOUND,
                        true,
                        1,
                        1.0,
                        DEFAULT_CHAMPION_ACCURACY
                ),
                TeamPosition.DEBUFFER,
                createDebuffer(),
                TeamPosition.DAMAGE_DEALER_ONE,
                createFenax("Fenax+4", FENAX_PLUS_FOUR_ATTACK, build.plusFour()),
                TeamPosition.DAMAGE_DEALER_TWO,
                createFenax("Fenax+0", FENAX_PLUS_ZERO_ATTACK, build.plusZero())
        ));
    }

    private static Champion createFenax(String name, double attack, FenaxBuild build) {
        double critDamage = build.critDamagePercent() / 100;
        double baseDamagePerHit = FENAX_A1_MULTIPLIER * attack * (1.0 + critDamage);
        return new Champion(
                name,
                ChampionKind.FENAX,
                true,
                2,
                baseDamagePerHit,
                DEFAULT_CHAMPION_ACCURACY,
                attack,
                critDamage,
                build.quickstrikeStones(),
                build.dungeonDelverStones(),
                build.zealSet(),
                FENAX_BOOKED_DAMAGE_BONUS
        );
    }

    private static void printBuildSetup(FenaxBuildSetup build) {
        System.out.println("\n" + build.name() + " Fenax build");
        printFenaxBuild("Fenax+4", FENAX_PLUS_FOUR_ATTACK, build.plusFour());
        printFenaxBuild("Fenax+0", FENAX_PLUS_ZERO_ATTACK, build.plusZero());
    }

    private static void printFenaxBuild(String name, double attack, FenaxBuild build) {
        double critDamage = build.critDamagePercent() / 100;
        System.out.printf(
                "%s: %.0f ATK; %.0f%% Crit Damage; %d Quickstrike, %d Dungeon Delver; Zeal=%s%n",
                name,
                attack,
                build.critDamagePercent(),
                build.quickstrikeStones(),
                build.dungeonDelverStones(),
                build.zealSet()
        );
        System.out.printf(
                "%s Mikage-buffed A1 before gemstones/masteries: %.1f x (%.0f ATK x %.2f) x "
                        + "(1 + %.0f%% + %.0f%% Crit Damage) = %s per hit%n",
                name,
                FENAX_A1_MULTIPLIER,
                attack,
                1.0 + MIKAGE_INCREASE_ATTACK,
                build.critDamagePercent(),
                MIKAGE_INCREASE_CRIT_DAMAGE * 100,
                formatFenaxDamage(calculateBuffedFenaxDamagePerHit(attack, critDamage))
        );
    }

    private record FenaxBuild(
            double critDamagePercent,
            int quickstrikeStones,
            int dungeonDelverStones,
            boolean zealSet
    ) {
    }

    private record FenaxBuildSetup(
            String name,
            FenaxBuild plusFour,
            FenaxBuild plusZero
    ) {
    }

    private static Champion createDebuffer() {
        return switch (DEBUFFER_KIND) {
            case MITHRALA -> new Champion(
                    "Mithrala",
                    ChampionKind.MITHRALA,
                    true,
                    2,
                    1.0,
                    DEFAULT_CHAMPION_ACCURACY
            );
            case ATHEL -> throw new IllegalArgumentException(
                    "Athel is excluded from selectable configurations due to weak affinity."
            );
            case DOROTHY -> new Champion(
                    "Dorothy",
                    ChampionKind.DOROTHY,
                    true,
                    1,
                    1.0,
                    DEFAULT_CHAMPION_ACCURACY
            );
            case WYSTERI_VINEGUARD -> new Champion(
                    "Wysteri Vineguard",
                    ChampionKind.WYSTERI_VINEGUARD,
                    true,
                    2,
                    1.0,
                    DEFAULT_CHAMPION_ACCURACY
            );
            default -> throw new IllegalArgumentException(
                    "Debuffer must be Mithrala, Dorothy, or Wysteri Vineguard: "
                            + DEBUFFER_KIND
            );
        };
    }

    private static double calculateBuffedFenaxDamagePerHit(double attack, double critDamage) {
        return FENAX_A1_MULTIPLIER
                * attack * (1.0 + MIKAGE_INCREASE_ATTACK)
                * (1.0 + critDamage + MIKAGE_INCREASE_CRIT_DAMAGE);
    }

    private static void printResult(
            SimulationResult result,
            SimulationResult cappedDamageResult
    ) {
        System.out.println("\n" + result.setupName());
        System.out.println(
                "Damage figures below include overkill from the killing hit; the boss's HP "
                        + "is still treated as zero after death."
        );
        System.out.printf(
                "Sleep failures: %,d / %,d (%.2f%%)%n",
                result.sleepFailedTrials(),
                result.trials(),
                (double) result.sleepFailedTrials() / result.trials() * 100
        );
        System.out.printf(
                "Losses from sleep resist: %,d / %,d (%.2f%%)%n",
                result.sleepResistFailedTrials(),
                result.trials(),
                (double) result.sleepResistFailedTrials() / result.trials() * 100
        );
        System.out.printf(
                "Runs passing the sleep gate: %,d / %,d%n",
                result.successfulTrials(),
                result.trials()
        );
        System.out.printf(
                "Wins by turn: turn 1: %,d; turn 3: %,d; turn 4: %,d%n",
                result.bossKillsByTurn().getOrDefault(1, 0L),
                result.bossKillsByTurn().getOrDefault(3, 0L),
                result.bossKillsByTurn().getOrDefault(4, 0L)
        );
        System.out.println("Turn 2 is the boss waking from Sleep; it cannot be a win turn.");
        System.out.printf(
                "Runs lost to insufficient Fenax damage: %,d%n",
                result.damageFailedTrials()
        );
        if (result.survivedTurnThreeTrials() > 0) {
            System.out.printf(
                    "Worst turn 3 shortfall: %.4f%% of damage dealt through turn 3%n",
                    result.worstTurnThreeMissPercent()
            );
            System.out.printf(
                    "  Debuffs landed in that run: %d total; %d active at turn 3%n",
                    result.worstTurnThreeDebuffsLanded(),
                    result.worstTurnThreeActiveDebuffs()
            );
        } else {
            System.out.println("Worst turn 3 shortfall: none; no eligible runs survived turn 3.");
        }
        if (result.damageFailedTrials() > 0) {
            System.out.printf(
                    "Worst turn 4 loss shortfall: %.4f%% of damage dealt through turn 4%n",
                    result.worstTurnFourMissPercent()
            );
            System.out.printf(
                    "  Debuffs landed in that loss: %d total; %d active at turn 4%n",
                    result.worstTurnFourDebuffsLanded(),
                    result.worstTurnFourActiveDebuffs()
            );
        } else {
            System.out.println("Worst turn 4 loss shortfall: none; no eligible runs lost to damage.");
        }
        printDamageBandResults(result, cappedDamageResult);
        if (result.damageSampleTrials() == 0) {
            System.out.println("No runs passed the sleep gate; Fenax damage is unavailable.");
            return;
        }
        System.out.println("Fenax average damage: "
                + formatFenaxDamage(result.averageFenaxDamagePerCycle()));
        System.out.println("Fenax damage standard-deviation chart (damage only):");
        System.out.printf(
                "%-32s %14s %14s %26s %26s %26s%n",
                "Fenax",
                "Average",
                "Standard dev.",
                "Average +/- 1 SD",
                "Average +/- 2 SD",
                "Average +/- 3 SD"
        );
        printFenaxDamageChartRow(
                "Combined Fenax",
                result.averageFenaxDamagePerCycle(),
                result.fenaxDamageStandardDeviation()
        );
        for (TeamPosition position : List.of(
                TeamPosition.DAMAGE_DEALER_ONE,
                TeamPosition.DAMAGE_DEALER_TWO
        )) {
            SimulationResult.FenaxDamageStatistics statistics =
                    result.fenaxDamageByPosition().get(position);
            if (statistics != null) {
                printFenaxDamageChartRow(
                        statistics.championName() + " (" + position + ")",
                        statistics.averageDamagePerCycle(),
                        statistics.standardDeviation()
                );
            }
        }
    }

    private static void printDamageBandResults(
            SimulationResult result,
            SimulationResult cappedDamageResult
    ) {
        System.out.println("Representative trials:");
        SimulationResult.RepresentativeDamageTrials representatives =
                result.representativeDamageTrials();
        if (representatives == null) {
            System.out.println("  Representative trial details are unavailable.");
        } else {
            printRepresentativeDamageTrial(
                    "Median trial (lower middle observed damage)",
                    representatives.medianTrial(),
                    result.averageFenaxDamagePerCycle()
            );
            printRepresentativeDamageTrial(
                    "Mean trial (observed trial closest to mean)",
                    representatives.meanTrial(),
                    result.averageFenaxDamagePerCycle()
            );
        }

        printCappedDamageComparison(cappedDamageResult);
        System.out.println("Outcomes among actual trials in each cumulative damage band:");
        printDamageBandSummaryTable(result);
        for (int standardDeviations = 1; standardDeviations <= 3; standardDeviations++) {
            SimulationResult.DamageBandStatistics statistics =
                    result.damageBandStatistics().get(standardDeviations);
            if (statistics == null) {
                continue;
            }
            double bandWidth = standardDeviations * result.fenaxDamageStandardDeviation();
            printDamageBand(
                    standardDeviations == 0
                            ? "0 SD (exact mean)"
                            : String.format(Locale.ROOT, "+/- %d SD", standardDeviations),
                    result.averageFenaxDamagePerCycle() - bandWidth,
                    result.averageFenaxDamagePerCycle() + bandWidth,
                    statistics
            );
        }
        SimulationResult.DamageBandStatistics allStatistics =
                result.damageBandStatistics().get(-1);
        if (allStatistics != null) {
            printDamageBand("All damage trials", Double.NaN, Double.NaN, allStatistics);
        }
    }

    private static void printCappedDamageComparison(SimulationResult result) {
        System.out.println("Comparison without overkill (killing hit capped at remaining HP):");
        System.out.printf(
                "%-32s %14s %14s %26s %26s %26s%n",
                "Fenax",
                "Average",
                "Standard dev.",
                "Average +/- 1 SD",
                "Average +/- 2 SD",
                "Average +/- 3 SD"
        );
        printFenaxDamageChartRow(
                "Combined Fenax",
                result.averageFenaxDamagePerCycle(),
                result.fenaxDamageStandardDeviation()
        );
        for (TeamPosition position : List.of(
                TeamPosition.DAMAGE_DEALER_ONE,
                TeamPosition.DAMAGE_DEALER_TWO
        )) {
            SimulationResult.FenaxDamageStatistics statistics =
                    result.fenaxDamageByPosition().get(position);
            if (statistics != null) {
                printFenaxDamageChartRow(
                        statistics.championName() + " (" + position + ")",
                        statistics.averageDamagePerCycle(),
                        statistics.standardDeviation()
                );
            }
        }
        SimulationResult.RepresentativeDamageTrials representatives =
                result.representativeDamageTrials();
        if (representatives != null) {
            printCompactDamageTrial("Capped median trial", representatives.medianTrial());
            printCompactDamageTrial("Capped mean-nearest trial", representatives.meanTrial());
        }
    }

    private static void printCompactDamageTrial(
            String label,
            SimulationResult.DamageTrialExample example
    ) {
        if (example == null) {
            return;
        }
        System.out.printf(
                Locale.ROOT,
                "  %s #%,d: %s total damage; %s turn-one damage; %d debuffs; %s%n",
                label,
                example.trialNumber(),
                formatDamagePoints(example.totalFenaxDamage()),
                formatDamagePoints(example.turnOneDamage()),
                example.debuffsLanded(),
                example.bossKilledTurn() > 0
                        ? "killed on turn " + example.bossKilledTurn()
                        : "boss survived"
        );
    }

    private static void printDamageBandSummaryTable(SimulationResult result) {
        System.out.printf(
                "%-12s | %-23s | %12s | %-25s | %12s%n",
                "Band",
                "Damage range",
                "Trials",
                "Wins T1 / T3 / T4",
                "Damage losses"
        );
        for (int standardDeviations = 1; standardDeviations <= 3; standardDeviations++) {
            SimulationResult.DamageBandStatistics statistics =
                    result.damageBandStatistics().get(standardDeviations);
            if (statistics != null) {
                double bandWidth =
                        standardDeviations * result.fenaxDamageStandardDeviation();
                printDamageBandSummaryRow(
                        String.format(Locale.ROOT, "+/- %d SD", standardDeviations),
                        result.averageFenaxDamagePerCycle() - bandWidth,
                        result.averageFenaxDamagePerCycle() + bandWidth,
                        statistics
                );
            }
        }
        SimulationResult.DamageBandStatistics allStatistics =
                result.damageBandStatistics().get(-1);
        if (allStatistics != null) {
            printDamageBandSummaryRow("All trials", Double.NaN, Double.NaN, allStatistics);
        }
    }

    private static void printDamageBandSummaryRow(
            String label,
            double minimumDamage,
            double maximumDamage,
            SimulationResult.DamageBandStatistics statistics
    ) {
        String damageRange = Double.isFinite(minimumDamage)
                ? formatFenaxDamage(minimumDamage) + " to " + formatFenaxDamage(maximumDamage)
                : "All";
        System.out.printf(
                Locale.ROOT,
                "%-12s | %-23s | %,12d | %,7d / %,7d / %,7d | %,12d%n",
                label,
                damageRange,
                statistics.trialCount(),
                statistics.bossKillsByTurn().getOrDefault(1, 0L),
                statistics.bossKillsByTurn().getOrDefault(3, 0L),
                statistics.bossKillsByTurn().getOrDefault(4, 0L),
                statistics.damageFailedTrials()
        );
    }

    private static void printRepresentativeDamageTrial(
            String label,
            SimulationResult.DamageTrialExample example,
            double meanDamage
    ) {
        if (example == null) {
            System.out.println("  " + label + ": unavailable; no trials passed the sleep gate.");
            return;
        }
        System.out.printf(
                Locale.ROOT,
                "  %s (trial %,d): total damage %s (mean %s); turn-1 damage %s; "
                        + "%d debuffs landed%n",
                label,
                example.trialNumber(),
                formatDamagePoints(example.totalFenaxDamage()),
                formatDamagePoints(meanDamage),
                formatDamagePoints(example.turnOneDamage()),
                example.debuffsLanded()
        );
        if (example.affinityFailed()) {
            System.out.println("    Outcome: affinity failure.");
        } else if (example.bossKilledTurn() > 0) {
            System.out.printf("    Outcome: boss killed on turn %d.%n", example.bossKilledTurn());
        } else {
            System.out.printf(
                    "    Outcome: damage loss; boss had %s HP remaining.%n",
                    formatDamagePoints(example.remainingBossHp())
            );
        }
        if (example.turnThreeDamage() > 0 || example.turnThreeRemainingHp() > 0) {
            System.out.printf(
                    "    Turn-3 checkpoint: %s damage; %s HP remaining; %d active debuffs.%n",
                    formatDamagePoints(example.turnThreeDamage()),
                    formatDamagePoints(example.turnThreeRemainingHp()),
                    example.turnThreeActiveDebuffs()
            );
        }
        System.out.printf("    Active debuffs at the end: %d%n", example.finalActiveDebuffs());
    }

    private static String formatDamagePoints(double damage) {
        return String.format(Locale.ROOT, "%,.0f", damage);
    }

    private static void printDamageBand(
            String label,
            double minimumDamage,
            double maximumDamage,
            SimulationResult.DamageBandStatistics statistics
    ) {
        if (Double.isFinite(minimumDamage) && Double.isFinite(maximumDamage)) {
            System.out.printf(
                    Locale.ROOT,
                    "  %s (%s to %s; %,d trials): affinity failures %,d; "
                            + "wins T1/T3/T4 %,d/%,d/%,d; insufficient-damage losses %,d%n",
                    label,
                    formatFenaxDamage(minimumDamage),
                    formatFenaxDamage(maximumDamage),
                    statistics.trialCount(),
                    statistics.affinityFailedTrials(),
                    statistics.bossKillsByTurn().getOrDefault(1, 0L),
                    statistics.bossKillsByTurn().getOrDefault(3, 0L),
                    statistics.bossKillsByTurn().getOrDefault(4, 0L),
                    statistics.damageFailedTrials()
            );
        } else {
            System.out.printf(
                    "  %s (%,d trials): affinity failures %,d; wins T1/T3/T4 %,d/%,d/%,d; "
                            + "insufficient-damage losses %,d%n",
                    label,
                    statistics.trialCount(),
                    statistics.affinityFailedTrials(),
                    statistics.bossKillsByTurn().getOrDefault(1, 0L),
                    statistics.bossKillsByTurn().getOrDefault(3, 0L),
                    statistics.bossKillsByTurn().getOrDefault(4, 0L),
                    statistics.damageFailedTrials()
            );
        }
        if (statistics.turnOneWinDistanceSamples() > 0) {
            System.out.printf(
                    Locale.ROOT,
                    "    Average extra turn-1 damage needed for a win: %.4f%% of turn-1 "
                            + "damage dealt%n",
                    statistics.averageTurnOneWinDistancePercent()
            );
        } else {
            System.out.println("    Turn-1 win distance: unavailable; no damage samples.");
        }
        long turnThreeCheckpointTrials =
                statistics.bossKillsByTurn().getOrDefault(3, 0L)
                        + statistics.bossKillsByTurn().getOrDefault(4, 0L)
                        + statistics.damageFailedTrials();
        if (turnThreeCheckpointTrials > 0) {
            System.out.printf(
                    Locale.ROOT,
                    "    Worst turn 3 shortfall: %.4f%% of damage dealt through turn 3%n",
                    statistics.worstTurnThreeMissPercent()
            );
            System.out.printf(
                    "      Debuffs landed in that run: %d total; %d active at turn 3%n",
                    statistics.worstTurnThreeDebuffsLanded(),
                    statistics.worstTurnThreeActiveDebuffs()
            );
        } else {
            System.out.println("    Worst turn 3 shortfall: none.");
        }
        if (statistics.damageFailedTrials() > 0) {
            System.out.printf(
                    Locale.ROOT,
                    "    Worst turn 4 loss shortfall: %.4f%% of damage dealt through turn 4%n",
                    statistics.worstTurnFourMissPercent()
            );
            System.out.printf(
                    "      Debuffs landed in that loss: %d total; %d active at turn 4%n",
                    statistics.worstTurnFourDebuffsLanded(),
                    statistics.worstTurnFourActiveDebuffs()
            );
        } else {
            System.out.println("    Worst turn 4 loss shortfall: none.");
        }
        System.out.println("    Breakdown by debuffs landed (ascending):");
        statistics.byDebuffCount().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> printDebuffBandRow(entry.getKey(), entry.getValue()));
    }

    private static void printDebuffBandRow(
            int debuffsLanded,
            SimulationResult.DebuffCountStatistics statistics
    ) {
        System.out.printf(
                Locale.ROOT,
                "      %d debuffs: %,d trials; wins T1/T3/T4 %,d/%,d/%,d; "
                        + "damage losses %,d%n",
                debuffsLanded,
                statistics.trialCount(),
                statistics.bossKillsByTurn().getOrDefault(1, 0L),
                statistics.bossKillsByTurn().getOrDefault(3, 0L),
                statistics.bossKillsByTurn().getOrDefault(4, 0L),
                statistics.damageFailedTrials()
        );
        if (statistics.turnOneWinDistanceSamples() > 0) {
            System.out.printf(
                    Locale.ROOT,
                    "        Average turn-1 win gap: %.4f%%%n",
                    statistics.averageTurnOneWinDistancePercent()
            );
        }
        if (statistics.worstTurnThreeMissPercent() > 0) {
            System.out.printf(
                    Locale.ROOT,
                    "        Worst turn-3 shortfall: %.4f%% (%d active debuffs)%n",
                    statistics.worstTurnThreeMissPercent(),
                    statistics.worstTurnThreeActiveDebuffs()
            );
        }
        if (statistics.worstTurnFourMissPercent() > 0) {
            System.out.printf(
                    Locale.ROOT,
                    "        Worst turn-4 loss shortfall: %.4f%% (%d active debuffs)%n",
                    statistics.worstTurnFourMissPercent(),
                    statistics.worstTurnFourActiveDebuffs()
            );
        }
    }

    private static void printFenaxDamageChartRow(
            String label,
            double average,
            double standardDeviation
    ) {
        System.out.printf(
                Locale.ROOT,
                "%-32s %14s %14s %26s %26s %26s%n",
                label,
                formatFenaxDamage(average),
                formatFenaxDamage(standardDeviation),
                formatFenaxDamage(average - standardDeviation)
                        + " to " + formatFenaxDamage(average + standardDeviation),
                formatFenaxDamage(average - 2 * standardDeviation)
                        + " to " + formatFenaxDamage(average + 2 * standardDeviation),
                formatFenaxDamage(average - 3 * standardDeviation)
                        + " to " + formatFenaxDamage(average + 3 * standardDeviation)
        );
    }

    private static String formatFenaxDamage(double damage) {
        double absoluteDamage = Math.abs(damage);
        boolean millions = absoluteDamage > 1_000_000;
        double scaledDamage = damage / (millions ? 1_000_000 : 1_000);
        int significantFigures = millions ? 4 : 3;
        int decimalPlaces = absoluteDamage == 0
                ? 0
                : Math.max(
                        0,
                        significantFigures - 1
                                - (int) Math.floor(Math.log10(Math.abs(scaledDamage)))
                );
        return String.format(
                Locale.ROOT,
                "%,." + decimalPlaces + "f%s",
                scaledDamage,
                millions ? "M" : "K"
        );
    }
}
