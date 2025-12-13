# Banking Core

A pure Java banking core domain library implementing DDD, event sourcing readiness, and clean architecture.

## Features

- **Pure Domain**: No frameworks, no dependencies
- **Immutable Models**: Thread-safe by design
- **Event Sourcing Ready**: Easy to add event store
- **Type Safety**: Strong typing with records
- **Banking Operations**: Accounts, Transactions, Transfers
- **Multi-Currency**: Built-in Money pattern
- **Validation**: Comprehensive business rules

## Modules

### Shared Kernel
- `Id`: Generic identifier with validation
- `Money`: Immutable monetary value with currency
- `Result`: Functional error handling
- `DomainEvent`: Base event interface

### Accounts
- `Account`: Core account entity with balance and status
- `AccountCommand`: Commands for account operations
- `AccountEvent`: Events for account state changes
- `AccountFactory`: Factory for creating/reconstituting accounts

### Transactions
- `Transaction`: Record of financial transactions
- `TransactionType`: Enumeration of transaction types

### Transfers
- `Transfer`: Atomic money transfer between accounts
- `TransferService`: Service for executing transfers

## Usage

```java
// Create accounts
Currency USD = Currency.getInstance("USD");
AccountId acc1 = AccountId.generate();
Account account1 = Account.create(acc1, "John Doe", USD);

// Deposit money
Money deposit = Money.of("1000.00", USD);
Account updated = account1.deposit(deposit);

// Execute transfer
TransferId transferId = TransferId.generate();
TransferCommand.InitiateTransfer cmd = TransferCommand.InitiateTransfer.now(
    transferId,
    fromAccountId,
    toAccountId,
    Money.of("500.00", USD),
    "Payment",
    "REF-001"
);

Result<TransferExecutionResult> result = TransferService.executeTransfer(
    fromAccount,
    toAccount,
    cmd
);
