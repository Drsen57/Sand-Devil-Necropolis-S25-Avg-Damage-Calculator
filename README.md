# Sand Devil Necropolis S25 Simulator

A Java Monte Carlo simulation for a configurable five-position team against
Sand Devil Necropolis S25. The model currently focuses on debuff application
and Fenax damage, with configurable champion options, gear, masteries, and
relic effects.

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

- `BUILD_SETUPS` defines the Fenax configuration to simulate. Each build
  specifies Crit Damage, Quickstrike and Dungeon Delver gemstones, and whether
  that Fenax has Zeal.
- `createTeam` defines the five ordered positions: ally attacker, sleeper,
  debuffer, and two damage dealers. When Mikage occupies the ally-attacker
  position, her 50% Attack and +30 percentage-point Crit Damage buffs apply to
  Fenax; her ally attack damage is not modeled.
- Change `DEBUFFER_KIND` between `MITHRALA`, `DOROTHY`, and
  `WYSTERI_VINEGUARD`. Athel is currently excluded from selectable
  configurations because weak-affinity attacks can fail. Dorothy attacks once
  and can apply a resistable Weaken; her Smite level is five stars by default.
  Wysteri attacks twice and can apply one singleton, resistable Weaken per
  first turn. `WYSTERI_WEAKEN_RUNES` adds 5% chance per rune (0-2), stacking
  with Sniper's 80% base chance; one rune and Sniper yield 85%.
- Potential future debuffer options to evaluate: Nethril, Esme the Dancer,
  Basim Ibn Ishaq, Cruetraxa, Karato Foxhunter, Turvold, and Zinogre
  Blademaster. These champions are not implemented as selectable debuffers yet.
- Future damage-model work: calculate damage for Mikage, Hellhound, and
  Mithrala, whose damage is currently omitted from team totals.
- Future team comparisons: replace Mikage with Tin Man, and replace Mithrala
  with Esme the Dancer.
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
- `DEBUFFER_SMITE_ENABLED` and `DEBUFFER_SMITE_STARS` configure Smite for
  Mithrala or Dorothy. Wysteri does not have Smite. Dorothy defaults to five
  stars; Mithrala defaults to three. Stars 2 and 4 use the same chances as 1
  and 3, respectively.
- Adjust each champion's accuracy and `BOSS_RESISTANCE` to calculate the
  accuracy/resistance failure chance using the supplied spreadsheet formula.
- Change `SIMULATION_COUNT` or `RANDOM_SEED` to control the trial count and
  reproducibility. Each setup uses the same seed for a fair comparison.

