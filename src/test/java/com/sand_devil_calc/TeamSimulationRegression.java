package com.sand_devil_calc;

import java.util.Map;
import java.util.Random;
import java.util.Set;

public class TeamSimulationRegression {
    public static void main(String[] args) {
        verifyDamageReductionClamp();
        verifyAccuracyResistanceFormula();
        verifyGearSetExclusivity();
        verifyRelicRequiresPoisoner();
        verifySleepFailureAndLegacyGateOption();
        verifyDazeAndToxicMasteries();
        verifySleepSingletonAndSuccessfulRunAverages();
        verifySporeburstRodAndSmite();
        verifyAthelWeaken();
        verifyAthelWeakAffinityLossesAreZeroDamageSamples();
        verifyDorothyWeakenSmiteAndToxic();
        verifyWysteriRunesSniperWeakenAndToxic();
        System.out.println("Team simulation checks passed.");
    }

    private static void verifyDamageReductionClamp() {
        DamageModel model = new DamageModel(0.75, 0.15);
        assertClose(0.25, model.damageMultiplier(0), "damage with no debuffs");
        assertClose(1.0, model.damageMultiplier(7), "damage after seven debuffs");
        assertClose(1.0, model.damageMultiplier(10), "damage clamp above seven debuffs");
    }

    private static void verifyAccuracyResistanceFormula() {
        assertClose(
                0.030048,
                DebuffResistance.failureProbability(584, 470),
                "resist chance with 114 more accuracy against 470 resistance"
        );
        assertClose(
                0.0399584551983348,
                DebuffResistance.failureProbability(25, 0),
                "resist chance with 25 more accuracy"
        );
    }

    private static void verifyGearSetExclusivity() {
        new GearSetup("Daze", Map.of(TeamPosition.SLEEPER, GearSet.DAZE));
        new GearSetup("Toxic", Map.of(TeamPosition.DEBUFFER, GearSet.TOXIC));
        new GearSetup("Feral", Map.of(TeamPosition.DEBUFFER, GearSet.FERAL));
        new GearSetup(
                "Toxic and Feral on different champions",
                Map.of(
                        TeamPosition.SLEEPER, GearSet.TOXIC,
                        TeamPosition.DEBUFFER, GearSet.FERAL
                )
        );

        new GearSetup("Hellhound Toxic", Map.of(TeamPosition.SLEEPER, GearSet.TOXIC));
        new GearSetup("Fenax Feral", Map.of(TeamPosition.DAMAGE_DEALER_ONE, GearSet.FERAL));
    }

    private static void verifyRelicRequiresPoisoner() {
        Team athelTeam = team(
                new Champion("Hellhound", ChampionKind.HELLHOUND, true, 1, 1, 584),
                new Champion("Athel", ChampionKind.ATHEL, true, 3, 1, 584)
        );
        SimulationOptions rodEnabled = options(
                true, 470, false, false, true, 3, false, 3
        );
        expectIllegalArgument(
                () -> new Simulator(new DamageModel(0.75, 0.15)).run(
                        1,
                        athelTeam,
                        new GearSetup("No gear", Map.of()),
                        rodEnabled,
                        new FixedRandom(0.5)
                ),
                "Sporeburst Rod must not be enabled without a poisoner."
        );
    }

