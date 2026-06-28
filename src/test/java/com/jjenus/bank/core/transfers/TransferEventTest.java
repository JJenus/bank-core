package com.jjenus.bank.core.transfers;

import com.jjenus.bank.core.accounts.AccountId;
import com.jjenus.bank.core.shared.Money;
import com.jjenus.bank.core.transactions.TransactionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Currency;
import static org.junit.jupiter.api.Assertions.*;

class TransferEventTest {
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    @DisplayName("TransferInitiated event carries all fields")
    void transferInitiated_hasAllFields() {
        TransferId id = TransferId.generate();
        AccountId from = AccountId.generate();
        AccountId to = AccountId.generate();
        Money amount = Money.of("500.00", USD);

        TransferEvent.TransferInitiated event = TransferEvent.transferInitiated(
            id, from, to, amount, "INV001"
        );

        assertNotNull(event.eventId());
        assertNotNull(event.occurredOn());
        assertEquals(id, event.transferId());
        assertEquals(from, event.fromAccountId());
        assertEquals(to, event.toAccountId());
        assertEquals(amount, event.amount());
        assertEquals("INV001", event.reference());
    }

    @Test
    @DisplayName("TransferDebited event carries transaction ID")
    void transferDebited_hasTransactionId() {
        TransferId id = TransferId.generate();
        TransactionId txId = TransactionId.generate();

        TransferEvent.TransferDebited event = TransferEvent.transferDebited(
            id, AccountId.generate(), Money.of("100.00", USD), txId
        );

        assertEquals(txId, event.debitTransactionId());
        assertEquals(id, event.transferId());
    }

    @Test
    @DisplayName("TransferCredited event carries transaction ID")
    void transferCredited_hasTransactionId() {
        TransferId id = TransferId.generate();
        TransactionId txId = TransactionId.generate();

        TransferEvent.TransferCredited event = TransferEvent.transferCredited(
            id, AccountId.generate(), Money.of("100.00", USD), txId
        );

        assertEquals(txId, event.creditTransactionId());
    }

    @Test
    @DisplayName("TransferCompleted event has from and to account IDs")
    void transferCompleted_hasFromAndTo() {
        AccountId from = AccountId.generate();
        AccountId to = AccountId.generate();

        TransferEvent.TransferCompleted event = TransferEvent.transferCompleted(
            TransferId.generate(), from, to, Money.of("300.00", USD)
        );

        assertEquals(from, event.fromAccountId());
        assertEquals(to, event.toAccountId());
    }

    @Test
    @DisplayName("TransferFailed event carries reason")
    void transferFailed_hasReason() {
        TransferEvent.TransferFailed event = TransferEvent.transferFailed(
            TransferId.generate(), "Insufficient funds"
        );

        assertEquals("Insufficient funds", event.reason());
        assertNotNull(event.occurredOn());
    }

    @Test
    @DisplayName("TransferReversed event carries both reversal transaction IDs")
    void transferReversed_hasBothTransactionIds() {
        TransactionId debitId = TransactionId.generate();
        TransactionId creditId = TransactionId.generate();

        TransferEvent.TransferReversed event = TransferEvent.transferReversed(
            TransferId.generate(),
            AccountId.generate(),
            AccountId.generate(),
            Money.of("200.00", USD),
            "Wrong amount",
            debitId,
            creditId
        );

        assertEquals(debitId, event.reversalDebitTransactionId());
        assertEquals(creditId, event.reversalCreditTransactionId());
        assertEquals("Wrong amount", event.reason());
    }

    @Test
    @DisplayName("TransferCancelled event carries reason")
    void transferCancelled_hasReason() {
        TransferEvent.TransferCancelled event = TransferEvent.transferCancelled(
            TransferId.generate(), "Customer request"
        );

        assertEquals("Customer request", event.reason());
    }

    @Test
    @DisplayName("All events implement DomainEvent interface")
    void allEvents_implementDomainEvent() {
        TransferId id = TransferId.generate();
        AccountId from = AccountId.generate();
        AccountId to = AccountId.generate();
        Money amount = Money.of("100.00", USD);
        TransactionId txId = TransactionId.generate();

        assertInstanceOf(com.jjenus.bank.core.shared.DomainEvent.class,
            TransferEvent.transferInitiated(id, from, to, amount, "REF"));
        assertInstanceOf(com.jjenus.bank.core.shared.DomainEvent.class,
            TransferEvent.transferDebited(id, from, amount, txId));
        assertInstanceOf(com.jjenus.bank.core.shared.DomainEvent.class,
            TransferEvent.transferCredited(id, to, amount, txId));
        assertInstanceOf(com.jjenus.bank.core.shared.DomainEvent.class,
            TransferEvent.transferCompleted(id, from, to, amount));
        assertInstanceOf(com.jjenus.bank.core.shared.DomainEvent.class,
            TransferEvent.transferFailed(id, "reason"));
        assertInstanceOf(com.jjenus.bank.core.shared.DomainEvent.class,
            TransferEvent.transferCancelled(id, "reason"));
    }

    @Test
    @DisplayName("executeTransfer emits 4 typed domain events")
    void executeTransfer_emits4TypedEvents() {
        AccountId fromId = AccountId.generate();
        AccountId toId = AccountId.generate();
        Account from = com.jjenus.bank.core.accounts.Account.create(fromId, "Sender", java.util.Currency.getInstance("USD"))
            .deposit(Money.of("1000.00", USD));
        Account to = com.jjenus.bank.core.accounts.Account.create(toId, "Receiver", java.util.Currency.getInstance("USD"));

        TransferCommand.InitiateTransfer cmd = TransferCommand.InitiateTransfer.now(
            TransferId.generate(), fromId, toId, Money.of("250.00", USD), "Payment", "INV001"
        );

        com.jjenus.bank.core.shared.Result<TransferService.TransferExecutionResult> result =
            TransferService.executeTransfer(from, to, cmd);

        assertTrue(result.isSuccess());
        var events = result.getOrThrow().domainEvents();
        assertEquals(4, events.size());
        assertInstanceOf(TransferEvent.TransferInitiated.class, events.get(0));
        assertInstanceOf(TransferEvent.TransferDebited.class, events.get(1));
        assertInstanceOf(TransferEvent.TransferCredited.class, events.get(2));
        assertInstanceOf(TransferEvent.TransferCompleted.class, events.get(3));
    }

    @Test
    @DisplayName("reverseTransfer emits 1 typed TransferReversed event")
    void reverseTransfer_emits1TypedEvent() {
        AccountId fromId = AccountId.generate();
        AccountId toId = AccountId.generate();
        Account from = com.jjenus.bank.core.accounts.Account.create(fromId, "Sender", java.util.Currency.getInstance("USD"))
            .deposit(Money.of("1000.00", USD));
        Account to = com.jjenus.bank.core.accounts.Account.create(toId, "Receiver", java.util.Currency.getInstance("USD"))
            .deposit(Money.of("300.00", USD));

        Transfer transfer = Transfer.initiate(
            TransferId.generate(), fromId, toId, Money.of("200.00", USD), "Payment", "INV002"
        ).markProcessing(null).complete(null);

        com.jjenus.bank.core.shared.Result<TransferService.ReversalResult> result =
            TransferService.reverseTransfer(transfer, to, from, "Wrong amount");

        assertTrue(result.isSuccess());
        var events = result.getOrThrow().domainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(TransferEvent.TransferReversed.class, events.get(0));
    }
}
