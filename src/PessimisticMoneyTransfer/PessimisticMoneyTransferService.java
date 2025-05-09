package PessimisticMoneyTransfer;

import java.math.BigDecimal;

/**
 * Реализация пессимистичного подхода к переводу денег
 */
public class PessimisticMoneyTransferService {
    /**
     * Выполняет перевод средств с использованием пессимистичной блокировки.
     * Предотвращает взаимоблокировки путем упорядочивания блокировок.
     * Оптимизирован для ранней проверки баланса.
     *
     * @param from   счет отправителя
     * @param to     счет получателя
     * @param amount сумма перевода
     * @return true если перевод выполнен успешно, false в случае ошибки
     */
    public static boolean transferMoney(PessimisticAccount from, PessimisticAccount to, BigDecimal amount) {
        // Защита от некорректных входных данных
        if (from == null || to == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        // Предотвращение deadlock через упорядочивание блокировок
        boolean fromFirst = from.getId() < to.getId();
        final Object firstLock = fromFirst ? from.getLock() : to.getLock();
        final Object secondLock = fromFirst ? to.getLock() : from.getLock();

        synchronized (firstLock) {
            // Если первой блокируем счет отправителя, сразу проверяем баланс
            if (fromFirst && from.getBalance().compareTo(amount) < 0) {
                return false; // Недостаточно средств, быстро выходим
            }

            synchronized (secondLock) {
                // Если первой была блокировка получателя, теперь проверяем баланс отправителя
                if (!fromFirst && from.getBalance().compareTo(amount) < 0) {
                    return false;
                }

                // Выполняем перевод
                from.withdraw(amount);
                to.deposit(amount);
                return true;
            }
        }
    }
}
