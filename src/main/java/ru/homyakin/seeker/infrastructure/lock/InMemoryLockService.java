package ru.homyakin.seeker.infrastructure.lock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLockService implements LockService {
    private final Map<String, Stub> storage = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String key) {
        return storage.putIfAbsent(key, Stub.INSTANCE) == null;
    }

    @Override
    public void unlock(String key) {
        storage.remove(key);
    }

    private enum Stub { INSTANCE }
}