    private static void verifySleepFailureAndLegacyGateOption() {
        Team team = new Team(Map.of(
                TeamPosition.ALLY_ATTACKER,
                new Champion("Mikage", ChampionKind.MIKAGE, false, 1, 1, 0),
                TeamPosition.SLEEPER,
                new Champion("Hellhound", ChampionKind.HELLHOUND, true, 1, 1, 0),
                TeamPosition.DEBUFFER,
                new Champion("Mithrala", ChampionKind.MITHRALA, true, 2, 1, 2_000),
                TeamPosition.DAMAGE_DEALER_ONE,
                new Champion("Fenax", ChampionKind.FENAX, false, 1, 1, 0),
                TeamPosition.DAMAGE_DEALER_TWO,
                new Champion("Fenax", ChampionKind.FENAX, false, 1, 1, 0)
        ));
        GearSetup noGear = new GearSetup("No gear", Map.of());
        Simulator simulator = new Simulator(new DamageModel(0.75, 0.15));
        SimulationOptions requireSleep = new SimulationOptions(
                true, false, 1_000, Set.of(), Set.of(), true, false, false, 3, false, 3
        );
        SimulationOptions allowNoSleep = new SimulationOptions(
                false, false, 1_000, Set.of(), Set.of(), true, false, false, 3, false, 3
        );

        SimulationResult gated = simulator.run(
                10,
                team,
                noGear,
                requireSleep,
                new FixedRandom(0.5)
        );
        if (gated.successfulTrials() != 0 || gated.sleepFailedTrials() != 10) {
            throw new AssertionError("Runs without sleep must fail when the gate is enabled.");
        }

        Team disabledSleeperTeam = new Team(Map.of(
                TeamPosition.ALLY_ATTACKER,
                team.at(TeamPosition.ALLY_ATTACKER),
                TeamPosition.SLEEPER,
                new Champion("Hellhound", ChampionKind.HELLHOUND, false, 1, 1, 0),
                TeamPosition.DEBUFFER,
                team.at(TeamPosition.DEBUFFER),
                TeamPosition.DAMAGE_DEALER_ONE,
                team.at(TeamPosition.DAMAGE_DEALER_ONE),
                TeamPosition.DAMAGE_DEALER_TWO,
                team.at(TeamPosition.DAMAGE_DEALER_TWO)
        ));
        SimulationResult disabledSleeper = simulator.run(
                10,
                disabledSleeperTeam,
                noGear,
                requireSleep,
                new FixedRandom(0.5)
        );
        if (disabledSleeper.successfulTrials() != 0) {
            throw new AssertionError("Disabling the sleeper must fail gated runs.");
        }

        SimulationResult ungated = simulator.run(
                10,
                team,
                noGear,
                allowNoSleep,
                new FixedRandom(0.5)
        );
        if (ungated.successfulTrials() != 10 || ungated.averageDebuffs() != 2.0) {
            throw new AssertionError("Disabling the sleep gate must allow legacy-style trials.");
        }

        SimulationOptions assumeSleep = new SimulationOptions(
                true, true, 1_000, Set.of(), Set.of(), false, false, false, 3, false, 3
        );
        Team assumedSleepTeam = team(
                team.at(TeamPosition.SLEEPER),
                disabledMithrala()
        );
        SimulationResult assumedSleep = simulator.run(
                1,
                assumedSleepTeam,
                noGear,
                assumeSleep,
                new FixedRandom(0.5)
        );
        if (assumedSleep.successfulTrials() != 1 || assumedSleep.averageDebuffs() != 1) {
            throw new AssertionError("Assumed Hellhound sleep must land without an Accuracy/Resistance roll.");
        }
    }