The simulator models the Fenaxes' combined ally attack on turn one. If the
boss survives, turn two is the boss waking from Sleep; Fenax+4 attacks alone
on turn three, followed by Fenax+0 alone on turn four if needed. No attack or
win is recorded on turn two. Sleep and Leech expire during the boss's turn;
other turn-one debuffs retain one turn and remain active for both Fenax attacks
on turns three and four because the boss does not take another turn between
them. Each Fenax hit uses the debuffs still active at that exact strike.
Debuff averages and distributions always describe the first turn only. Damage
is modeled only for Fenax. The current restored setup is Fenax+4 at 8,974
Attack and 375% Crit Damage, with two Quickstrike gemstones, one Dungeon
Delver gemstone, and no Zeal; Fenax+0 at 7,184 Attack and 366% Crit Damage,
with one Quickstrike gemstone, two Dungeon Delver gemstones, and Zeal.
Both attack twice with an A1 multiplier of 4.1. Each hit independently varies
by +/-10% and has a 30% chance to deal an additional 30% damage. Both Fenaxes
are assumed to have 100% Crit Rate and strong affinity. Fenax damage and the debuff-derived damage
multiplier are reported independently and are never combined in output.
When Mikage is assigned to the ally-attacker position, she applies Increase
ATK (+50%) and Increase Crit Damage (+30 percentage points) to the team before
the Fenaxes' combined turn-one ally attack. Both Fenaxes' ally-attack hits use
the buffs. Mikage's own ally-attack damage is still not modeled.
Fenax strikes also receive multiplicative 8% Ruthless Ambush, 12% Opportunist,
and 6% Bring It Down bonuses. Single Out adds another 8% only when the boss is
below 40% HP at the start of that strike. Fenax strikes are simulated in order,
updating the boss's remaining HP after each hit, using exactly 3,082,910
starting HP. Damage statistics record the full damage from the killing hit,
including overkill, while remaining boss HP is clamped to zero and the run
stops on the kill. The report also runs a same-seed comparison that caps the
killing hit's recorded damage at the boss's remaining HP.
Each build's gemstone assignment is listed above. Each gemstone independently
multiplies attack damage by 1.10. Quickstrike applies on turn one only, and
boosts both hits of each Fenax's combined ally attack; it does not apply on
turns three or four. Dungeon Delver applies to every hit. Zeal multiplies that
Fenax's damage strikes by 1.12. Both Fenaxes' booked skills add a further
multiplicative 20% damage bonus on every strike.
Fenax averages include affinity-failed runs as zero damage, but exclude
sleep-failed runs. Sleep-resist losses are counted separately from other sleep
failures. Kill counts are reported by turn (1, 3, or 4), along with
insufficient-damage losses. Debuff statistics and the separate debuff
multiplier are calculated only across completed runs and include first-turn
debuffs only.
For runs still alive after turn three, the report gives the worst turn-three
shortfall: remaining boss HP as a percentage of damage dealt through turn
three. It reports the worst final loss the same way after turn four. For
example, if a run dealt 1,000,000 damage and the boss had 1,500,000 HP left,
the shortfall is 150%: the run needed another 150% of the damage it had dealt.
The turn-three shortfall is measured before the turn-four attack, so it is not
necessarily a final loss.
Runs where Athel fails a weak-affinity
attack (35% chance on each hit) end immediately and contribute zero Fenax
damage, but are excluded from debuff statistics.
The example team uses Hellhound and Mithrala in Toxic, with Hellhound's sleep
checked against Accuracy and Resistance. The console report includes
sleep-resist losses, kill counts by turn, insufficient-damage losses, and
worst turn-three and turn-four damage shortfalls. It also reports these
outcomes overall and for actual trials whose total Fenax damage falls within
cumulative +/-1, +/-2, and +/-3 standard deviations of the mean. These bands
overlap; each includes trials from the narrower bands. Before the bands, the
report shows the median-damage observed trial and the observed trial whose
damage is closest to the mean. A final all-damage band includes every trial
that passed the sleep gate. Sleep failures are not
included in damage bands, and affinity failures are counted separately inside
each band. Each band also reports the average extra turn-one damage required
for a turn-one kill, as a percentage of the turn-one damage dealt in each
eligible trial. For the worst turn-three shortfall and worst turn-four damage
loss, the report also gives total debuffs landed and how many were still active
at that checkpoint. Each standard-deviation band is further broken out by
debuffs landed, in ascending debuff-count order, with the trials and outcomes
for each count.
Damage results show standard-deviation ranges for the combined Fenax damage and
for each Fenax independently. Fenax damage values below one million are shown
in thousands with three significant figures; values over one million are shown
in millions with four significant figures.

The separately reported debuff-derived multiplier includes the first-turn
boss damage-reduction formula, Weaken's 25% damage bonus, and Feral's 5% per
debuff applied by its wearer (including Toxic and relic debuffs). It is a
summary statistic, not an extra flat damage amount. Actual Fenax hits apply the
boss damage-reduction formula using the active debuff count at each strike;
they do not use the aggregate report multiplier or add Weaken/Feral bonuses.

Every landed debuff contributes to one count in the boss formula:
`1 - 0.75 * max(1 - 0.15 * numberOfDebuffsLanded, 0)^2`.
The reduction factor is clamped at zero, so debuffs beyond seven cannot add
damage. Results include population standard-deviation ranges at one, two, and
three standard deviations from the average; these describe variation, not
minimum/maximum bounds or confidence intervals.

Sand Devil's default Resistance is 470 and the default champion Accuracy is
584 (114 higher). The configured formula determines the skill-debuff resist
chance. Boss Defense (3,587) is not modeled yet.

The prior simulator and its champion/gear types now live under
`src/test/java/com/sand_devil_calc/legacy` as a fixed-seed historical oracle.
