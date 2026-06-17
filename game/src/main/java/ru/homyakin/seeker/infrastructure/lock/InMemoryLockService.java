package ru.homyakin.seeker.infrastructure.lock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InMemoryLockService implements LockService {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryLockService.class);
    private final Map<String, Stub> storage = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String key) {
        return storage.putIfAbsent(key, Stub.INSTANCE) == null;
    }

    @Override
    public void unlock(String key) {
        storage.remove(key);
    }

    @Override
    public <T> Either<KeyLocked, T> tryLockAndCalc(String key, Supplier<T> supplier) {
        logger.debug("try to lock: " + key);
        if (tryLock(key)) {
            logger.debug("success lock " + key);
            final T result;
            try {
                result = supplier.get();
            } finally {
                unlock(key);
            }
            logger.debug("free lock " + key);
            return Either.right(result);
        }
        logger.debug("key already locked " + key);
        return Either.left(KeyLocked.INSTANCE);
    }

    private enum Stub { INSTANCE }
}
