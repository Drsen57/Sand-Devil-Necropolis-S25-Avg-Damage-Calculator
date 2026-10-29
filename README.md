# Sand Devil Necropolis S25 Damage Calculator

A Java Monte Carlo simulation for comparing champion and gear setups against
Sand Devil Necropolis S25.

## Requirements

- Java Development Kit (JDK) 17 or newer

## Build and run

From this directory, compile the source and run the calculator:

```powershell
javac --release 17 -d out (Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object FullName)
java -cp out com.sand_devil_calc.Main
```

To run the fixed-seed comparison against the original simulation:

```powershell
javac --release 17 -cp out -d out (Get-ChildItem -Recurse src/test/java -Filter *.java | ForEach-Object FullName)
java -cp out com.sand_devil_calc.LegacySimulationComparison
```

## Configure a simulation

Edit `Main.java`:

- Set a champion's `enabled` value to `false` to exclude them from every setup.
- Adjust hit count, expected damage per hit, debuff chances, or other champion
  settings in the `CHAMPIONS` list.
- Add or change entries in `GEAR_SETUPS` to compare gear assignments. Champions
  without an entry use `GearSet.NONE`.
- Change `SIMULATION_COUNT` or `RANDOM_SEED` to control the trial count and
  reproducibility. Each setup uses the same seed for a fair comparison.

Damage per hit defaults to `1.0`, so results are normalized comparisons until
you enter expected damage values for your champions. The simulator calculates
each cycle's damage from the final debuff count; it does not model turn order or
damage variation within a multi-hit skill.

The damage formula preserves the original calculator's two reduction factors:
`1 - max(1 - 0.15 * mitigationDebuffs, 0) * max(1 - 0.15 * totalDebuffs, 0)`.
The original code initialized damage reduction to 75% but replaced that value
before calculating damage, so the 75% did not affect its results. Toxic
debuffs can be configured per champion to count toward the first factor, as in
the original Hellhound/Mithrala setup. Feral increases damage by the configured
percentage for each debuff applied by a champion wearing the set.
