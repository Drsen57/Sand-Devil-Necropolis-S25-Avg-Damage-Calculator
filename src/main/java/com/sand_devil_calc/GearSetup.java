package com.sand_devil_calc;

import java.util.Map;

public record GearSetup(String name, Map<TeamPosition, GearSet> gearByPosition) {
    public GearSetup {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Gear setup name must not be blank.");
        }
        if (gearByPosition == null) {
            throw new IllegalArgumentException("Gear assignments are required.");
        }
        gearByPosition = Map.copyOf(gearByPosition);

        for (Map.Entry<TeamPosition, GearSet> entry : gearByPosition.entrySet()) {
            GearSet gearSet = entry.getValue();
            if (gearSet == GearSet.DAZE && entry.getKey() != TeamPosition.SLEEPER) {
                throw new IllegalArgumentException("Daze is only configured for the sleeper position.");
            }
        }
    }

    public GearSet gearFor(TeamPosition position) {
        return gearByPosition.getOrDefault(position, GearSet.NONE);
    }
}
