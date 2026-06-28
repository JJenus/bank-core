package com.jjenus.bank.core.ports;

import com.jjenus.bank.core.accounts.AccountId;
import com.jjenus.bank.core.transfers.Transfer;
import com.jjenus.bank.core.transfers.TransferId;
import com.jjenus.bank.core.transfers.TransferStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Output port for transfer persistence.
 *
 * <p>Defined in the domain layer; implemented in the infrastructure layer.
 */
public interface TransferRepository {

    /** Persists a new transfer. */
    Transfer save(Transfer transfer);

    /** Updates an existing transfer (e.g., after status change). */
    Transfer update(Transfer transfer);

    /** Finds a transfer by its ID. */
    Optional<Transfer> findById(TransferId id);

    /** Finds a transfer by its ID, throwing if absent. */
    default Transfer getById(TransferId id) {
        return findById(id).orElseThrow(() ->
            new IllegalArgumentException("Transfer not found: " + id.value())
        );
    }

    /** Finds all transfers originating from a given account. */
    List<Transfer> findByFromAccountId(AccountId accountId);

    /** Finds all transfers received by a given account. */
    List<Transfer> findByToAccountId(AccountId accountId);

    /** Finds all transfers involving an account (either direction). */
    List<Transfer> findByAccountId(AccountId accountId);

    /** Finds all transfers with a given status. */
    List<Transfer> findByStatus(TransferStatus status);

    /** Finds all transfers for an account created within a time range. */
    List<Transfer> findByAccountIdAndDateRange(AccountId accountId, Instant from, Instant to);

    /** Checks if a transfer with the given reference already exists (idempotency check). */
    boolean existsByReference(String reference);

    /** Returns the total number of transfers. */
    long count();
}