    private static void verifyDazeAndToxicMasteries() {
        Team sleeperOnly = team(
                new Champion("Hellhound", ChampionKind.HELLHOUND, true, 1, 1, 114),
                disabledMithrala()
        );
        Simulator simulator = new Simulator(new DamageModel(0.75, 0.15));
        GearSetup daze = new GearSetup(
                "Daze",
                Map.of(TeamPosition.SLEEPER, GearSet.DAZE)
        );
        SequenceRandom baseDazeRandom = new SequenceRandom(0.999, 0.28, 0.7, 0.5);
        SequenceRandom masteredDazeRandom = new SequenceRandom(0.999, 0.28, 0.7, 0.5);
        SimulationResult baseDaze = simulator.run(
                1, sleeperOnly, daze, options(true, 470, false, false, false, 3, false, 3),
                baseDazeRandom
        );
        SimulationResult masteredDaze = simulator.run(
                1, sleeperOnly, daze, options(true, 470, true, false, false, 3, false, 3),
                masteredDazeRandom
        );
        if (baseDaze.successfulTrials() != 0 || masteredDaze.successfulTrials() != 1
                || masteredDaze.averageDebuffs() != 1) {
            throw new AssertionError("Fearsome Presence must raise Daze's chance from 25% to 30%.");
        }

        SimulationResult dazeLandsAfterResistedSkill = simulator.run(
                1,
                sleeperOnly,
                daze,
                options(true, 470, false, false, false, 3, false, 3),
                new FixedRandom(0.0)
        );
        if (dazeLandsAfterResistedSkill.successfulTrials() != 1
                || dazeLandsAfterResistedSkill.averageDebuffs() != 2) {
            throw new AssertionError("Daze sleep must be unresistable and singleton with Hellhound sleep.");
        }

        SimulationResult harvestBaseChance = simulator.run(
                1,
                sleeperOnly,
                new GearSetup("No gear", Map.of()),
                options(true, 0, false, false, false, 3, false, 3),
                new SequenceRandom(0.5, 0.62, 0.5)
        );
        SimulationResult harvestSniperChance = simulator.run(
                1,
                sleeperOnly,
                new GearSetup("No gear", Map.of()),
                new SimulationOptions(
                        true,
                        false,
                        0,
                        Set.of(),
                        Set.of(),
                        true,
                        true,
                        false,
                        3,
                        false,
                        3
                ),
                new SequenceRandom(0.5, 0.62, 0.5)
        );
        if (harvestBaseChance.averageDebuffs() != 1 || harvestSniperChance.averageDebuffs() != 2) {
            throw new AssertionError("Harvest Despair's independent Sniper toggle must raise its chance to 65%.");
        }

        Team toxicTeam = team(
                new Champion("Hellhound", ChampionKind.HELLHOUND, true, 1, 1, 114),
                new Champion("Mithrala", ChampionKind.MITHRALA, true, 2, 1, 114)
        );
        GearSetup toxic = new GearSetup(
                "Hellhound Toxic",
                Map.of(TeamPosition.SLEEPER, GearSet.TOXIC)
        );
        SimulationResult baseToxic = simulator.run(
                1, toxicTeam, toxic, options(true, 0, false, false, false, 3, false, 3),
                new FixedRandom(0.76)
        );
        SimulationResult masteredToxic = simulator.run(
                1,
                toxicTeam,
                toxic,
                new SimulationOptions(
                        true,
                        false,
                        0,
                        Set.of(),
                        Set.of(TeamPosition.SLEEPER),
                        true,
                        false,
                        false,
                        3,
                        false,
                        3
                ),
                new FixedRandom(0.76)
        );
        if (baseToxic.averageDebuffs() != 3 || masteredToxic.averageDebuffs() != 4) {
            throw new AssertionError("Sniper mastery must raise Toxic's chance from 75% to 80%.");
        }
    }

    private static void verifySleepSingletonAndSuccessfulRunAverages() {
        Team sleeperOnly = team(
                new Champion("Hellhound", ChampionKind.HELLHOUND, true, 1, 1, 584),
                disabledMithrala()
        );
        Simulator simulator = new Simulator(new DamageModel(0.75, 0.15));
        GearSetup daze = new GearSetup(
                "Daze",
                Map.of(TeamPosition.SLEEPER, GearSet.DAZE)
        );
        SimulationResult bothSleepSourcesLand = simulator.run(
                1,
                sleeperOnly,
                daze,
                options(true, 470, false, false, false, 3, false, 3),
                new FixedRandom(0.1)
        );
        if (bothSleepSourcesLand.averageDebuffs() != 2) {
            throw new AssertionError("Hellhound and Daze sleep are singleton; Harvest Despair adds one Leech.");
        }

        Team skillSleeperOnly = team(
                new Champion("Hellhound", ChampionKind.HELLHOUND, true, 1, 1, 584),
                disabledMithrala()
        );
        SimulationResult mixedRuns = simulator.run(
                2,
                skillSleeperOnly,
                new GearSetup("No gear", Map.of()),
                options(true, 470, false, false, false, 3, false, 3),
                new SequenceRandom(0.5, 0.0, 0.5, 0.999)
        );
        if (mixedRuns.successfulTrials() != 1 || mixedRuns.sleepFailedTrials() != 1) {
            throw new AssertionError("Successful and sleep-failed runs must be reported separately.");
        }
        assertClose(
                0.6325,
                mixedRuns.averageDamagePerCycle(),
                "successful-run damage average excluding sleep failures"
        );
        assertClose(2.0, mixedRuns.averageDebuffs(), "successful-run debuff average");
        assertClose(1.0, mixedRuns.probabilityForDebuffCount(2), "successful-run debuff distribution");
    }

