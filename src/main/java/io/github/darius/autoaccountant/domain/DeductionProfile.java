package io.github.darius.autoaccountant.domain;

public enum DeductionProfile {
    TRANSPORT(true),
    TAXI_VTC(true),
    DRIVING_SCHOOL(true),
    COMMERCIAL_AGENT(true),
    STUDIO_BASED(false),
    DESK_BASED(false),
    GENERIC(false);

    private final boolean vehicleExclusiveUse;

    DeductionProfile(boolean vehicleExclusiveUse) {
        this.vehicleExclusiveUse = vehicleExclusiveUse;
    }

    public boolean hasVehicleExclusiveUse() {
        return vehicleExclusiveUse;
    }
}
