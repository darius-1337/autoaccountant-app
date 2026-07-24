package io.github.darius.autoaccountant.domain;

public enum ClientType {
    PARTICULAR(false),
    BUSINESS(true),
    FOREIGN(false);

    private final boolean triggersRetention;

    ClientType(boolean triggersRetention) {
        this.triggersRetention = triggersRetention;
    }

    public boolean triggersRetention() {
        return triggersRetention;
    }
}
