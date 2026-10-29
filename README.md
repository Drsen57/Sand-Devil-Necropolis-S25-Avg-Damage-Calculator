# Sand Devil Necropolis S25 Simulator

A Java Monte Carlo simulation for a configurable five-position team against
Sand Devil Necropolis S25. The model currently focuses on debuff application
and normalized potential damage; champion-specific damage stats are not yet
modeled.

## Requirements

- Java Development Kit (JDK) 17 or newer

## Build and run

From this directory, compile the source and run the calculator:

```powershell
javac --release 17 -d out (Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object FullName)
java -cp out com.sand_devil_calc.Main
```

To run the fixed-seed historical comparison against the corrected v1.1
simulation:

```powershell
javac --release 17 -cp out -d out (Get-ChildItem -Recurse src/test/java -Filter *.java | ForEach-Object FullName)
java -cp out com.sand_devil_calc.LegacySimulationComparison
java -cp out com.sand_devil_calc.TeamSimulationRegression
```

## Configure a simulation

Edit `Main.java`:

- `TEAM` defines the five ordered positions: ally attacker, sleeper, debuffer,
  and two damage dealers. Mikage is present but disabled; her 50% Attack buff,
  30% increased-damage buff, and ordered ally attack are deferred until
  champion damage is modeled. Enabling her currently raises an explicit error.
- Change `DEBUFFER_KIND` between `MITHRALA` and `ATHEL`.
- `GEAR_SETUPS` compares gear-set assignments by team position. A champion can
  wear only one gear-set type, but different champions may use different sets
  in the same setup. Daze is modeled only for Hellhound in the sleeper position.
  Sporeburst Rod is a separate relic and can be equipped alongside a gear set,
  but only an enabled poisoner can use it (Mithrala is the only poisoner
  currently modeled).
- `FAIL_RUN_WITHOUT_SLEEP` controls the first-debuff sleep gate. Turn it off
  when testing without the Sand Devil sleep requirement.
- `ASSUME_HELLHOUND_SLEEP_LANDS` skips Hellhound's sleep Accuracy/Resistance
  roll for legacy comparisons; when false, his skill sleep uses the configured
  accuracy and boss resistance.
- Add champion positions to `FEARSOME_PRESENCE_POSITIONS` to change Daze's
  sleep chance from 25% to 30%, and to `SNIPER_MASTERY_POSITIONS` to change
  those champions' Toxic chance from 75% to 80%.
- `HELLHOUND_HARVEST_DESPAIR_ENABLED` toggles Hellhound's Leech mastery proc.
  When enabled, a newly landed sleep (skill or Daze) triggers one unresistable
  Leech attempt. `HELLHOUND_HARVEST_DESPAIR_SNIPER_ENABLED` independently
  raises this proc chance from 60% to 65%, regardless of the champion positions
  in `SNIPER_MASTERY_POSITIONS`.
- Daze is modeled only for Hellhound in the sleeper position. Its sleep proc
  is gear-based and does not receive an Accuracy/Resistance check. Sleep is
  singleton across both sources.
- `MITHRALA_SPOREBURST_ROD_ENABLED` toggles the relic;
  its star level sets the extra-poison chance from 10% at 1 star to 35% at 6.
- `MITHRALA_SMITE_ENABLED` and `MITHRALA_SMITE_STARS` configure Smite. Stars
  2 and 4 use the same chances as 1 and 3, respectively.
- Adjust each champion's accuracy and `BOSS_RESISTANCE` to calculate the
  accuracy/resistance failure chance using the supplied spreadsheet formula.
- Change `SIMULATION_COUNT` or `RANDOM_SEED` to control the trial count and
  reproducibility. Each setup uses the same seed for a fair comparison.

The simulator processes one first-turn sequence only; it does not run an
additional team pass after an ally attack. Mikage's ally attack is not enabled
or included yet. Champion damage per hit defaults to `1.0`, so damage is
currently normalized. Sleep-failed runs are reported separately and excluded
from average damage, standard deviation, and debuff statistics. These
statistics are calculated only across successful runs. An Athel Weaken
landing increases modeled damage by 25%; Feral adds 5% for each debuff that
its wearer successfully applies, including Toxic and relic debuffs.

Every landed debuff contributes to one count in the boss formula:
`1 - 0.75 * max(1 - 0.15 * numberOfDebuffsLanded, 0)^2`.
The reduction factor is clamped at zero, so debuffs beyond seven cannot add
damage. Results include population standard-deviation ranges at one, two, and
three standard deviations from the average; these describe variation, not
minimum/maximum bounds or confidence intervals.

Sand Devil's default Resistance is 470 and the default champion Accuracy is
584 (114 higher). The configured formula determines the skill-debuff resist
chance. Boss HP (3.1 million) and Defense (3,587) are noted in `Main.java` but
not modeled yet.

The prior simulator and its champion/gear types now live under
`src/test/java/com/sand_devil_calc/legacy` as a fixed-seed historical oracle.
