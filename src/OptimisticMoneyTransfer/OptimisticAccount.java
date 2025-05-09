package OptimisticMoneyTransfer;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public class OptimisticAccount {

    private final String id;
    private final AtomicReference<BigDecimal> balance;

    /**
     * Создает новый счет с указанным идентификатором и начальным балансом
     *
     * @param id             идентификатор счета
     * @param initialBalance начальный баланс
     */
    public OptimisticAccount(String id, BigDecimal initialBalance) {
        this.id = id;
        this.balance = new AtomicReference<>(initialBalance);
    }

    /**
     * Возвращает идентификатор счета
     *
     * @return идентификатор счета
     */
    public String getId() {
        return id;
    }

    /**
     * Возвращает текущий баланс счета
     *
     * @return текущий баланс
     */
    public BigDecimal getBalance() {
        return balance.get();
    }

    /**
     * Атомарно изменяет баланс счета, если его текущее значение
     * соответствует ожидаемому.
     *
     * @param expectedBalance ожидаемый баланс
     * @param newBalance      новый баланс
     * @return true, если операция выполнена успешно; false, если текущий
     * баланс отличается от ожидаемого
     */
    public boolean compareAndSetBalance(BigDecimal expectedBalance, BigDecimal newBalance) {
        return balance.compareAndSet(expectedBalance, newBalance);
    }

    /**
     * Пополняет счет на указанную сумму.
     * Не гарантирует атомарность - для параллельных операций используйте
     * compareAndSetBalance.
     *
     * @param amount сумма для пополнения
     * @throws IllegalArgumentException если сумма отрицательная или равна нулю
     */
    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма пополнения должна быть положительной");
        }

        boolean updated = false;
        while (!updated) {
            BigDecimal currentBalance = balance.get();
            updated = balance.compareAndSet(currentBalance, currentBalance.add(amount));
        }
    }
}