    private static void verifySporeburstRodAndSmite() {
        Team mithralaTeam = team(
                new Champion("Hellhound", ChampionKind.HELLHOUND, true, 1, 1, 114),
                new Champion("Mithrala", ChampionKind.MITHRALA, true, 2, 1, 114)
        );
        Simulator simulator = new Simulator(new DamageModel(0.75, 0.15));
        GearSetup noGear = new GearSetup("No gear", Map.of());
        SimulationResult rodOff = simulator.run(
                1, mithralaTeam, noGear, options(true, 0, false, false, false, 3, false, 3),
                new FixedRandom(0.1)
        );
        SimulationResult rodOn = simulator.run(
                1, mithralaTeam, noGear, options(true, 0, false, false, true, 3, false, 3),
                new FixedRandom(0.1)
        );
        if (rodOff.averageDebuffs() != 4 || rodOn.averageDebuffs() != 6) {
            throw new AssertionError("Sporeburst Rod must add one unresistable poison per landed A1 poison.");
        }

        SimulationResult rodWithToxic = simulator.run(
                1,
                mithralaTeam,
                new GearSetup(
                        "Mithrala Toxic",
                        Map.of(TeamPosition.DEBUFFER, GearSet.TOXIC)
                ),
                options(true, 0, false, false, true, 3, false, 3),
                new FixedRandom(0.1)
        );
        if (rodWithToxic.averageDebuffs() != 8) {
            throw new AssertionError("Sporeburst Rod must not trigger from Toxic poisons.");
        }

        SimulationResult sixStarSmite = simulator.run(
                1, mithralaTeam, noGear, options(false, 1_000, false, false, false, 3, true, 6),
                new FixedRandom(0.1)
        );
        if (sixStarSmite.averageDebuffs() != 1) {
            throw new AssertionError("Six-star Smite must be guaranteed, unresistable, and singleton.");
        }

        SimulationOptions stars = options(false, 0, false, false, false, 3, true, 2);
        assertClose(0.15, stars.smiteChance(), "two-star Smite chance");
        assertClose(0.20, stars.sporeburstRodChance(), "three-star Sporeburst Rod chance");
        assertClose(
                0.30,
                options(false, 0, false, false, false, 1, true, 3).smiteChance(),
                "three-star Smite chance"
        );
        assertClose(
                0.30,
                options(false, 0, false, false, false, 1, true, 4).smiteChance(),
                "four-star Smite chance"
        );
        assertClose(
                0.35,
                options(false, 0, false, false, false, 6, false, 1).sporeburstRodChance(),
                "six-star Sporeburst Rod chance"
        );
    }

    private static void verifyAthelWeaken() {
        Team athelTeam = team(
                new Champion("Hellhound", ChampionKind.HELLHOUND, true, 1, 1, 114),
                new Champion("Athel", ChampionKind.ATHEL, true, 3, 1, 114)
        );
        SimulationResult result = new Simulator(new DamageModel(0.75, 0.15)).run(
                1,
                athelTeam,
                new GearSetup("No gear", Map.of()),
                options(true, 0, false, false, false, 3, false, 3),
                new FixedRandom(0.5)
        );
        if (result.averageDebuffs() != 3) {
            throw new AssertionError("Athel must apply Weaken only on her third hit.");
        }
        assertClose(3.865625, result.averageDamagePerCycle(), "Athel Weaken damage bonus");
    }

