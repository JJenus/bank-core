package com.jjenus.bank.core.ports;

import com.jjenus.bank.core.accounts.Account;
import com.jjenus.bank.core.accounts.AccountId;
import java.util.List;
import java.util.Optional;

/**
 * Output port (secondary port) for account persistence.
 *
 * <p>This interface is defined in the domain layer and implemented in the infrastructure
 * layer (e.g., a JPA repository or event store adapter). Domain services depend only
 * on this interface, never on infrastructure types like {@code JpaRepository} or
 * {@code EntityManager}.
 *
 * <p>In a Spring Boot application, the implementation would be annotated with
 * {@code @Repository} and injected via constructor injection into application services.
 */
public interface AccountRepository {

    /**
     * Persists a new account. Throws if an account with the same ID already exists.
     */
    Account save(Account account);

    /**
     * Finds an account by its ID.
     */
    Optional<Account> findById(AccountId id);

    /**
     * Finds an account by its ID, throwing if not present.
     *
     * @throws IllegalArgumentException if no account with the given ID exists
     */
    default Account getById(AccountId id) {
        return findById(id).orElseThrow(() ->
            new IllegalArgumentException("Account not found: " + id.value())
        );
    }

    /**
     * Finds all accounts belonging to a customer (by customerId / ownerName).
     */
    List<Account> findByCustomerId(String customerId);

    /**
     * Checks whether an account with the given ID exists.
     */
    boolean existsById(AccountId id);

    /**
     * Updates an existing account. Throws if no account with the given ID exists.
     * Implementations should enforce optimistic locking via {@code Account.version()}.
     */
    Account update(Account account);

    /**
     * Returns the total number of accounts in the repository.
     */
    long count();
}
