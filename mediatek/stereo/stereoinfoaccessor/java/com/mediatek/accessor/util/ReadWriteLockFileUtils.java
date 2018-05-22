package com.mediatek.accessor.util;

import com.mediatek.accessor.util.TraceHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockFileUtils {
    private static Map<String, ReadWriteLock> sLockCollections =
            new HashMap<String, ReadWriteLock>();
    private static Object sAutomicLock = new Object();

    public static void readLock(String filePath) {
        TraceHelper.beginSection(">>>>ReadWriteLockFileUtils-readLock");
        ReadWriteLock lock = null;
        synchronized (sAutomicLock) {
            if (!sLockCollections.isEmpty() && sLockCollections.containsKey(filePath)) {
                lock = sLockCollections.get(filePath);
            } else {
                lock = new ReentrantReadWriteLock();
                sLockCollections.put(filePath, lock);
            }
        }
        lock.readLock().lock();
        TraceHelper.endSection();
    }

    public static void readUnlock(String filePath) {
        TraceHelper.beginSection(">>>>ReadWriteLockFileUtils-readUnlock");
        ReadWriteLock lock = null;
        synchronized (sAutomicLock) {
            if (!sLockCollections.isEmpty() && sLockCollections.containsKey(filePath)) {
                lock = sLockCollections.get(filePath);
            } else {
                lock = new ReentrantReadWriteLock();
                sLockCollections.put(filePath, lock);
            }
        }
        lock.readLock().unlock();
        TraceHelper.endSection();
    }

    public static void writeLock(String filePath) {
        TraceHelper.beginSection(">>>>ReadWriteLockFileUtils-writeLock");
        ReadWriteLock lock = null;
        synchronized (sAutomicLock) {
            if (!sLockCollections.isEmpty() && sLockCollections.containsKey(filePath)) {
                lock = sLockCollections.get(filePath);
            } else {
                lock = new ReentrantReadWriteLock();
                sLockCollections.put(filePath, lock);
            }
        }
        lock.writeLock().lock();
        TraceHelper.endSection();
    }

    public static void writeUnlock(String filePath) {
        TraceHelper.beginSection(">>>>ReadWriteLockFileUtils-writeUnlock");
        ReadWriteLock lock = null;
        synchronized (sAutomicLock) {
            if (!sLockCollections.isEmpty() && sLockCollections.containsKey(filePath)) {
                lock = sLockCollections.get(filePath);
            } else {
                lock = new ReentrantReadWriteLock();
                sLockCollections.put(filePath, lock);
            }
        }
        lock.writeLock().unlock();
        TraceHelper.endSection();
    }
}
