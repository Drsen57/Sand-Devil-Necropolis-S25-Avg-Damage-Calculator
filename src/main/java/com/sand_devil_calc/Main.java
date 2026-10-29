package com.sand_devil_calc;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        System.out.println("\n\nSand Devil Calculator\n");
    
        ArrayList<Double> toxicDamageDealtArray = new ArrayList<Double>();
        ArrayList<Double> feralDamageDealtArray = new ArrayList<Double>();

        ArrayList<Double> oneDebuffArrayToxic = new ArrayList<Double>();
        ArrayList<Double> twoDebuffArrayToxic = new ArrayList<Double>();
        ArrayList<Double> threeDebuffArrayToxic = new ArrayList<Double>();
        ArrayList<Double> fourDebuffArrayToxic = new ArrayList<Double>();
        ArrayList<Double> fiveDebuffArrayToxic = new ArrayList<Double>();
        ArrayList<Double> sixDebuffArrayToxic = new ArrayList<Double>();
        ArrayList<Double> sevenDebuffArrayToxic = new ArrayList<Double>();
        ArrayList<Double> eightDebuffArrayToxic = new ArrayList<Double>();
        ArrayList<Double> nineDebuffArrayToxic = new ArrayList<Double>();
        ArrayList<Double> tenDebuffArrayToxic = new ArrayList<Double>();

        ArrayList<Double> oneDebuffArrayFeral = new ArrayList<Double>();
        ArrayList<Double> twoDebuffArrayFeral = new ArrayList<Double>();
        ArrayList<Double> threeDebuffArrayFeral = new ArrayList<Double>();
        ArrayList<Double> fourDebuffArrayFeral = new ArrayList<Double>();
        ArrayList<Double> fiveDebuffArrayFeral = new ArrayList<Double>();
        ArrayList<Double> sixDebuffArrayFeral = new ArrayList<Double>();
        ArrayList<Double> sevenDebuffArrayFeral = new ArrayList<Double>();
        ArrayList<Double> eightDebuffArrayFeral = new ArrayList<Double>();
        ArrayList<Double> nineDebuffArrayFeral = new ArrayList<Double>();
        ArrayList<Double> tenDebuffArrayFeral = new ArrayList<Double>();

        double numberOfSimulations = 1000000; // Number of simulations to run

        for(int simulationRun = 1; simulationRun <= numberOfSimulations; simulationRun++){
            double toxicSetChance = 0.80; // 80% chance to apply poison
            double mithralaSporeburstChance = 0.25; // 30% is my current build
            double hellhoundLeechChance = 0.60; // 60% chance to apply leech is believed

            double mithralaPoisonChance = 0.97; // 97% chance to apply poison
            double smiteChance = 0.30; // 30% chance to apply Smite is current build
            double smiteSuccessChance = 0.97; // 97% chance for Smite to land

            int hellhoundDebuffs = 1; // Start with 1 debuff from sleep
            int mithralaDebuffs = 0;
            int ifToxicDebuffs = 0;
            double damageReduction = 0.75; // Start with 75% damage reduction
            int mithralaSmiteApplied = 0; // 0 if Smite is not applied, 1 if it is applied

            if(Math.random() < hellhoundLeechChance) { // 65% chance to place leech debuff
                hellhoundDebuffs++;
            }
            if(Math.random() < toxicSetChance) { // 80% chance to place poison debuff
                hellhoundDebuffs++;
            }
            
            //Mithrala debuffing
            for(int i = 0; i < 2; i++) { // Mithrala hits twice
                if(Math.random() < mithralaPoisonChance) { // 97% chance to place poison debuff
                    mithralaDebuffs++;
                    if(Math.random() < mithralaSporeburstChance) { // 30% chance to place additional poison from Sporeburst Rod
                        mithralaDebuffs++;
                    }
                }

                if(Math.random() < smiteChance && Math.random() < smiteSuccessChance && mithralaSmiteApplied == 0) { // 30% chance to apply Smite at 97% success rate, only if it hasn't been applied yet
                    mithralaSmiteApplied = 1;
                    mithralaDebuffs++;
                }
                
                if(Math.random() < toxicSetChance) { // 80% chance to place poison debuff
                    ifToxicDebuffs++;
                }
            }
            
            damageReduction *= Math.max(1 - 0.15 * (hellhoundDebuffs + mithralaDebuffs), 0); // Apply debuff reduction to the starting 75% reduction

            feralDamageDealtArray.add((1 - damageReduction * Math.max(1 - 0.15 * (hellhoundDebuffs + mithralaDebuffs), 0)) * (1 + 0.05 * mithralaDebuffs)); // If feral, increase damage taken by 5% for each debuff
            toxicDamageDealtArray.add(1 - damageReduction * Math.max(1 - 0.15 * (hellhoundDebuffs + mithralaDebuffs + ifToxicDebuffs), 0)); // If toxic, just use the calculated damage reduction
            
            if(hellhoundDebuffs + mithralaDebuffs == 1) {
                oneDebuffArrayFeral.add(1.0);
            }
            else{
                oneDebuffArrayFeral.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs == 2) {
                twoDebuffArrayFeral.add(1.0);
            }
            else{
                twoDebuffArrayFeral.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs == 3) {
                threeDebuffArrayFeral.add(1.0);
            }
            else{
                threeDebuffArrayFeral.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs == 4) {
                fourDebuffArrayFeral.add(1.0);
            }
            else{
                fourDebuffArrayFeral.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs == 5) {
                fiveDebuffArrayFeral.add(1.0);
            }
            else{
                fiveDebuffArrayFeral.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs == 6) {
                sixDebuffArrayFeral.add(1.0);
            }
            else{
                sixDebuffArrayFeral.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs == 7) {
                sevenDebuffArrayFeral.add(1.0);
            }
            else{
                sevenDebuffArrayFeral.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs == 8) {
                eightDebuffArrayFeral.add(1.0);
            }
            else{
                eightDebuffArrayFeral.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs == 9) {
                nineDebuffArrayFeral.add(1.0);
            }
            else{
                nineDebuffArrayFeral.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs == 10) {
                tenDebuffArrayFeral.add(1.0);
            }
            else{
                tenDebuffArrayFeral.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs + ifToxicDebuffs == 1) {
                oneDebuffArrayToxic.add(1.0);
            }
            else{
                oneDebuffArrayToxic.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs + ifToxicDebuffs == 2) {
                twoDebuffArrayToxic.add(1.0);
            }
            else{
                twoDebuffArrayToxic.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs + ifToxicDebuffs == 3) {
                threeDebuffArrayToxic.add(1.0);
            }
            else{
                threeDebuffArrayToxic.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs + ifToxicDebuffs == 4) {
                fourDebuffArrayToxic.add(1.0);
            }
            else{
                fourDebuffArrayToxic.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs + ifToxicDebuffs == 5) {
                fiveDebuffArrayToxic.add(1.0);
            }
            else{
                fiveDebuffArrayToxic.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs + ifToxicDebuffs == 6) {
                sixDebuffArrayToxic.add(1.0);
            }
            else{
                sixDebuffArrayToxic.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs + ifToxicDebuffs == 7) {
                sevenDebuffArrayToxic.add(1.0);
            }
            else{
                sevenDebuffArrayToxic.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs + ifToxicDebuffs == 8) {
                eightDebuffArrayToxic.add(1.0);
            }
            else{
                eightDebuffArrayToxic.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs + ifToxicDebuffs == 9) {
                nineDebuffArrayToxic.add(1.0);
            }
            else{
                nineDebuffArrayToxic.add(0.0);
            }

            if(hellhoundDebuffs + mithralaDebuffs + ifToxicDebuffs == 10) {
                tenDebuffArrayToxic.add(1.0);
            }
            else{
                tenDebuffArrayToxic.add(0.0);
            }


            //The main boss mechanic is that it starts with a 75% damage reduction to all damage, which decreases multiplicatively. 
            // This can be represented with x=max(1−0.15n, 0) where n is the number of current debuffs. 
            // In order to fight the boss at all, Hellhound has to land a sleep debuff, 
            // so we will assume that is 100% chance for the sake of this calculator, 
            // so the boss will start with 1 debuff already. Now, when Hellhound attacks, 
            // he has a 65% chance to place a leech debuff from his Harvest Despair mastery. 
            // He also wears toxic gear, which has an 80% chance to place one poison debuff on hit. 
            // So, he can place either 1, 2, or 3 debuffs himself, depending on what procs. 
            // Then Mithrala comes in. She has a 97% chance to place a poison debuff on hit, and she hits two times. 
            // Whenever she successfully lands a poison on a hit, 
            // she has a 30% (or 35% chance, maybe 40% chance - would like to see if that affects things later) 
            // to place an additional poison from her relic Sporeburst Rod. 
            // She also has a singleton debuff she can place called Smite from her blessing, which has a 30% chance 
            // (or 60% or 100% - but 100% ignores the 97% chance I will talk about in a second, depending on level of mastery
            //  - would like to see how this affects things later) to apply on each hit, but the boss can only have one on them. 
            // If it is applied, it has a 97% chance to actually land. Now, she can wear either feral or toxic. 
            // Feral would increase the damage the boss takes by 5% for each debuff that she landed on it. 
            // Toxic would have an 80% chance to land a poison on each of her hits. Sporeburst Rod cannot trigger from this. 
            // So, she could have either a maximum of 25% increase damage from feral if all 5 debuffs land, or she can land 
            // a total of 7 debuffs without feral.


        }

        System.out.print("\nAverage damage dealt with toxic: " + (toxicDamageDealtArray.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");

        System.out.print("\nChance of 1 debuff with toxic: " + (oneDebuffArrayToxic.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        System.out.print("\nChance of 2 debuffs with toxic: " + (twoDebuffArrayToxic.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        System.out.print("\nChance of 3 debuffs with toxic: " + (threeDebuffArrayToxic.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        System.out.print("\nChance of 4 debuffs with toxic: " + (fourDebuffArrayToxic.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        System.out.print("\nChance of 5 debuffs with toxic: " + (fiveDebuffArrayToxic.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        System.out.print("\nChance of 6 debuffs with toxic: " + (sixDebuffArrayToxic.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        System.out.print("\nChance of 7 debuffs with toxic: " + (sevenDebuffArrayToxic.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        System.out.print("\nChance of 8 debuffs with toxic: " + (eightDebuffArrayToxic.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        System.out.print("\nChance of 9 debuffs with toxic: " + (nineDebuffArrayToxic.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        System.out.print("\nChance of 10 debuffs with toxic: " + (tenDebuffArrayToxic.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");


        System.out.print("\n\nAverage damage dealt with feral: " + (feralDamageDealtArray.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        
        System.out.print("\nChance of 1 debuff with feral: " + (oneDebuffArrayFeral.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        System.out.print("\nChance of 2 debuffs with feral: " + (twoDebuffArrayFeral.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        System.out.print("\nChance of 3 debuffs with feral: " + (threeDebuffArrayFeral.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        System.out.print("\nChance of 4 debuffs with feral: " + (fourDebuffArrayFeral.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        System.out.print("\nChance of 5 debuffs with feral: " + (fiveDebuffArrayFeral.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        System.out.print("\nChance of 6 debuffs with feral: " + (sixDebuffArrayFeral.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        System.out.print("\nChance of 7 debuffs with feral: " + (sevenDebuffArrayFeral.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        System.out.print("\nChance of 8 debuffs with feral: " + (eightDebuffArrayFeral.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        System.out.print("\nChance of 9 debuffs with feral: " + (nineDebuffArrayFeral.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");
        System.out.print("\nChance of 10 debuffs with feral: " + (tenDebuffArrayFeral.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) + "%");

        System.out.println();
        }
}
