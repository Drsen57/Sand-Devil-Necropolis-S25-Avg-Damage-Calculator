# Sand Devil Necropolis S25 Average Damage Calculator

A Java simulation that compares estimated damage outcomes for Toxic and Feral
builds in the Sand Devil Necropolis encounter.

## Requirements

- Java Development Kit (JDK) 17 or newer

## Build and run

From this directory, compile the source and run the calculator:

```text
javac -d out src/main/java/com/sand_devil_calc/Main.java
java -cp out com.sand_devil_calc.Main
```

The simulation runs 1,000,000 trials and prints average damage percentages and
the estimated probabilities of having 1 through 10 debuffs for each build.
Proc chances and damage calculations are defined in `Main.java`.
