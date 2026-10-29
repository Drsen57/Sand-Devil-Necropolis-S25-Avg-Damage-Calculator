package com.sand_devil_calc;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Main {
    private static final long SIMULATION_COUNT = 1_000_000;
    private static final long RANDOM_SEED = 42;
    private static final double BOSS_RESISTANCE = 470;
    private static final double DEFAULT_CHAMPION_ACCURACY = BOSS_RESISTANCE + 114;
    // private static final long BOSS_MAX_HP = 3_100_000; // Not modeled yet.
    // private static final int BOSS_DEFENSE = 3_587; // Not modeled yet.
    private static final ChampionKind DEBUFFER_KIND = ChampionKind.MITHRALA;
    private static final boolean FAIL_RUN_WITHOUT_SLEEP = true;
    private static final boolean ASSUME_HELLHOUND_SLEEP_LANDS = true;
    private static final Set<TeamPosition> FEARSOME_PRESENCE_POSITIONS = Set.of();
    private static final Set<TeamPosition> SNIPER_MASTERY_POSITIONS = Set.of(
            TeamPosition.SLEEPER,
            TeamPosition.DEBUFFER
    );
    private static final boolean HELLHOUND_HARVEST_DESPAIR_ENABLED = true;
    private static final boolean HELLHOUND_HARVEST_DESPAIR_SNIPER_ENABLED = false;
    private static final boolean MITHRALA_SPOREBURST_ROD_ENABLED = true;
    private static final int MITHRALA_SPOREBURST_ROD_STARS = 4;
    private static final boolean MITHRALA_SMITE_ENABLED = true;
    private static final int MITHRALA_SMITE_STARS = 3;

    private static final Team TEAM = new Team(Map.of(
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
            new Champion(
                    "Fenax",
                    ChampionKind.FENAX,
                    false,
                    1,
                    1.0,
                    DEFAULT_CHAMPION_ACCURACY
            ),
            TeamPosition.DAMAGE_DEALER_TWO,
            new Champion(
                    "Fenax",
                    ChampionKind.FENAX,
                    false,
                    1,
                    1.0,
                    DEFAULT_CHAMPION_ACCURACY
            )
    ));

    private static final List<GearSetup> GEAR_SETUPS = List.of(
            new GearSetup("Hellhound Toxic / Mithrala Toxic", Map.of(
                    TeamPosition.SLEEPER, GearSet.TOXIC,
                    TeamPosition.DEBUFFER, GearSet.TOXIC
            )),
            new GearSetup("Hellhound Toxic / Mithrala Feral", Map.of(
                    TeamPosition.SLEEPER, GearSet.TOXIC,
                    TeamPosition.DEBUFFER, GearSet.FERAL
            ))
    );

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
            MITHRALA_SMITE_ENABLED,
            MITHRALA_SMITE_STARS
    );

    private static final DamageModel BOSS_DAMAGE_MODEL = new DamageModel(0.75, 0.15);

    public static void main(String[] args) {
        List<Champion> enabledChampions = TEAM.inTurnOrder().stream()
                .filter(Champion::enabled)
                .toList();
        Simulator simulator = new Simulator(BOSS_DAMAGE_MODEL);

        System.out.println("Sand Devil Necropolis S25 Team Simulator");
        System.out.printf("Trials per setup: %,d%n", SIMULATION_COUNT);
        System.out.println("Team positions:");
        for (TeamPosition position : TeamPosition.values()) {
            Champion champion = TEAM.at(position);
            System.out.printf(
                    "  %s: %s (%s)%n",
                    position,
                    champion.name(),
                    champion.enabled() ? "enabled" : "disabled"
            );
        }
        System.out.println("Debuffer: " + TEAM.at(TeamPosition.DEBUFFER).name());
        System.out.println("Boss resistance: " + BOSS_RESISTANCE);
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
        System.out.println("Mithrala Smite: "
                + MITHRALA_SMITE_ENABLED + " (" + MITHRALA_SMITE_STARS
                + " stars; applies only when Mithrala is selected)");

        for (GearSetup setup : GEAR_SETUPS) {
            SimulationResult result = simulator.run(
                    SIMULATION_COUNT,
                    TEAM,
                    setup,
                    OPTIONS,
                    new Random(RANDOM_SEED)
            );
            printResult(result);
        }
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
            case ATHEL -> new Champion(
                    "Athel",
                    ChampionKind.ATHEL,
                    true,
                    3,
                    1.0,
                    DEFAULT_CHAMPION_ACCURACY
            );
            default -> throw new IllegalArgumentException(
                    "Debuffer must be Mithrala or Athel: " + DEBUFFER_KIND
            );
        };
    }

    private static void printResult(SimulationResult result) {
        System.out.println("\n" + result.setupName());
        System.out.printf(
                "Sleep failures: %,d / %,d (%.2f%%)%n",
                result.sleepFailedTrials(),
                result.trials(),
                (double) result.sleepFailedTrials() / result.trials() * 100
        );
        System.out.printf(
                "Successful runs: %,d / %,d (%.2f%%)%n",
                result.successfulTrials(),
                result.trials(),
                result.successRate() * 100
        );
        if (result.successfulTrials() == 0) {
            System.out.println("No successful runs; damage and debuff averages are unavailable.");
            return;
        }
        System.out.printf(
                "Average potential damage per successful run: %.4f (%.2f%% of unmitigated baseline)%n",
                result.averageDamagePerCycle(),
                result.averageDamageMultiplier() * 100
        );
        double averageDamageMultiplier = result.averageDamageMultiplier();
        double standardDeviationMultiplier = result.damageStandardDeviationMultiplier();
        System.out.printf(
                "1 standard deviation range (average +/- 1 SD): %.2f%% to %.2f%%%n",
                (averageDamageMultiplier - standardDeviationMultiplier) * 100,
                (averageDamageMultiplier + standardDeviationMultiplier) * 100
        );
        System.out.printf(
                "2 standard deviations range (average +/- 2 SD): %.2f%% to %.2f%%%n",
                (averageDamageMultiplier - 2 * standardDeviationMultiplier) * 100,
                (averageDamageMultiplier + 2 * standardDeviationMultiplier) * 100
        );
        System.out.printf(
                "3 standard deviations range (average +/- 3 SD): %.2f%% to %.2f%%%n",
                (averageDamageMultiplier - 3 * standardDeviationMultiplier) * 100,
                (averageDamageMultiplier + 3 * standardDeviationMultiplier) * 100
        );
        System.out.printf("Average landed debuffs: %.4f%n", result.averageDebuffs());
        System.out.println("Landed-debuff-count probabilities:");

        for (int debuffs = 0; debuffs <= result.maximumDebuffsObserved(); debuffs++) {
            System.out.printf(
                    "  %2d debuffs: %6.2f%%%n",
                    debuffs,
                    result.probabilityForDebuffCount(debuffs) * 100
            );
        }
    }
}
