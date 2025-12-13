package com.jjenus.bank.core.shared;

import java.time.Instant;

public interface DomainEvent {

    Id<DomainEvent> eventId();
    Instant occurredOn();

    default boolean isBefore(Instant timestamp) {
        return occurredOn().isBefore(timestamp);
    }

    default boolean isAfter(Instant timestamp) {
        return occurredOn().isAfter(timestamp);
    }
}
