package OptimisticMoneyTransfer;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class OptimisticTransferDemo {
    public static void main(String[] args) {
        System.out.println("=== Демонстрация денежных переводов с оптимистичной блокировкой ===");

        // Запускаем тесты
        testSimpleTransfer();
        testInsufficientFunds();
        testConcurrentTransfers();
        testHighConcurrencyScenario();
    }

    /**
     * Тест 1: Простой перевод между двумя счетами
     */
    static void testSimpleTransfer() {
        System.out.println("\n=== Тест 1: Простой перевод ===");
        OptimisticAccount alice = new OptimisticAccount("A001", new BigDecimal("1000.00"));
        OptimisticAccount bob = new OptimisticAccount("B002", new BigDecimal("500.00"));

        System.out.println("До перевода:");
        System.out.println("Alice: " + alice.getBalance());
        System.out.println("Bob: " + bob.getBalance());

        OptimisticTransferService service = new OptimisticTransferService();
        boolean success = service.transferMoneyOptimistic(alice, bob, new BigDecimal("300.00"));

        System.out.println("\nПосле перевода (успешно: " + success + "):");
        System.out.println("Alice: " + alice.getBalance());
        System.out.println("Bob: " + bob.getBalance());
    }

    /**
     * Тест 2: Попытка перевода при недостатке средств
     */
    static void testInsufficientFunds() {
        System.out.println("\n=== Тест 2: Недостаточно средств ===");
        OptimisticAccount alice = new OptimisticAccount("A003", new BigDecimal("100.00"));
        OptimisticAccount bob = new OptimisticAccount("B004", new BigDecimal("500.00"));

        System.out.println("До перевода:");
        System.out.println("Alice: " + alice.getBalance());
        System.out.println("Bob: " + bob.getBalance());

        OptimisticTransferService service = new OptimisticTransferService();
        boolean success = service.transferMoneyOptimistic(alice, bob, new BigDecimal("300.00"));

        System.out.println("\nПосле попытки перевода (успешно: " + success + "):");
        System.out.println("Alice: " + alice.getBalance());
        System.out.println("Bob: " + bob.getBalance());
    }

    /**
     * Тест 3: Параллельные переводы между разными счетами
     */
    static void testConcurrentTransfers() {
        System.out.println("\n=== Тест 3: Параллельные переводы ===");
        OptimisticAccount alice = new OptimisticAccount("A005", new BigDecimal("1000.00"));
        OptimisticAccount bob = new OptimisticAccount("B006", new BigDecimal("1000.00"));
        OptimisticAccount charlie = new OptimisticAccount("C007", new BigDecimal("1000.00"));
        OptimisticAccount david = new OptimisticAccount("D008", new BigDecimal("1000.00"));

        System.out.println("До переводов:");
        System.out.println("Alice: " + alice.getBalance());
        System.out.println("Bob: " + bob.getBalance());
        System.out.println("Charlie: " + charlie.getBalance());
        System.out.println("David: " + david.getBalance());

        final int THREADS = 2;
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch latch = new CountDownLatch(THREADS);

        OptimisticTransferService service = new OptimisticTransferService();

        // Первый перевод
        executor.submit(() -> {
            try {
                service.transferMoneyOptimistic(alice, bob, new BigDecimal("300.00"));
            } finally {
                latch.countDown();
            }
        });

        // Второй перевод
        executor.submit(() -> {
            try {
                service.transferMoneyOptimistic(charlie, david, new BigDecimal("200.00"));
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();

        System.out.println("\nПосле переводов:");
        System.out.println("Alice: " + alice.getBalance());
        System.out.println("Bob: " + bob.getBalance());
        System.out.println("Charlie: " + charlie.getBalance());
        System.out.println("David: " + david.getBalance());
    }

    /**
     * Тест 4: Сценарий с высокой конкуренцией (много потоков работают с одними счетами)
     */
    static void testHighConcurrencyScenario() {
        System.out.println("\n=== Тест 4: Высокая конкуренция ===");
        OptimisticAccount shared1 = new OptimisticAccount("S001", new BigDecimal("10000.00"));
        OptimisticAccount shared2 = new OptimisticAccount("S002", new BigDecimal("10000.00"));

        BigDecimal initialTotal = shared1.getBalance().add(shared2.getBalance());

        System.out.println("До переводов:");
        System.out.println("Счет 1: " + shared1.getBalance());
        System.out.println("Счет 2: " + shared2.getBalance());
        System.out.println("Сумма: " + initialTotal);

        final int THREADS = 50;
        final int TRANSFERS_PER_THREAD = 10;
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch latch = new CountDownLatch(THREADS);
        AtomicInteger successCount = new AtomicInteger(0);

        OptimisticTransferService service = new OptimisticTransferService();

        for (int i = 0; i < THREADS; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < TRANSFERS_PER_THREAD; j++) {
                        // Четные потоки переводят в одном направлении, нечетные - в обратном
                        boolean success;
                        if (threadNum % 2 == 0) {
                            success = service.transferMoneyOptimistic(shared1, shared2, new BigDecimal("10"));
                        } else {
                            success = service.transferMoneyOptimistic(shared2, shared1, new BigDecimal("10"));
                        }

                        if (success) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();

        BigDecimal finalTotal = shared1.getBalance().add(shared2.getBalance());

        System.out.println("\nПосле переводов (успешных: " + successCount.get() + " из " +
                (THREADS * TRANSFERS_PER_THREAD) + "):");
        System.out.println("Счет 1: " + shared1.getBalance());
        System.out.println("Счет 2: " + shared2.getBalance());
        System.out.println("Сумма: " + finalTotal);
        System.out.println("Сумма сохранилась: " + initialTotal.equals(finalTotal));
    }
}