    private static void verifyAthelWeakAffinityLossesAreZeroDamageSamples() {
        Team athelTeam = team(
                new Champion("Hellhound", ChampionKind.HELLHOUND, true, 1, 1, 114),
                new Champion("Athel", ChampionKind.ATHEL, true, 3, 1, 114)
        );
        SimulationOptions assumeSleep = new SimulationOptions(
                true, true, 0, Set.of(), Set.of(), true, false, false, 3, false, 3
        );
        Simulator simulator = new Simulator(new DamageModel(0.75, 0.15));
        GearSetup noGear = new GearSetup("No gear", Map.of());

        SimulationResult affinityLoss = simulator.run(
                1,
                athelTeam,
                noGear,
                assumeSleep,
                new FixedRandom(0.1)
        );
        if (affinityLoss.affinityFailedTrials() != 1
                || affinityLoss.successfulTrials() != 0
                || affinityLoss.damageSampleTrials() != 1) {
            throw new AssertionError("Athel weak affinity must end and count the run.");
        }
        assertClose(0, affinityLoss.averageDamagePerCycle(), "Athel affinity-failed run damage");

        SimulationResult mixedRuns = simulator.run(
                2,
                athelTeam,
                noGear,
                assumeSleep,
                new SequenceRandom(
                        0.1, 0.1, 0.1,
                        0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9
                )
        );
        if (mixedRuns.affinityFailedTrials() != 1
                || mixedRuns.successfulTrials() != 1
                || mixedRuns.damageSampleTrials() != 2) {
            throw new AssertionError("Athel affinity losses must remain in the damage average.");
        }
        assertClose(1.58125, mixedRuns.averageDamagePerCycle(), "Athel damage average including failed run");
        assertClose(1.58125, mixedRuns.damageStandardDeviation(), "Athel SD including failed run");
        assertClose(2, mixedRuns.averageDebuffs(), "Athel debuffs from completed runs");
    }

    private static void verifyDorothyWeakenSmiteAndToxic() {
        Team dorothyTeam = team(
                new Champion("Hellhound", ChampionKind.HELLHOUND, true, 1, 1, 114),
                new Champion("Dorothy", ChampionKind.DOROTHY, true, 1, 1, 114)
        );
        GearSetup toxic = new GearSetup(
                "Dorothy Toxic",
                Map.of(TeamPosition.DEBUFFER, GearSet.TOXIC)
        );
        SimulationResult landed = new Simulator(new DamageModel(0.75, 0.15)).run(
                1,
                dorothyTeam,
                toxic,
                options(true, 0, false, false, false, 4, true, 5),
                new SequenceRandom(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5)
        );
        if (landed.averageDebuffs() != 5) {
            throw new AssertionError("Dorothy must apply Weaken, five-star Smite, and Toxic after Hellhound sleep.");
        }
        assertClose(2.3828125, landed.averageDamagePerCycle(), "Dorothy Weaken damage bonus");

        Team lowAccuracyDorothy = team(
                new Champion("Hellhound", ChampionKind.HELLHOUND, true, 1, 1, 114),
                new Champion("Dorothy", ChampionKind.DOROTHY, true, 1, 1, 0)
        );
        SimulationResult resisted = new Simulator(new DamageModel(0.75, 0.15)).run(
                1,
                lowAccuracyDorothy,
                toxic,
                new SimulationOptions(
                        true, true, 1_000, Set.of(), Set.of(), true, false, false, 4, true, 5
                ),
                new SequenceRandom(0.9, 0.9, 0.5, 0.0, 0.0, 0.9)
        );
        if (resisted.averageDebuffs() != 1) {
            throw new AssertionError("Dorothy's Weaken and Smite must both be resistable.");
        }
    }

