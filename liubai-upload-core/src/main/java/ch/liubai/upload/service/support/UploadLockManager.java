package ch.liubai.upload.service.support;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Fixed-size striped locks serialize writes to the same logical upload without leaking keys.
 */
public final class UploadLockManager {

    private static final int LOCK_COUNT = 256;
    private final ReentrantLock[] locks = new ReentrantLock[LOCK_COUNT];

    public UploadLockManager() {
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    public <T> T execute(String key, CheckedSupplier<T> action) throws Exception {
        ReentrantLock lock = locks[(key.hashCode() & Integer.MAX_VALUE) % locks.length];
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    @FunctionalInterface
    public interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
