package com.mediatek.util.readwritelock;

import android.os.ConditionVariable;

import com.mediatek.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FileReadWriteLock {
    private static final String TAG = Log.Tag(FileReadWriteLock.class.getSimpleName());

    private volatile static FileReadWriteLock mLock;

    private Map<String, LockCounter> sLockMap = new HashMap<String, LockCounter>();
    private ConditionList mConditionList = new ConditionList();

    public static FileReadWriteLock getReadWriteLock() {
        if (mLock == null) {
            synchronized (FileReadWriteLock.class) {
                if (mLock == null) {
                    mLock = new FileReadWriteLock();
                }
            }
        }
        return mLock;
    }

    private FileReadWriteLock() {
    }

    private class LockCounter {
        private int mReadCount = 0;
        private int mWriteCount = 0;

        public synchronized int getReadCount() {
            return mReadCount;
        }

        public synchronized int getWriteCount() {
            return mWriteCount;
        }

        public synchronized void setReadCount(int count) {
            mReadCount = count;
        }

        public synchronized void setWriteCount(int count) {
            mWriteCount = count;
        }
    }

    private class ConditionList {
        private ArrayList<ConditionVariable> mConditionList = new ArrayList<ConditionVariable>();

        public synchronized void add(ConditionVariable condition) {
            mConditionList.add(condition);
        }

        public synchronized void remove(ConditionVariable condition) {
            mConditionList.remove(condition);
        }

        public synchronized void open() {
            for (ConditionVariable condition : mConditionList) {
                condition.open();
            }
        }
    }

    public void readLock(String filePath) {
        Log.d(TAG, "<readLock> filePath:" + filePath + ",thread:" + Thread.currentThread().getId());

        LockCounter counter = findCounter(filePath, true);
        if (counter == null) {
            Log.d(TAG, "<readLock> find and create LockCounter fai!!!");
            return;
        }

        ConditionVariable condition = new ConditionVariable();
        mConditionList.add(condition);

        condition.close();
        while (counter.getWriteCount() > 0) {
            condition.block();
            counter = sLockMap.get(filePath);
            condition.close();
        }

        counter.setReadCount(counter.getReadCount() + 1);
        mConditionList.remove(condition);
        Log.d(TAG, "<readLock> begin work" + ",thread:" + Thread.currentThread().getId());
    }

    public void readUnlock(String filePath) {
        Log.d(TAG, "<readUnlock> filePath:" + filePath);

        LockCounter counter = findCounter(filePath, false);
        if (counter == null) {
            Log.d(TAG, "<readUnlock> find LockCounter fai!!!");
            return;
        }

        counter.setReadCount(counter.getReadCount() - 1);
        mConditionList.open();
    }

    public void writeLock(String filePath) {
        Log.d(TAG, "<writeLock> filePath:" + filePath + ",thread:"
                + Thread.currentThread().getId());
        LockCounter counter = findCounter(filePath, true);
        if (counter == null) {
            Log.d(TAG, "<writeLock> create LockCounter fai!!!");
            return;
        }

        ConditionVariable condition = new ConditionVariable();
        mConditionList.add(condition);

        condition.close();
        while (counter.getReadCount() > 0 || counter.getWriteCount() > 0) {
            condition.block();
            counter = sLockMap.get(filePath);
            condition.close();
        }

        counter.setWriteCount(counter.getWriteCount() + 1);
        mConditionList.remove(condition);
        Log.d(TAG, "<writeLock> begin work" + ",thread:" + Thread.currentThread().getId());
    }

    public void writeUnlock(String filePath) {
        Log.d(TAG, "<writeUnlock> filePath:" + filePath);
        LockCounter counter = findCounter(filePath, false);
        if (counter == null) {
            Log.d(TAG, "<writeUnlock> find LockCounter fai!!!");
            return;
        }

        counter.setWriteCount(counter.getWriteCount() - 1);
        mConditionList.open();
    }

    private synchronized LockCounter findCounter(String filePath, boolean create) {
        LockCounter counter = null;

        if (!sLockMap.isEmpty() && sLockMap.containsKey(filePath)) {
            counter = sLockMap.get(filePath);
        } else {
            if (create) {
                counter = new LockCounter();
                sLockMap.put(filePath, counter);
            }
        }
        return counter;
    }
}
