package PessimisticMoneyTransfer;

import java.math.BigDecimal;

/**
 * Класс Account с правильной поддержкой многопоточности
 * Используется только в PessimisticMoneyTransferService.
 * Требует внешней синхронизации через getLock().
 */
public class PessimisticAccount {
    private final long id;
    private BigDecimal balance;
    private final Object lock = new Object(); // Объект для блокировки

    public PessimisticAccount(long id, BigDecimal initialBalance) {
        this.id = id;
        this.balance = initialBalance;
    }

    public long getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void withdraw(BigDecimal amount) {
        balance = balance.subtract(amount);
    }

    public void deposit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public Object getLock() {
        return lock;
    }
}

