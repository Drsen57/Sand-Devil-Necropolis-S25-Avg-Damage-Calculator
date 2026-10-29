package com.sand_devil_calc.legacy;

import java.util.Map;

public record GearSetup(String name, Map<String, GearSet> gearByChampion) {
    public GearSetup {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Gear setup name must not be blank.");
        }
        gearByChampion = Map.copyOf(gearByChampion);
    }

    public GearSet gearFor(Champion champion) {
        return gearByChampion.getOrDefault(champion.name(), GearSet.NONE);
    }
}
