package com.sand_devil_calc;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record Team(Map<TeamPosition, Champion> members) {
    public Team {
        if (members == null) {
            throw new IllegalArgumentException("Team members are required.");
        }
        EnumMap<TeamPosition, Champion> copy = new EnumMap<>(TeamPosition.class);
        copy.putAll(members);
        for (TeamPosition position : TeamPosition.values()) {
            if (copy.get(position) == null) {
                throw new IllegalArgumentException("Missing team position: " + position);
            }
        }
        if (copy.size() != TeamPosition.values().length) {
            throw new IllegalArgumentException("A team must have exactly five positions.");
        }
        members = Map.copyOf(copy);
    }

    public Champion at(TeamPosition position) {
        return members.get(position);
    }

    public List<Champion> inTurnOrder() {
        return List.of(
                at(TeamPosition.ALLY_ATTACKER),
                at(TeamPosition.SLEEPER),
                at(TeamPosition.DEBUFFER),
                at(TeamPosition.DAMAGE_DEALER_ONE),
                at(TeamPosition.DAMAGE_DEALER_TWO)
        );
    }
}
