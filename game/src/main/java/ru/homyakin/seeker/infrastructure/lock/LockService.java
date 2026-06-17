package ru.homyakin.seeker.infrastructure.lock;

import io.vavr.control.Either;
import ru.homyakin.seeker.utils.models.Success;

import java.util.function.Supplier;

public interface LockService {
    boolean tryLock(String key);

    void unlock(String key);

    default Either<KeyLocked, Success> tryLockAndExecute(String key, Runnable action) {
        return tryLockAndCalc(key, () -> {
            action.run();
            return Success.INSTANCE;
        });
    }

    <T> Either<KeyLocked, T> tryLockAndCalc(String key, Supplier<T> supplier);
}
