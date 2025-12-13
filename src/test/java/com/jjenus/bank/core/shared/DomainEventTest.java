package com.jjenus.bank.core.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class DomainEventTest {

    // Test implementation for testing DomainEvent interface
    record TestEvent(Id<DomainEvent> eventId, Instant occurredOn) implements DomainEvent {}

    @Test
    @DisplayName("DomainEvent creation")
    void createDomainEvent() {
        Id<DomainEvent> eventId = Id.random();
        Instant occurredOn = Instant.now();

        TestEvent event = new TestEvent(eventId, occurredOn);

        assertEquals(eventId, event.eventId());
        assertEquals(occurredOn, event.occurredOn());
    }

    @Test
    @DisplayName("DomainEvent isBefore")
    void isBefore() {
        Instant earlier = Instant.parse("2024-01-01T00:00:00Z");
        Instant later = Instant.parse("2024-01-02T00:00:00Z");

        TestEvent event = new TestEvent(Id.random(), earlier);

        assertTrue(event.isBefore(later));
        assertFalse(event.isBefore(earlier));
        assertFalse(event.isBefore(earlier.minusSeconds(1)));
    }

    @Test
    @DisplayName("DomainEvent isAfter")
    void isAfter() {
        Instant earlier = Instant.parse("2024-01-01T00:00:00Z");
        Instant later = Instant.parse("2024-01-02T00:00:00Z");

        TestEvent event = new TestEvent(Id.random(), later);

        assertTrue(event.isAfter(earlier));
        assertFalse(event.isAfter(later));
        assertFalse(event.isAfter(later.plusSeconds(1)));
    }

    @Test
    @DisplayName("Id creation")
    void idCreation() {
        String value = "test-id-123";
        Id<String> id = Id.of(value);

        assertEquals(value, id.value());
    }

    @Test
    @DisplayName("Id with null value throws exception")
    void idWithNullValue_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> Id.of(null));
    }

    @Test
    @DisplayName("Id with blank value throws exception")
    void idWithBlankValue_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> Id.of(""));
        assertThrows(IllegalArgumentException.class, () -> Id.of("   "));
    }

    @Test
    @DisplayName("Id random generation")
    void idRandom() {
        Id<String> id1 = Id.random();
        Id<String> id2 = Id.random();

        assertNotNull(id1.value());
        assertNotNull(id2.value());
        assertNotEquals(id1.value(), id2.value());
        assertTrue(id1.value().length() > 0);
    }
}
