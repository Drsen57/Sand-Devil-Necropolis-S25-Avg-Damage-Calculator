package com.sand_devil_calc;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class Main {
    private static final long SIMULATION_COUNT = 1_000_000;
    private static final long RANDOM_SEED = 42;

    private static final List<Champion> CHAMPIONS = List.of(
            // Champion settings: name, enabled, hits, damage per hit, starting
            // debuffs, primary chance, bonus debuffs, bonus chance, one-time
            // proc chance, one-time landing chance, Toxic chance, Feral bonus,
            // whether Toxic debuffs count toward the first reduction factor.
            // Set enabled to false to remove a champion from every setup.
            // Replace 1.0 with the champion's expected damage per hit to estimate
            // damage in game; leaving it at 1.0 compares normalized damage.
            new Champion(
                    "Hellhound",
                    true,
                    1,
                    1.0,
                    1,
                    0.60,
                    0,
                    0.0,
                    0.0,
                    0.0,
                    0.80,
                    0.05,
                    true
            ),
            new Champion(
                    "Mithrala",
                    true,
                    2,
                    1.0,
                    0,
                    0.97,
                    1,
                    0.25,
                    0.30,
                    0.97,
                    0.80,
                    0.05,
                    false
            )
    );

    private static final List<GearSetup> GEAR_SETUPS = List.of(
            new GearSetup("Hellhound Toxic / Mithrala Toxic", Map.of(
                    "Hellhound", GearSet.TOXIC,
                    "Mithrala", GearSet.TOXIC
            )),
            new GearSetup("Hellhound Toxic / Mithrala Feral", Map.of(
                    "Hellhound", GearSet.TOXIC,
                    "Mithrala", GearSet.FERAL
            ))
    );

    private static final DamageModel BOSS_DAMAGE_MODEL = new DamageModel(0.15);

    public static void main(String[] args) {
        List<Champion> enabledChampions = CHAMPIONS.stream()
                .filter(Champion::enabled)
                .toList();
        Simulator simulator = new Simulator(BOSS_DAMAGE_MODEL);

        System.out.println("Sand Devil Necropolis S25 Damage Calculator");
        System.out.printf("Trials per setup: %,d%n", SIMULATION_COUNT);
        System.out.println("Enabled champions: " + enabledChampions.stream()
                .map(Champion::name)
                .toList());

        for (GearSetup setup : GEAR_SETUPS) {
            SimulationResult result = simulator.run(
                    SIMULATION_COUNT,
                    enabledChampions,
                    setup,
                    new Random(RANDOM_SEED)
            );
            printResult(result);
        }
    }

    private static void printResult(SimulationResult result) {
        System.out.println("\n" + result.setupName());
        System.out.printf(
                "Average damage per cycle: %.4f (%.2f%% of unmitigated baseline)%n",
                result.averageDamagePerCycle(),
                result.averageDamageMultiplier() * 100
        );
        System.out.printf("Average debuffs: %.4f%n", result.averageDebuffs());
        System.out.println("Debuff-count probabilities:");

        for (int debuffs = 0; debuffs <= result.maximumDebuffsObserved(); debuffs++) {
            System.out.printf(
                    "  %2d debuffs: %6.2f%%%n",
                    debuffs,
                    result.probabilityForDebuffCount(debuffs) * 100
            );
        }
    }
}
