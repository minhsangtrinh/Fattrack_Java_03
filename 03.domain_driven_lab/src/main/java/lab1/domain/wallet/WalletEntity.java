package lab1.domain.wallet;

import lab1.domain.exception.DomainException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Wallet aggregate root. All state changes go through behavior that enforces
 * the wallet's business invariants.
 */
public final class WalletEntity {
    private final UUID id;
    private BigDecimal balance;
    private WalletStatus status;

    public WalletEntity(UUID id, BigDecimal openingBalance) {
        this.id = Objects.requireNonNull(id, "id is required");
        if (openingBalance == null || openingBalance.signum() < 0) {
            throw new DomainException("Opening balance must be zero or greater");
        }
        this.balance = openingBalance;
        this.status = WalletStatus.ACTIVE;
    }

    public void withdrawMoney(BigDecimal amount) {
        if (status == WalletStatus.LOCKED) {
            throw new DomainException("Cannot withdraw from a locked wallet");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new DomainException("Withdrawal amount must be greater than zero");
        }
        if (balance.compareTo(amount) < 0) {
            throw new DomainException("Insufficient wallet balance");
        }
        balance = balance.subtract(amount);
    }

    public void lockWallet() {
        status = WalletStatus.LOCKED;
    }

    // Read-only accessors; there are deliberately no state setters.
    public UUID getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public WalletStatus getStatus() {
        return status;
    }
}