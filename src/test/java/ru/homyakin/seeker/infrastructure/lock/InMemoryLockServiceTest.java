package ru.homyakin.seeker.infrastructure.lock;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InMemoryLockServiceTest {
    private final InMemoryLockService service = new InMemoryLockService();

    @Test
    public void When_TryToLockNewKey_Then_KeyLocked() {
        //when
        final var key = UUID.randomUUID().toString();
        final var result = service.tryLock(key);

        //then
        Assertions.assertTrue(result);
    }

    @Test
    public void Given_LockedKey_When_TryToLockSeveralTimes_Then_NoLockHappened() {
        //given
        final var key = UUID.randomUUID().toString();
        service.tryLock(key);

        //when
        final var result1 = service.tryLock(key);
        final var result2 = service.tryLock(key);
        final var result3 = service.tryLock(key);

        //then
        Assertions.assertFalse(result1);
        Assertions.assertFalse(result2);
        Assertions.assertFalse(result3);
    }

    @Test
    public void Given_LockedKey_When_LockAfterUnlock_Then_SuccessLocked() {
        //given
        final var key = UUID.randomUUID().toString();
        service.tryLock(key);

        //when
        service.unlock(key);
        final var result = service.tryLock(key);

        //then
        Assertions.assertTrue(result);
    }

    @Test
    public void When_TryLockInMultithreading_Then_Only_OneLockHappened() throws InterruptedException {
        final var key = UUID.randomUUID().toString();
        final var threadCount = 10;
        final var executorService = Executors.newFixedThreadPool(threadCount);
        final var latch = new CountDownLatch(threadCount);
        final var startSignal =  new CountDownLatch(1);
        final var results = new ArrayList<Boolean>();

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    startSignal.await();
                    results.add(service.tryLock(key));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        startSignal.countDown();
        latch.await();
        executorService.shutdown();

        // then
        final var trueCount = results.stream().filter(Boolean::booleanValue).count();
        Assertions.assertEquals(1, trueCount);
    }

    @Test
    public void Given_TwoKeys_When_LockKeys_Then_SuccessLock() {
        // given
        final var key1 = UUID.randomUUID().toString();
        final var key2 = UUID.randomUUID().toString();

        // when
        final var result1 = service.tryLock(key1);
        final var result2 = service.tryLock(key2);

        // then
        Assertions.assertTrue(result1);
        Assertions.assertTrue(result2);
    }
    @Test
    public void Given_TwoLockedKeys_When_UnlockKey1_Then_Key2StillLocked() {
        // given
        final var key1 = UUID.randomUUID().toString();
        final var key2 = UUID.randomUUID().toString();
        service.tryLock(key1);
        service.tryLock(key2);

        // when
        service.unlock(key1);

        // then
        Assertions.assertFalse(service.tryLock(key2));
    }
}
