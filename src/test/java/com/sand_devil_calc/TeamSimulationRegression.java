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
        verifySleepResistLossesAndWinTurns();
        verifySporeburstRodAndSmite();
        verifyAthelWeaken();
        verifyAthelWeakAffinityLossesAreZeroDamageSamples();
        verifyDorothyWeakenSmiteAndToxic();
        verifyWysteriRunesSniperWeakenAndToxic();
        verifyFenaxDamageModel();
        verifyFenaxBookedDamageBonus();
        verifyQuickstrikeTurnLimitAndWorstLossMargins();
        verifyDamageStandardDeviationBands();
        verifyMikageBuffsFenaxDamage();
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
        assertClose(10, gated.sleepResistFailedTrials(), "sleep resist losses are tracked separately");

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
        assertClose(0, disabledSleeper.sleepResistFailedTrials(),
                "missing sleeper is not classified as a sleep resist");

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
        assertClose(1, mixedRuns.sleepResistFailedTrials(),
                "a resisted Hellhound sleep is counted as a sleep-resist loss");
        assertClose(
                0,
                mixedRuns.averageFenaxDamagePerCycle(),
                "non-Fenax champions contribute no damage"
        );
        assertClose(2.0, mixedRuns.averageDebuffs(), "successful-run debuff average");
        assertClose(1.0, mixedRuns.probabilityForDebuffCount(2), "successful-run debuff distribution");
    }

    private static void verifySleepResistLossesAndWinTurns() {
        Simulator simulator = new Simulator(new DamageModel(0, 0));
        SimulationOptions noSleepGate = new SimulationOptions(
                false, false, 0, Set.of(), Set.of(), false, false, false, 4, false, 3
        );
        GearSetup noGear = new GearSetup("No gear", Map.of());

        SimulationResult turnOneWin = simulator.run(
                1, turnWinTeam(800_000), noGear, noSleepGate, new FixedRandom(0.5)
        );
        assertClose(1, turnOneWin.bossKillsByTurn().getOrDefault(1, 0L),
                "first combined Fenax attack is a turn-one win");

        SimulationResult turnThreeWin = simulator.run(
                1, turnWinTeam(450_000), noGear, noSleepGate, new FixedRandom(0.5)
        );
        assertClose(1, turnThreeWin.bossKillsByTurn().getOrDefault(3, 0L),
                "Fenax+4 can finish the boss on turn three");

        SimulationResult turnFourWin = simulator.run(
                1, turnWinTeam(310_000), noGear, noSleepGate, new FixedRandom(0.5)
        );
        assertClose(1, turnFourWin.bossKillsByTurn().getOrDefault(4, 0L),
                "Fenax+0 can finish the boss on turn four");
        assertClose(0, turnFourWin.bossKillsByTurn().getOrDefault(2, 0L),
                "turn two is the boss waking and cannot be a win");
        assertClose(1, turnFourWin.bossKilledTrials(),
                "turn-based win counts sum to killed trials");
    }

    private static Team turnWinTeam(double damagePerHit) {
        return new Team(Map.of(
                TeamPosition.ALLY_ATTACKER,
                new Champion("Mikage", ChampionKind.MIKAGE, false, 1, 0, 0),
                TeamPosition.SLEEPER,
                new Champion("Hellhound", ChampionKind.HELLHOUND, false, 1, 0, 0),
                TeamPosition.DEBUFFER,
                disabledMithrala(),
                TeamPosition.DAMAGE_DEALER_ONE,
                new Champion("Fenax+4", ChampionKind.FENAX, true, 2, damagePerHit, 0),
                TeamPosition.DAMAGE_DEALER_TWO,
                new Champion("Fenax+0", ChampionKind.FENAX, true, 2, damagePerHit, 0)
        ));
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
        assertClose(0, result.averageFenaxDamagePerCycle(), "Athel damage is excluded from the damage model");
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
                || affinityLoss.damageSampleTrials() != 1
                || affinityLoss.damageFailedTrials() != 0) {
            throw new AssertionError("Athel weak affinity must end and count the run.");
        }
        assertClose(0, affinityLoss.averageFenaxDamagePerCycle(), "Athel affinity-failed run damage");

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
        assertClose(0, mixedRuns.averageFenaxDamagePerCycle(), "Athel failed runs remain zero Fenax damage");
        assertClose(0, mixedRuns.fenaxDamageStandardDeviation(), "no Fenax damage means zero damage SD");
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
        assertClose(0, landed.averageFenaxDamagePerCycle(), "Dorothy damage is excluded from the damage model");

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

    private static void verifyFenaxDamageModel() {
        double masteryMultiplier = 1.08 * 1.12 * 1.06;
        double fenaxBaseHitDamage = 4.1 * 8_000 * (1.0 + 4.0);
        Team fenaxTeam = new Team(Map.of(
                TeamPosition.ALLY_ATTACKER,
                new Champion("Ally attacker", ChampionKind.DOROTHY, false, 1, 0, 0),
                TeamPosition.SLEEPER,
                new Champion("Hellhound", ChampionKind.HELLHOUND, false, 1, 0, 0),
                TeamPosition.DEBUFFER,
                disabledMithrala(),
                TeamPosition.DAMAGE_DEALER_ONE,
                new Champion("Fenax 1", ChampionKind.FENAX, true, 2, fenaxBaseHitDamage, 0,
                        8_000, 4.0),
                TeamPosition.DAMAGE_DEALER_TWO,
                new Champion("Fenax 2", ChampionKind.FENAX, true, 2, fenaxBaseHitDamage, 0,
                        8_000, 4.0)
        ));
        SimulationOptions noDebuffs = new SimulationOptions(
                false, false, 0, Set.of(), Set.of(), false, false, false, 4, false, 3
        );
        GearSetup noGear = new GearSetup("Fenax damage only", Map.of());
        Simulator simulator = new Simulator(new DamageModel(0.75, 0.15));

        SimulationResult baseDamage = simulator.run(
                1,
                fenaxTeam,
                noGear,
                noDebuffs,
                new SequenceRandom(
                        0.5, 0.9, 0.9,
                        0.5, 0.9, 0.9,
                        0.5, 0.9, 0.9,
                        0.5, 0.9, 0.9,
                        0.5, 0.9, 0.5, 0.9,
                        0.5, 0.9, 0.5, 0.9
                )
        );
        assertClose(
                1_312_000 * masteryMultiplier,
                baseDamage.baseFenaxDamagePerCycle(),
                "Fenax baseline includes unconditional mastery multipliers"
        );
        assertClose(
                328_000 * masteryMultiplier,
                baseDamage.averageFenaxDamagePerCycle(),
                "Fenax damage includes both attack sets and the zero-debuff boss multiplier"
        );
        assertClose(
                0.25,
                baseDamage.averageDebuffDamageMultiplier(),
                "debuff multiplier is reported separately from Fenax damage"
        );
        assertClose(
                164_000 * masteryMultiplier,
                baseDamage.fenaxDamageByPosition()
                        .get(TeamPosition.DAMAGE_DEALER_ONE).averageDamagePerCycle(),
                "first Fenax has independent damage statistics"
        );
        assertClose(
                164_000 * masteryMultiplier,
                baseDamage.fenaxDamageByPosition()
                        .get(TeamPosition.DAMAGE_DEALER_TWO).averageDamagePerCycle(),
                "second Fenax has independent damage statistics"
        );

        SimulationResult everyHitProcs = simulator.run(
                1,
                fenaxTeam,
                noGear,
                noDebuffs,
                new SequenceRandom(
                        0.5, 0.0, 0.9,
                        0.5, 0.0, 0.9,
                        0.5, 0.0, 0.9,
                        0.5, 0.0, 0.9,
                        0.5, 0.0, 0.5, 0.0,
                        0.5, 0.0, 0.5, 0.0
                )
        );
        assertClose(
                426_400 * masteryMultiplier,
                everyHitProcs.averageFenaxDamagePerCycle(),
                "independent strong-affinity damage bonus on all four hits"
        );
        assertClose(
                0.25,
                everyHitProcs.averageDebuffDamageMultiplier(),
                "strong-affinity damage does not change the separate debuff multiplier"
        );
        assertClose(
                213_200 * masteryMultiplier,
                everyHitProcs.fenaxDamageByPosition()
                        .get(TeamPosition.DAMAGE_DEALER_ONE).averageDamagePerCycle(),
                "first Fenax damage distribution is separately tracked"
        );
        assertClose(
                213_200 * masteryMultiplier,
                everyHitProcs.fenaxDamageByPosition()
                        .get(TeamPosition.DAMAGE_DEALER_TWO).averageDamagePerCycle(),
                "second Fenax damage distribution is separately tracked"
        );

        Team weakenFenaxTeam = new Team(Map.of(
                TeamPosition.ALLY_ATTACKER,
                fenaxTeam.at(TeamPosition.ALLY_ATTACKER),
                TeamPosition.SLEEPER,
                fenaxTeam.at(TeamPosition.SLEEPER),
                TeamPosition.DEBUFFER,
                new Champion("Dorothy", ChampionKind.DOROTHY, true, 1, 0, 0),
                TeamPosition.DAMAGE_DEALER_ONE,
                fenaxTeam.at(TeamPosition.DAMAGE_DEALER_ONE),
                TeamPosition.DAMAGE_DEALER_TWO,
                fenaxTeam.at(TeamPosition.DAMAGE_DEALER_TWO)
        ));
        SimulationOptions weakenOptions = new SimulationOptions(
                false, false, 0, Set.of(), Set.of(), false, false, false, 4, false, 3
        );
        SimulationResult weakenDamage = new Simulator(new DamageModel(0, 0)).run(
                1,
                weakenFenaxTeam,
                noGear,
                weakenOptions,
                new SequenceRandom(
                        0.0, 0.9, 0.9,
                        0.5, 0.9, 0.9,
                        0.5, 0.9, 0.9,
                        0.5, 0.9, 0.9,
                        0.5, 0.9, 0.9,
                        0.5, 0.9, 0.5, 0.9,
                        0.5, 0.9, 0.5, 0.9
                )
        );
        assertClose(
                1_312_000 * masteryMultiplier,
                weakenDamage.averageFenaxDamagePerCycle(),
                "Weaken does not get included in Fenax-only damage"
        );
        assertClose(
                1.25,
                weakenDamage.averageDebuffDamageMultiplier(),
                "Weaken is reflected only in the separate debuff multiplier"
        );

        SimulationResult lowerDamageRoll = simulator.run(
                1,
                fenaxTeam,
                noGear,
                noDebuffs,
                new SequenceRandom(
                        0.0, 0.9, 0.9,
                        0.0, 0.9, 0.9,
                        0.0, 0.9, 0.9,
                        0.0, 0.9, 0.9,
                        0.0, 0.9, 0.0, 0.9,
                        0.0, 0.9, 0.0, 0.9
                )
        );
        assertClose(
                295_200 * masteryMultiplier,
                lowerDamageRoll.averageFenaxDamagePerCycle(),
                "minus-ten-percent damage variation for every Fenax hit"
        );

        SimulationResult upperDamageRoll = simulator.run(
                1,
                fenaxTeam,
                noGear,
                noDebuffs,
                new SequenceRandom(
                        1.0, 0.9, 0.9,
                        1.0, 0.9, 0.9,
                        1.0, 0.9, 0.9,
                        1.0, 0.9, 0.9,
                        1.0, 0.9, 1.0, 0.9,
                        1.0, 0.9, 1.0, 0.9
                )
        );
        assertClose(
                360_800 * masteryMultiplier,
                upperDamageRoll.averageFenaxDamagePerCycle(),
                "plus-ten-percent damage variation for every Fenax hit"
        );

        Team singleOutTeam = new Team(Map.of(
                TeamPosition.ALLY_ATTACKER,
                new Champion("Ally attacker", ChampionKind.DOROTHY, false, 1, 0, 0),
                TeamPosition.SLEEPER,
                new Champion("Hellhound", ChampionKind.HELLHOUND, false, 1, 0, 0),
                TeamPosition.DEBUFFER,
                disabledMithrala(),
                TeamPosition.DAMAGE_DEALER_ONE,
                new Champion("Fenax+4", ChampionKind.FENAX, true, 2, 500_000, 0),
                TeamPosition.DAMAGE_DEALER_TWO,
                new Champion("Fenax+0", ChampionKind.FENAX, true, 2, 500_000, 0)
        ));
        Simulator unmitigatedSimulator = new Simulator(new DamageModel(0, 0));
        SimulationResult singleOutAtFourthStrike = unmitigatedSimulator.run(
                1,
                singleOutTeam,
                noGear,
                noDebuffs,
                new SequenceRandom(
                        0.5, 0.9, 0.9,
                        0.5, 0.9, 0.9,
                        0.5, 0.9, 0.9,
                        0.5, 0.9, 0.9,
                        0.5, 0.9, 0.5, 0.9,
                        0.5, 0.9, 0.5, 0.9
                )
        );
        double normalMasteryHit = 500_000 * masteryMultiplier;
        assertTrue(
                singleOutAtFourthStrike.averageFenaxDamagePerCycle() > 3_082_910,
                "damage statistics include killing-hit overkill while Single Out triggers below 40% HP"
        );
        assertClose(1, singleOutAtFourthStrike.bossKilledTrials(),
                "Fenaxes kill the boss within the second attack set");
        assertClose(0, singleOutAtFourthStrike.damageFailedTrials(),
                "a killed boss is not counted as an insufficient-damage loss");
        verifyKillingHitOverkillIsCounted();

        Team gemstoneTeam = new Team(Map.of(
                TeamPosition.ALLY_ATTACKER,
                new Champion("Ally attacker", ChampionKind.DOROTHY, false, 1, 0, 0),
                TeamPosition.SLEEPER,
                new Champion("Hellhound", ChampionKind.HELLHOUND, false, 1, 0, 0),
                TeamPosition.DEBUFFER,
                disabledMithrala(),
                TeamPosition.DAMAGE_DEALER_ONE,
                new Champion("Fenax+4", ChampionKind.FENAX, true, 2,
                        fenaxBaseHitDamage, 0, 8_000, 4.0, 2, 1),
                TeamPosition.DAMAGE_DEALER_TWO,
                new Champion("Fenax+0", ChampionKind.FENAX, true, 2,
                        fenaxBaseHitDamage, 0, 8_000, 4.0, 1, 2, true)
        ));
        SimulationResult gemstoneDamage = simulator.run(
                1,
                gemstoneTeam,
                noGear,
                noDebuffs,
                new SequenceRandom(
                        0.5, 0.9, 0.9,
                        0.5, 0.9, 0.9,
                        0.5, 0.9, 0.9,
                        0.5, 0.9, 0.9,
                        0.5, 0.9, 0.5, 0.9,
                        0.5, 0.9, 0.5, 0.9
                )
        );
        double baseGemstoneHit = 164_000 * masteryMultiplier;
        double fenaxFourGemstoneDamage =
                baseGemstoneHit * 2 * (Math.pow(1.10, 3) + Math.pow(1.10, 1));
        double fenaxZeroGemstoneDamage =
                baseGemstoneHit * 2 * (Math.pow(1.10, 3) + Math.pow(1.10, 2)) * 1.12;
        assertClose(
                (fenaxFourGemstoneDamage + fenaxZeroGemstoneDamage) * 0.25,
                gemstoneDamage.averageFenaxDamagePerCycle(),
                "Quickstrike applies on turn one, Dungeon Delver applies on both Fenax attacks"
        );
        assertClose(
                fenaxFourGemstoneDamage,
                gemstoneDamage.fenaxDamageByPosition()
                        .get(TeamPosition.DAMAGE_DEALER_ONE).baseDamagePerCycle(),
                "Fenax+4 Quickstrike only applies to its turn-one attack"
        );
        assertClose(
                fenaxZeroGemstoneDamage,
                gemstoneDamage.fenaxDamageByPosition()
                        .get(TeamPosition.DAMAGE_DEALER_TWO).baseDamagePerCycle(),
                "Fenax+0 Quickstrike and Zeal are accounted for across the two attacks"
        );

        Team durationTeam = new Team(Map.of(
                TeamPosition.ALLY_ATTACKER,
                fenaxTeam.at(TeamPosition.ALLY_ATTACKER),
                TeamPosition.SLEEPER,
                new Champion("Hellhound", ChampionKind.HELLHOUND, true, 1, 0, 0),
                TeamPosition.DEBUFFER,
                disabledMithrala(),
                TeamPosition.DAMAGE_DEALER_ONE,
                new Champion("Fenax", ChampionKind.FENAX, true, 2, 100_000, 0),
                TeamPosition.DAMAGE_DEALER_TWO,
                new Champion("Second Fenax", ChampionKind.FENAX, true, 2, 100_000, 0)
        ));
        SimulationOptions guaranteedSleep = new SimulationOptions(
                false, true, 0, Set.of(), Set.of(), false, false, false, 4, false, 3
        );
        SimulationResult durationDamage = simulator.run(
                1,
                durationTeam,
                new GearSetup("Toxic duration", Map.of(TeamPosition.SLEEPER, GearSet.TOXIC)),
                guaranteedSleep,
                new FixedRandom(0.5)
        );
        assertClose(
                100_000 * masteryMultiplier * (4 * 0.6325 + 4 * 0.458125),
                durationDamage.averageFenaxDamagePerCycle(),
                "two-turn Toxic persists through turns three and four while one-turn Sleep expires"
        );
        assertClose(2, durationDamage.averageDebuffs(),
                "debuff statistics count only first-turn debuffs");
        assertClose(0.6325, durationDamage.averageDebuffDamageMultiplier(),
                "debuff multiplier statistics remain limited to first-turn debuffs");
        assertClose(1, durationDamage.damageFailedTrials(),
                "surviving the second attack set is recorded as insufficient damage");
        assertClose(0, durationDamage.bossKilledTrials(),
                "insufficient Fenax damage does not count as a kill");

        expectIllegalArgument(
                () -> new Champion(
                        "Fenax", ChampionKind.FENAX, true, 2, fenaxBaseHitDamage, 0,
                        8_000, 4.0, 2, 2
                ),
                "Fenax cannot equip more than three gemstones."
        );
    }

    private static void verifyQuickstrikeTurnLimitAndWorstLossMargins() {
        double masteryMultiplier = 1.08 * 1.12 * 1.06;
        double baseHit = 100_000 * masteryMultiplier;
        Team team = new Team(Map.of(
                TeamPosition.ALLY_ATTACKER,
                new Champion("Ally attacker", ChampionKind.DOROTHY, false, 1, 0, 0),
                TeamPosition.SLEEPER,
                new Champion("Hellhound", ChampionKind.HELLHOUND, false, 1, 0, 0),
                TeamPosition.DEBUFFER,
                disabledMithrala(),
                TeamPosition.DAMAGE_DEALER_ONE,
                new Champion("Fenax+4", ChampionKind.FENAX, true, 2, 100_000, 0,
                        8_000, 4.0, 1, 0),
                TeamPosition.DAMAGE_DEALER_TWO,
                new Champion("Fenax+0", ChampionKind.FENAX, false, 2, 100_000, 0)
        ));
        SimulationOptions noSleepGate = new SimulationOptions(
                false, false, 0, Set.of(), Set.of(), false, false, false, 4, false, 3
        );
        SimulationResult result = new Simulator(new DamageModel(0, 0)).run(
                1,
                team,
                new GearSetup("Quickstrike duration", Map.of()),
                noSleepGate,
                new FixedRandom(0.5)
        );
        double expectedDamage = baseHit * (2 * 1.10 + 2);
        double expectedShortfallPercent = (3_082_910 - expectedDamage) / expectedDamage * 100;
        assertClose(expectedDamage, result.averageFenaxDamagePerCycle(),
                "Quickstrike increases both turn-one hits but not either later hit");
        assertClose(expectedDamage, result.baseFenaxDamagePerCycle(),
                "the baseline includes first-turn-only Quickstrike");
        assertClose(1, result.survivedTurnThreeTrials(),
                "the damage-failed run is measured after Fenax+4's turn-three attack");
        assertClose(expectedShortfallPercent, result.worstTurnThreeMissPercent(),
                "turn-three shortfall is relative to damage dealt through turn three");
        assertClose(expectedShortfallPercent, result.worstTurnFourMissPercent(),
                "turn-four shortfall is relative to total damage dealt through turn four");
    }

    private static void verifyFenaxBookedDamageBonus() {
        double baseHit = 100_000;
        double masteryMultiplier = 1.08 * 1.12 * 1.06;
        Team bookedTeam = new Team(Map.of(
                TeamPosition.ALLY_ATTACKER,
                new Champion("Ally attacker", ChampionKind.DOROTHY, false, 1, 0, 0),
                TeamPosition.SLEEPER,
                new Champion("Hellhound", ChampionKind.HELLHOUND, false, 1, 0, 0),
                TeamPosition.DEBUFFER,
                disabledMithrala(),
                TeamPosition.DAMAGE_DEALER_ONE,
                new Champion("Booked Fenax", ChampionKind.FENAX, true, 2, baseHit, 0,
                        0, 0, 0, 0, false, 0.20),
                TeamPosition.DAMAGE_DEALER_TWO,
                new Champion("Disabled Fenax", ChampionKind.FENAX, false, 2, baseHit, 0)
        ));
        SimulationOptions noSleepGate = new SimulationOptions(
                false, false, 0, Set.of(), Set.of(), false, false, false, 4, false, 3
        );
        SimulationResult result = new Simulator(new DamageModel(0, 0)).run(
                1,
                bookedTeam,
                new GearSetup("Booked Fenax", Map.of()),
                noSleepGate,
                new FixedRandom(0.5)
        );
        double expectedDamage = baseHit * 1.20 * masteryMultiplier * 4;
        assertClose(expectedDamage, result.averageFenaxDamagePerCycle(),
                "Fenax book bonus multiplies total damage by 1.20");
        assertClose(expectedDamage, result.baseFenaxDamagePerCycle(),
                "Fenax baseline includes booked damage bonus");
    }

    private static void verifyKillingHitOverkillIsCounted() {
        double baseHit = 1_300_000;
        double masteryMultiplier = 1.08 * 1.12 * 1.06;
        Team team = new Team(Map.of(
                TeamPosition.ALLY_ATTACKER,
                new Champion("Ally attacker", ChampionKind.DOROTHY, false, 1, 0, 0),
                TeamPosition.SLEEPER,
                new Champion("Hellhound", ChampionKind.HELLHOUND, false, 1, 0, 0),
                TeamPosition.DEBUFFER,
                disabledMithrala(),
                TeamPosition.DAMAGE_DEALER_ONE,
                new Champion("Fenax+4", ChampionKind.FENAX, true, 1, baseHit, 0),
                TeamPosition.DAMAGE_DEALER_TWO,
                new Champion("Fenax+0", ChampionKind.FENAX, false, 1, 0, 0)
        ));
        SimulationOptions noSleepGate = new SimulationOptions(
                false, false, 0, Set.of(), Set.of(), false, false, false, 4, false, 3
        );
        SimulationResult result = new Simulator(new DamageModel(0, 0)).run(
                1,
                team,
                new GearSetup("Killing hit overkill", Map.of()),
                noSleepGate,
                new FixedRandom(0.5)
        );
        double expectedDamage = 2 * baseHit * masteryMultiplier;
        assertClose(expectedDamage, result.averageFenaxDamagePerCycle(),
                "the full killing-hit damage is included in Fenax damage statistics");
        assertTrue(expectedDamage > 3_082_910,
                "the test strike sequence deals more damage than the boss HP");
        assertClose(1, result.bossKilledTrials(),
                "the boss still dies when the hit deals overkill damage");
        assertClose(0, result.damageFailedTrials(),
                "overkill does not convert a kill into a damage loss");

        SimulationResult cappedResult = new Simulator(new DamageModel(0, 0), false).run(
                1,
                team,
                new GearSetup("Capped killing hit", Map.of()),
                noSleepGate,
                new FixedRandom(0.5)
        );
        assertClose(3_082_910, cappedResult.averageFenaxDamagePerCycle(),
                "comparison mode caps recorded damage at the boss's remaining HP");
        assertClose(1, cappedResult.bossKilledTrials(),
                "the boss still dies in capped-damage comparison mode");
    }

    private static void verifyDamageStandardDeviationBands() {
        Team team = new Team(Map.of(
                TeamPosition.ALLY_ATTACKER,
                new Champion("Ally attacker", ChampionKind.DOROTHY, false, 1, 0, 0),
                TeamPosition.SLEEPER,
                new Champion("Hellhound", ChampionKind.HELLHOUND, false, 1, 0, 0),
                TeamPosition.DEBUFFER,
                disabledMithrala(),
                TeamPosition.DAMAGE_DEALER_ONE,
                new Champion("Fenax+4", ChampionKind.FENAX, true, 2, 100_000, 0),
                TeamPosition.DAMAGE_DEALER_TWO,
                new Champion("Fenax+0", ChampionKind.FENAX, false, 2, 100_000, 0)
        ));
        SimulationOptions noSleepGate = new SimulationOptions(
                false, false, 0, Set.of(), Set.of(), false, false, false, 4, false, 3
        );
        SimulationResult result = new Simulator(new DamageModel(0, 0)).runWithDamageBands(
                100,
                team,
                new GearSetup("Damage bands", Map.of()),
                noSleepGate,
                42
        );

        long previousBandCount = 0;
        for (int standardDeviations = 1; standardDeviations <= 3; standardDeviations++) {
            SimulationResult.DamageBandStatistics band =
                    result.damageBandStatistics().get(standardDeviations);
            assertTrue(band.trialCount() >= previousBandCount,
                    "standard-deviation bands are cumulative");
            assertTrue(
                    band.trialCount() == band.damageFailedTrials()
                            + band.affinityFailedTrials()
                            + band.bossKillsByTurn().values().stream()
                                    .mapToLong(Long::longValue)
                                    .sum(),
                    "damage-band outcomes account for every included trial"
            );
            assertClose(
                    band.trialCount(),
                    band.byDebuffCount().values().stream()
                            .mapToLong(SimulationResult.DebuffCountStatistics::trialCount)
                            .sum(),
                    "debuff-count breakdown accounts for every band trial"
            );
            assertTrue(
                    band.byDebuffCount().entrySet().stream().allMatch(entry -> {
                        SimulationResult.DebuffCountStatistics countStats = entry.getValue();
                        long outcomes = countStats.affinityFailedTrials()
                                + countStats.damageFailedTrials()
                                + countStats.bossKillsByTurn().values().stream()
                                        .mapToLong(Long::longValue)
                                        .sum();
                        return countStats.trialCount() == outcomes;
                    }),
                    "debuff-count outcomes account for every subgroup trial"
            );
            previousBandCount = band.trialCount();
        }
        assertTrue(result.damageBandStatistics().get(3).trialCount() > 0,
                "three-standard-deviation band includes observed damage trials");
        assertTrue(result.damageBandStatistics().get(3).trialCount() <= 100,
                "three-standard-deviation band does not exceed all simulated damage trials");
        assertTrue(
                result.damageBandStatistics().get(3).turnOneWinDistanceSamples() > 0,
                "damage-band trials with turn-one damage report a turn-one win distance"
        );
        assertTrue(
                result.damageBandStatistics().get(3).averageTurnOneWinDistancePercent() > 0,
                "the turn-one win distance is positive when turn-one attacks do not kill"
        );
        SimulationResult.DamageBandStatistics allTrials =
                result.damageBandStatistics().get(-1);
        assertTrue(result.representativeDamageTrials() != null,
                "median and mean representative trials are available");
        assertTrue(result.representativeDamageTrials().medianTrial() != null,
                "median representative trial is selected from observed trials");
        assertTrue(result.representativeDamageTrials().meanTrial() != null,
                "mean representative trial is selected from observed trials");
        assertClose(result.damageSampleTrials(), allTrials.trialCount(),
                "all-damage band includes every run passing the sleep gate");
        assertClose(result.damageSampleTrials(), allTrials.byDebuffCount().values().stream()
                .mapToLong(SimulationResult.DebuffCountStatistics::trialCount)
                .sum(), "all-damage debuff breakdown accounts for every eligible run");
    }

    private static void verifyMikageBuffsFenaxDamage() {
        double masteryMultiplier = 1.08 * 1.12 * 1.06;
        double plusFourBaseHit = 4.1 * 8_654 * (1.0 + 3.72);
        double plusZeroBaseHit = 4.1 * 7_101 * (1.0 + 3.63);
        Team mikageBuffedTeam = new Team(Map.of(
                TeamPosition.ALLY_ATTACKER,
                new Champion("Mikage", ChampionKind.MIKAGE, false, 1, 0, 0),
                TeamPosition.SLEEPER,
                new Champion("Hellhound", ChampionKind.HELLHOUND, false, 1, 0, 0),
                TeamPosition.DEBUFFER,
                new Champion("Mikage", ChampionKind.MIKAGE, false, 1, 0, 0),
                TeamPosition.DAMAGE_DEALER_ONE,
                new Champion(
                        "Fenax+4", ChampionKind.FENAX, true, 2, plusFourBaseHit, 0, 8_654, 3.72
                ),
                TeamPosition.DAMAGE_DEALER_TWO,
                new Champion(
                        "Fenax+0", ChampionKind.FENAX, true, 2, plusZeroBaseHit, 0, 7_101, 3.63
                )
        ));
        SimulationOptions noSleepGate = new SimulationOptions(
                false, false, 0, Set.of(), Set.of(), false, false, false, 4, false, 3
        );
        SimulationResult result = new Simulator(new DamageModel(0, 0)).run(
                1,
                mikageBuffedTeam,
                new GearSetup("Mikage buffs", Map.of()),
                noSleepGate,
                new FixedRandom(0.5)
        );

        assertClose(
                (4.1 * (8_654 * 1.5) * (1.0 + 3.72 + 0.30) * 4
                        + 4.1 * (7_101 * 1.5) * (1.0 + 3.63 + 0.30) * 4)
                        * masteryMultiplier,
                result.baseFenaxDamagePerCycle(),
                "Mikage ally-attacker buffs are included in the Fenax baseline"
        );
        assertClose(
                4.1 * (8_654 * 1.5) * (1.0 + 3.72 + 0.30) * 4 * masteryMultiplier,
                result.fenaxDamageByPosition()
                        .get(TeamPosition.DAMAGE_DEALER_ONE).baseDamagePerCycle(),
                "Mikage's Attack and Crit Damage buffs apply to Fenax+4"
        );
        assertClose(
                4.1 * (7_101 * 1.5) * (1.0 + 3.63 + 0.30) * 4 * masteryMultiplier,
                result.fenaxDamageByPosition()
                        .get(TeamPosition.DAMAGE_DEALER_TWO).baseDamagePerCycle(),
                "Mikage's Attack and Crit Damage buffs apply to Fenax+0"
        );
        assertClose(
                result.baseFenaxDamagePerCycle()
                        + 4.1 * (7_101 * 1.5) * (1.0 + 3.63 + 0.30)
                        * 2 * masteryMultiplier * 0.08,
                result.averageFenaxDamagePerCycle(),
                "Mikage buffs apply before hits, with Single Out triggered by remaining HP"
        );

        Team mithralaOnlyTeam = new Team(Map.of(
                TeamPosition.ALLY_ATTACKER,
                new Champion("Ally attacker", ChampionKind.DOROTHY, false, 1, 0, 0),
                TeamPosition.SLEEPER,
                new Champion("Hellhound", ChampionKind.HELLHOUND, false, 1, 0, 0),
                TeamPosition.DEBUFFER,
                new Champion("Mithrala", ChampionKind.MITHRALA, true, 2, 0, 0),
                TeamPosition.DAMAGE_DEALER_ONE,
                new Champion(
                        "Fenax+4", ChampionKind.FENAX, true, 2, plusFourBaseHit, 0, 8_654, 3.72
                ),
                TeamPosition.DAMAGE_DEALER_TWO,
                new Champion(
                        "Fenax+0", ChampionKind.FENAX, true, 2, plusZeroBaseHit, 0, 7_101, 3.63
                )
        ));
        SimulationResult mithralaNoBuffs = new Simulator(new DamageModel(0, 0)).run(
                1,
                mithralaOnlyTeam,
                new GearSetup("Mithrala does not buff Fenax", Map.of()),
                noSleepGate,
                new FixedRandom(0.5)
        );
        assertClose(
                (4.1 * 8_654 * (1.0 + 3.72) * 4 + 4.1 * 7_101 * (1.0 + 3.63) * 4)
                        * masteryMultiplier,
                mithralaNoBuffs.baseFenaxDamagePerCycle(),
                "Mithrala alone does not grant Mikage's Attack or Crit Damage buffs"
        );
    }

    private static Team team(Champion sleeper, Champion debuffer) {
        return new Team(Map.of(
                TeamPosition.ALLY_ATTACKER,
                new Champion("Ally attacker", ChampionKind.DOROTHY, false, 1, 1, 114),
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

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
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