    private static void verifyWysteriRunesSniperWeakenAndToxic() {
        Team wysteriTeam = team(
                new Champion("Hellhound", ChampionKind.HELLHOUND, true, 1, 1, 114),
                new Champion("Wysteri Vineguard", ChampionKind.WYSTERI_VINEGUARD, true, 2, 1, 0)
        );
        GearSetup toxic = new GearSetup(
                "Wysteri Toxic",
                Map.of(TeamPosition.DEBUFFER, GearSet.TOXIC)
        );
        SimulationOptions oneRuneWithSniper = new SimulationOptions(
                true,
                true,
                0,
                Set.of(),
                Set.of(TeamPosition.DEBUFFER),
                true,
                false,
                false,
                4,
                true,
                5,
                1
        );
        assertClose(
                0.85,
                oneRuneWithSniper.wysteriWeakenChance(TeamPosition.DEBUFFER),
                "Wysteri Sniper plus one Weaken rune chance"
        );
        assertClose(
                0.90,
                new SimulationOptions(
                        true, true, 0, Set.of(), Set.of(TeamPosition.DEBUFFER),
                        true, false, false, 4, false, 3, 2
                ).wysteriWeakenChance(TeamPosition.DEBUFFER),
                "Wysteri Sniper plus two Weaken runes chance"
        );

        SimulationResult landedOnce = new Simulator(new DamageModel(0.75, 0.15)).run(
                1,
                wysteriTeam,
                toxic,
                oneRuneWithSniper,
                new SequenceRandom(0.9, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5)
        );
        if (landedOnce.averageDebuffs() != 4) {
            throw new AssertionError(
                    "Wysteri should land at most one singleton Weaken, plus Hellhound sleep and Toxic procs."
            );
        }

        SimulationOptions oneRuneNoSniper = new SimulationOptions(
                true, true, 0, Set.of(), Set.of(), true, false, false, 4, false, 3, 1
        );
        assertClose(
                0.80,
                oneRuneNoSniper.wysteriWeakenChance(TeamPosition.DEBUFFER),
                "Wysteri one rune without Sniper chance"
        );

        SimulationResult resisted = new Simulator(new DamageModel(0.75, 0.15)).run(
                1,
                wysteriTeam,
                new GearSetup("No gear", Map.of()),
                new SimulationOptions(
                        true, true, 1_000, Set.of(), Set.of(), true, false, false, 4, false, 3, 0
                ),
                new SequenceRandom(0.9, 0.5, 0.0, 0.5, 0.5, 0.5, 0.5, 0.5)
        );
        if (resisted.averageDebuffs() != 1) {
            throw new AssertionError("Wysteri's Weaken must be resistable.");
        }

        expectIllegalArgument(
                () -> new SimulationOptions(
                        true, true, 0, Set.of(), Set.of(), true, false, false, 4, false, 3, 3
                ),
                "Wysteri cannot equip more than two Weaken runes."
        );
    }

    private static Team team(Champion sleeper, Champion debuffer) {
        return new Team(Map.of(
                TeamPosition.ALLY_ATTACKER,
                new Champion("Mikage", ChampionKind.MIKAGE, false, 1, 1, 114),
                TeamPosition.SLEEPER,
                sleeper,
                TeamPosition.DEBUFFER,
                debuffer,
                TeamPosition.DAMAGE_DEALER_ONE,
                new Champion("Fenax", ChampionKind.FENAX, false, 1, 1, 114),
                TeamPosition.DAMAGE_DEALER_TWO,
                new Champion("Fenax", ChampionKind.FENAX, false, 1, 1, 114)
        ));
    }

    private static Champion disabledMithrala() {
        return new Champion("Mithrala", ChampionKind.MITHRALA, false, 2, 1, 114);
    }

    private static SimulationOptions options(
            boolean requireSleep,
            double bossResistance,
            boolean fearsomePresence,
            boolean sniperMastery,
            boolean sporeburstRod,
            int rodStars,
            boolean smite,
            int smiteStars
    ) {
        return new SimulationOptions(
                requireSleep,
                false,
                bossResistance,
                fearsomePresence ? Set.of(TeamPosition.SLEEPER) : Set.of(),
                sniperMastery ? Set.of(TeamPosition.SLEEPER) : Set.of(),
                true,
                false,
                sporeburstRod,
                rodStars,
                smite,
                smiteStars
        );
    }

    private static void assertClose(double expected, double actual, String label) {
        if (Math.abs(expected - actual) > 1e-6) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void expectIllegalArgument(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    private static final class FixedRandom extends Random {
        private static final long serialVersionUID = 1L;
        private final double value;

        private FixedRandom(double value) {
            this.value = value;
        }

        @Override
        public double nextDouble() {
            return value;
        }
    }

    private static final class SequenceRandom extends Random {
        private static final long serialVersionUID = 1L;
        private final double[] values;
        private int index;

        private SequenceRandom(double... values) {
            this.values = values.clone();
        }

        @Override
        public double nextDouble() {
            if (index >= values.length) {
                throw new AssertionError("Unexpected random draw.");
            }
            return values[index++];
        }
    }
}
