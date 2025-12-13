package com.jjenus.bank.core.accounts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class AccountStatusTest {

    @Test
    @DisplayName("Active account can transact")
    void activeAccount_canTransact() {
        assertTrue(AccountStatus.ACTIVE.canTransact());
        assertTrue(AccountStatus.ACTIVE.canDeposit());
        assertTrue(AccountStatus.ACTIVE.canWithdraw());
        assertFalse(AccountStatus.ACTIVE.isTerminal());
    }

    @Test
    @DisplayName("Frozen account cannot transact")
    void frozenAccount_cannotTransact() {
        assertFalse(AccountStatus.FROZEN.canTransact());
        assertFalse(AccountStatus.FROZEN.canDeposit());
        assertFalse(AccountStatus.FROZEN.canWithdraw());
        assertFalse(AccountStatus.FROZEN.isTerminal());
    }

    @Test
    @DisplayName("Closed account is terminal")
    void closedAccount_isTerminal() {
        assertFalse(AccountStatus.CLOSED.canTransact());
        assertFalse(AccountStatus.CLOSED.canDeposit());
        assertFalse(AccountStatus.CLOSED.canWithdraw());
        assertTrue(AccountStatus.CLOSED.isTerminal());
    }

    @Test
    @DisplayName("Suspended account cannot transact")
    void suspendedAccount_cannotTransact() {
        assertFalse(AccountStatus.SUSPENDED.canTransact());
        assertFalse(AccountStatus.SUSPENDED.canDeposit());
        assertFalse(AccountStatus.SUSPENDED.canWithdraw());
        assertFalse(AccountStatus.SUSPENDED.isTerminal());
    }

    @Test
    @DisplayName("Dormant account can deposit but not withdraw")
    void dormantAccount_canDepositNotWithdraw() {
        assertFalse(AccountStatus.DORMANT.canTransact());
        assertTrue(AccountStatus.DORMANT.canDeposit());
        assertFalse(AccountStatus.DORMANT.canWithdraw());
        assertFalse(AccountStatus.DORMANT.isTerminal());
    }

    @Test
    @DisplayName("Account status descriptions")
    void statusDescriptions() {
        assertNotNull(AccountStatus.ACTIVE.getDescription());
        assertNotNull(AccountStatus.FROZEN.getDescription());
        assertNotNull(AccountStatus.CLOSED.getDescription());
        assertNotNull(AccountStatus.SUSPENDED.getDescription());
        assertNotNull(AccountStatus.DORMANT.getDescription());

        assertFalse(AccountStatus.ACTIVE.getDescription().isEmpty());
        assertFalse(AccountStatus.CLOSED.getDescription().isEmpty());
    }

    @Test
    @DisplayName("Account status values")
    void values() {
        AccountStatus[] values = AccountStatus.values();

        assertEquals(5, values.length);
        assertArrayEquals(new AccountStatus[]{
            AccountStatus.ACTIVE,
            AccountStatus.FROZEN,
            AccountStatus.CLOSED,
            AccountStatus.SUSPENDED,
            AccountStatus.DORMANT
        }, values);
    }
}
