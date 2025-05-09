package OptimisticMoneyTransfer;

import java.math.BigDecimal;

public class OptimisticTransferService {

    /**
     * Перевод денег между счетами с использованием оптимистичной блокировки
     *
     * @param from   счет отправителя
     * @param to     счет получателя
     * @param amount сумма перевода
     * @return true если перевод успешен, false в противном случае
     */
    public boolean transferMoneyOptimistic(Account from, Account to, BigDecimal amount) {
        // 1. Проверка входных данных
        if (from == null || to == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        // 2. Настройка механизма повторных попыток
        int attempts = 0;
        final int MAX_ATTEMPTS = 10;
        final int MAX_ROLLBACK_ATTEMPTS = 5;
        final long BASE_DELAY_MS = 5;

        // 3. Основной цикл транзакции
        while (attempts < MAX_ATTEMPTS) {
            // Получаем текущие балансы счетов
            BigDecimal fromBalance = from.getBalance();
            BigDecimal toBalance = to.getBalance();

            // Проверяем достаточность средств
            if (fromBalance.compareTo(amount) < 0) {
                return false;
            }

            // Вычисляем новые балансы
            BigDecimal newFromBalance = fromBalance.subtract(amount);
            BigDecimal newToBalance = toBalance.add(amount);

            // 4. Пытаемся списать средства
            boolean transferFrom = from.compareAndSetBalance(fromBalance, newFromBalance);

            if (transferFrom) {
                // 5. Если списание успешно, пытаемся зачислить
                boolean transferTo = to.compareAndSetBalance(toBalance, newToBalance);

                if (transferTo) {
                    return true; // 6. Обе операции успешны - перевод завершен
                } else {
                    // 7. ОТКАТ: зачисление не удалось, возвращаем средства отправителю
                    boolean rollbackSuccess = false;
                    int rollbackAttempts = 0;

                    while (!rollbackSuccess && rollbackAttempts < MAX_ROLLBACK_ATTEMPTS) {
                        // Получаем актуальный баланс (он мог измениться)
                        BigDecimal currentFromBalance = from.getBalance();

                        // Пытаемся вернуть средства на счет отправителя
                        if (currentFromBalance.equals(newFromBalance)) {
                            // Если баланс не менялся после нашего списания
                            rollbackSuccess = from.compareAndSetBalance(newFromBalance, fromBalance);
                        } else {
                            // Если баланс уже изменился, просто добавляем сумму к текущему
                            rollbackSuccess = from.compareAndSetBalance(currentFromBalance,
                                    currentFromBalance.add(amount));
                        }

                        if (!rollbackSuccess) {
                            rollbackAttempts++;
                            try {
                                Thread.sleep(5 * rollbackAttempts); // Backoff для отката
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }

                    // Если откат не удался после всех попыток - логируем критическую ошибку
                    if (!rollbackSuccess) {
                        logTransactionError(from, to, amount);
                    }
                }
            }

            // 8. Подготовка к следующей попытке
            attempts++;

            // Экспоненциальный backoff между попытками
            try {
                long delay = Math.min(100, BASE_DELAY_MS * (1L << Math.min(attempts, 10)));
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false; // Исчерпали все попытки
    }

    // Вспомогательный метод для записи ошибок
    private void logTransactionError(Account from, Account to, BigDecimal amount) {
        // В реальной системе здесь должен быть мониторинг и система восстановления
        System.err.println("КРИТИЧЕСКАЯ ОШИБКА ТРАНЗАКЦИИ: не удалось откатить списание");
        System.err.println("Счет отправителя: " + from.getId() + ", баланс: " + from.getBalance());
        System.err.println("Счет получателя: " + to.getId() + ", баланс: " + to.getBalance());
        System.err.println("Сумма: " + amount);
    }
}
