package PessemisticMoneyTransfer;

import java.math.BigDecimal;

/**
 * Класс Account с правильной поддержкой многопоточности
 */
public class Account {
    private final long id;
    private BigDecimal balance;
    private final Object lock = new Object(); // Объект для блокировки

    public Account(long id, BigDecimal initialBalance) {
        this.id = id;
        this.balance = initialBalance;
    }

    public long getId() {
        return id;
    }

    public BigDecimal getBalance() {
        synchronized (lock) {
            return balance;
        }
    }

    public void withdraw(BigDecimal amount) {
        synchronized (lock) {
            balance = balance.subtract(amount);
        }
    }

    public void deposit(BigDecimal amount) {
        synchronized (lock) {
            balance = balance.add(amount);
        }
    }

    public Object getLock() {
        return lock;
    }
}

