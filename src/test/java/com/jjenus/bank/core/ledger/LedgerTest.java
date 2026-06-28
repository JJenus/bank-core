package com.jjenus.bank.core.ledger;

import com.jjenus.bank.core.accounts.AccountId;
import com.jjenus.bank.core.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LedgerTest {
    private static final Currency USD = Currency.getInstance("USD");
    private Ledger ledger;
    private AccountId customerAccount;
    private AccountId cashAccount;
    private AccountId feeAccount;

    @BeforeEach
    void setUp() {
        ledger = new Ledger();
        customerAccount = AccountId.generate();
        cashAccount = AccountId.generate();
        feeAccount = AccountId.generate();
    }

    @Test
    @DisplayName("Post a deposit entry and verify balance")
    void post_depositEntry_updatesBalance() {
        LedgerEntry entry = LedgerEntry.forDeposit(
            LedgerEntryId.generate(), cashAccount, customerAccount,
            Money.of("500.00", USD), "DEP001", "TXN-001"
        );

        LedgerEvent.JournalEntryPosted event = ledger.post(entry);

        assertNotNull(event);
        assertEquals(entry, event.entry());

        // Customer's balance: credit side = +500
        Money balance = ledger.balanceOf(customerAccount, USD);
        assertEquals(0, balance.amount().compareTo(new BigDecimal("500.0000")));

        // Cash account: debit side = -500 (from perspective of cash account)
        Money cashBalance = ledger.balanceOf(cashAccount, USD);
        assertEquals(0, cashBalance.amount().compareTo(new BigDecimal("-500.0000")));
    }

    @Test
    @DisplayName("Post a transfer entry and verify both account balances")
    void post_transferEntry_updatesBothBalances() {
        // Setup: fund both accounts
        AccountId sender = AccountId.generate();
        AccountId receiver = AccountId.generate();

        ledger.post(LedgerEntry.forDeposit(
            LedgerEntryId.generate(), cashAccount, sender,
            Money.of("1000.00", USD), "DEP001", "TXN-001"
        ));
        ledger.post(LedgerEntry.forDeposit(
            LedgerEntryId.generate(), cashAccount, receiver,
            Money.of("500.00", USD), "DEP002", "TXN-002"
        ));

        // Transfer 300 from sender to receiver
        ledger.post(LedgerEntry.forTransfer(
            LedgerEntryId.generate(), sender, receiver,
            Money.of("300.00", USD), "TRF001", "TRF-001"
        ));

        assertEquals(0, ledger.balanceOf(sender, USD).amount().compareTo(new BigDecimal("700.0000")));
        assertEquals(0, ledger.balanceOf(receiver, USD).amount().compareTo(new BigDecimal("800.0000")));
    }

    @Test
    @DisplayName("Post a fee entry")
    void post_feeEntry_debitedFromCustomer() {
        // Fund customer first
        ledger.post(LedgerEntry.forDeposit(
            LedgerEntryId.generate(), cashAccount, customerAccount,
            Money.of("100.00", USD), "DEP001", "TXN-001"
        ));

        ledger.post(LedgerEntry.forFee(
            LedgerEntryId.generate(), customerAccount, feeAccount,
            Money.of("5.00", USD), "Transfer fee", "TRF-001"
        ));

        assertEquals(0, ledger.balanceOf(customerAccount, USD).amount()
            .compareTo(new BigDecimal("95.0000")));
        assertEquals(0, ledger.balanceOf(feeAccount, USD).amount()
            .compareTo(new BigDecimal("5.0000")));
    }

    @Test
    @DisplayName("Reverse a posted entry creates mirror entry")
    void reverse_postedEntry_createsReversal() {
        LedgerEntry original = LedgerEntry.forDeposit(
            LedgerEntryId.generate(), cashAccount, customerAccount,
            Money.of("200.00", USD), "DEP001", "TXN-001"
        );
        ledger.post(original);

        LedgerEvent.JournalEntryReversed event = ledger.reverse(original.id(), "Erroneous deposit");

        assertNotNull(event);
        assertEquals(original, event.originalEntry());
        assertEquals("Erroneous deposit", event.reason());

        // After reversal, customer balance should be back to zero
        Money balance = ledger.balanceOf(customerAccount, USD);
        assertEquals(0, balance.amount().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Reversing a non-existent entry throws exception")
    void reverse_nonExistentEntry_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            ledger.reverse(LedgerEntryId.generate(), "Not found")
        );
    }

    @Test
    @DisplayName("Reversing an already reversed entry throws exception")
    void reverse_alreadyReversed_throwsException() {
        LedgerEntry entry = LedgerEntry.forDeposit(
            LedgerEntryId.generate(), cashAccount, customerAccount,
            Money.of("100.00", USD), "DEP001", "TXN-001"
        );
        ledger.post(entry);
        ledger.reverse(entry.id(), "First reversal");

        assertThrows(IllegalArgumentException.class, () ->
            ledger.reverse(entry.id(), "Second reversal attempt")
        );
    }

    @Test
    @DisplayName("Duplicate reference and source rejected for idempotency")
    void post_duplicateReferenceAndSource_throwsException() {
        LedgerEntry first = LedgerEntry.forDeposit(
            LedgerEntryId.generate(), cashAccount, customerAccount,
            Money.of("100.00", USD), "DEP001", "TXN-001"
        );
        ledger.post(first);

        // Same reference, same sourceId, same accounts — should be rejected
        LedgerEntry duplicate = LedgerEntry.forDeposit(
            LedgerEntryId.generate(), cashAccount, customerAccount,
            Money.of("100.00", USD), "DEP001", "TXN-001"
        );

        assertThrows(IllegalArgumentException.class, () -> ledger.post(duplicate));
    }

    @Test
    @DisplayName("Balance as of a specific timestamp")
    void balanceOf_asOf_returnsCorrectBalance() throws InterruptedException {
        LedgerEntry early = LedgerEntry.forDeposit(
            LedgerEntryId.generate(), cashAccount, customerAccount,
            Money.of("300.00", USD), "DEP001", "TXN-001"
        );
        ledger.post(early);

        java.time.Instant checkpoint = java.time.Instant.now().plusMillis(10);
        Thread.sleep(20);  // Ensure next entry has a later timestamp

        // Post a second entry after the checkpoint
        LedgerEntry late = LedgerEntry.forDeposit(
            LedgerEntryId.generate(), cashAccount, customerAccount,
            Money.of("200.00", USD), "DEP002", "TXN-002"
        );
        ledger.post(late);

        // Balance at checkpoint should only include the first entry
        Money balanceAtCheckpoint = ledger.balanceOf(customerAccount, USD, checkpoint);
        assertEquals(0, balanceAtCheckpoint.amount().compareTo(new BigDecimal("300.0000")));

        // Current balance includes both
        Money currentBalance = ledger.balanceOf(customerAccount, USD);
        assertEquals(0, currentBalance.amount().compareTo(new BigDecimal("500.0000")));
    }

    @Test
    @DisplayName("entriesFor returns only entries involving the account")
    void entriesFor_returnsFilteredEntries() {
        AccountId other = AccountId.generate();

        ledger.post(LedgerEntry.forDeposit(
            LedgerEntryId.generate(), cashAccount, customerAccount,
            Money.of("100.00", USD), "DEP001", "TXN-001"
        ));
        ledger.post(LedgerEntry.forDeposit(
            LedgerEntryId.generate(), cashAccount, other,
            Money.of("200.00", USD), "DEP002", "TXN-002"
        ));
        ledger.post(LedgerEntry.forTransfer(
            LedgerEntryId.generate(), customerAccount, other,
            Money.of("50.00", USD), "TRF001", "TRF-001"
        ));

        List<LedgerEntry> customerEntries = ledger.entriesFor(customerAccount);

        assertEquals(2, customerEntries.size());  // deposit + transfer out
    }

    @Test
    @DisplayName("drainEvents clears pending events after first call")
    void drainEvents_clearsAfterDrain() {
        ledger.post(LedgerEntry.forDeposit(
            LedgerEntryId.generate(), cashAccount, customerAccount,
            Money.of("100.00", USD), "DEP001", "TXN-001"
        ));

        List<LedgerEvent> first = ledger.drainEvents();
        List<LedgerEvent> second = ledger.drainEvents();

        assertEquals(1, first.size());
        assertEquals(0, second.size());
    }

    @Test
    @DisplayName("LedgerEntry with same debit and credit account throws exception")
    void ledgerEntry_sameDebitCreditAccount_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new LedgerEntry(
                LedgerEntryId.generate(),
                customerAccount,
                customerAccount,  // Same as debit — illegal
                Money.of("100.00", USD),
                "Self transfer",
                "REF001",
                "SRC001",
                null
            )
        );
    }

    @Test
    @DisplayName("LedgerEntry with zero amount throws exception")
    void ledgerEntry_zeroAmount_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new LedgerEntry(
                LedgerEntryId.generate(),
                cashAccount,
                customerAccount,
                Money.zero(USD),  // Zero — illegal
                "Zero amount entry",
                "REF001",
                "SRC001",
                null
            )
        );
    }

    @Test
    @DisplayName("LedgerEntry netEffectOn throws for non-participant account")
    void ledgerEntry_netEffectOn_nonParticipant_throwsException() {
        AccountId outsider = AccountId.generate();
        LedgerEntry entry = LedgerEntry.forDeposit(
            LedgerEntryId.generate(), cashAccount, customerAccount,
            Money.of("100.00", USD), "DEP001", "TXN-001"
        );

        assertThrows(IllegalArgumentException.class, () -> entry.netEffectOn(outsider));
    }

    @Test
    @DisplayName("Reconstitute ledger from historical entries")
    void reconstitute_fromHistoricalEntries() {
        List<LedgerEntry> historical = List.of(
            LedgerEntry.forDeposit(
                LedgerEntryId.generate(), cashAccount, customerAccount,
                Money.of("500.00", USD), "DEP001", "TXN-001"
            ),
            LedgerEntry.forWithdrawal(
                LedgerEntryId.generate(), customerAccount, cashAccount,
                Money.of("100.00", USD), "WITH001", "TXN-002"
            )
        );

        Ledger reconstructed = Ledger.reconstitute(historical);

        assertEquals(2, reconstructed.size());
        assertEquals(0, reconstructed.balanceOf(customerAccount, USD).amount()
            .compareTo(new BigDecimal("400.0000")));
    }

    @Test
    @DisplayName("LedgerEntryId format validation")
    void ledgerEntryId_invalidFormat_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> LedgerEntryId.of("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> LedgerEntryId.of("JNL-too-short"));
        assertThrows(IllegalArgumentException.class, () -> LedgerEntryId.of(null));
    }

    @Test
    @DisplayName("LedgerEntryId generate produces valid format")
    void ledgerEntryId_generate_validFormat() {
        LedgerEntryId id = LedgerEntryId.generate();
        assertTrue(id.value().startsWith("JNL-"));
        assertEquals(16, id.value().length());
        assertTrue(id.value().substring(4).matches("[A-Z0-9]{12}"));
    }
}
