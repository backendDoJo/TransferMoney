package PessemisticMoneyTransfer;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PessimisticTransferDemo {
    public static void main(String[] args) {
        System.out.println("=== Демонстрация денежных переводов с пессимистичной блокировкой ===");

        // Создаем счета
        Account alice = new Account(1, new BigDecimal("1000.00"));
        Account bob = new Account(2, new BigDecimal("500.00"));

        System.out.println("До перевода:");
        System.out.println("Alice (ID: " + alice.getId() + "): " + alice.getBalance());
        System.out.println("Bob (ID: " + bob.getId() + "): " + bob.getBalance());

        // Выполняем перевод
        boolean result = PessemisticMoneyTransferService.transferMoney(alice, bob, new BigDecimal("300.00"));

        System.out.println("\nПосле перевода (успешно: " + result + "):");
        System.out.println("Alice: " + alice.getBalance());
        System.out.println("Bob: " + bob.getBalance());

        // Тест на параллельные переводы
        testConcurrentTransfers();
    }

    static void testConcurrentTransfers() {
        System.out.println("\n=== Тест параллельных переводов с пессимистичной блокировкой ===");

        final Account account1 = new Account(1, new BigDecimal("10000.00"));
        final Account account2 = new Account(2, new BigDecimal("10000.00"));

        BigDecimal initialTotal = account1.getBalance().add(account2.getBalance());
        System.out.println("Начальная сумма: " + initialTotal);

        // Создаем и запускаем потоки
        final int THREADS = 100;
        final int TRANSFERS_PER_THREAD = 100;
        final CountDownLatch latch = new CountDownLatch(THREADS);
        final ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < TRANSFERS_PER_THREAD; j++) {
                        // Четные потоки переводят в одном направлении, нечетные - в обратном
                        if (threadId % 2 == 0) {
                            PessemisticMoneyTransferService.transferMoney(account1, account2, new BigDecimal("10"));
                        } else {
                            PessemisticMoneyTransferService.transferMoney(account2, account1, new BigDecimal("10"));
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();

        long endTime = System.currentTimeMillis();
        BigDecimal finalTotal = account1.getBalance().add(account2.getBalance());

        System.out.println("Финальная сумма: " + finalTotal);
        System.out.println("Сумма сохранилась: " + initialTotal.equals(finalTotal));
        System.out.println("Время выполнения: " + (endTime - startTime) + " мс");
    }
}
