package ru.homyakin.seeker.infrastructure.lock;

public interface LockService {
    boolean tryLock(String key);

    void unlock(String key);

    boolean tryLockAndExecute(String key, Runnable action);
}
