package com.jjenus.bank.core.ports;

import com.jjenus.bank.core.accounts.AccountEvent;
import com.jjenus.bank.core.accounts.AccountId;
import com.jjenus.bank.core.shared.DomainEvent;
import java.time.Instant;
import java.util.List;

/**
 * Output port for the event store — the append-only log of all domain events.
 *
 * <p>Defined in the domain layer; implemented in the infrastructure layer
 * (e.g., backed by PostgreSQL, EventStoreDB, or Kafka).
 *
 * <p>The event store is the source of truth in an event-sourced design.
 * Account state is always derived by replaying events from this store via
 * {@link com.jjenus.bank.core.accounts.AccountFactory#reconstituteFromEvents}.
 */
public interface EventStore {

    /**
     * Appends a single event to the store for the given aggregate.
     *
     * @param aggregateId the aggregate (e.g., AccountId) this event belongs to
     * @param event       the domain event to persist
     */
    void append(String aggregateId, DomainEvent event);

    /**
     * Appends multiple events atomically for the given aggregate.
     * All events are stored or none are (transactional).
     */
    void appendAll(String aggregateId, List<? extends DomainEvent> events);

    /**
     * Loads all events for an account aggregate, in the order they were appended.
     */
    List<AccountEvent> loadAccountEvents(AccountId accountId);

    /**
     * Loads events for an account aggregate after a specific version number.
     * Useful for optimistic concurrency and partial replays.
     *
     * @param accountId      the account whose events to load
     * @param afterVersion   the version to start loading from (exclusive)
     */
    List<AccountEvent> loadAccountEventsSince(AccountId accountId, long afterVersion);

    /**
     * Loads all events for an account that occurred before a given timestamp.
     * Enables point-in-time account reconstruction.
     */
    List<AccountEvent> loadAccountEventsAsOf(AccountId accountId, Instant asOf);

    /**
     * Returns the current version (event count) for an account aggregate.
     * Returns 0 if the account has no events.
     */
    long currentVersion(AccountId accountId);

    /**
     * Checks whether any events exist for the given aggregate ID.
     */
    boolean hasEvents(String aggregateId);
}
