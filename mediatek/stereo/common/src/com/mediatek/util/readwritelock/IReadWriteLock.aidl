package com.mediatek.util.readwritelock;

interface IReadWriteLock
{
    void readLock(String filePath);
    void readUnlock(String filePath);
    void writeLock(String filePath);
    void writeUnlock(String filePath);
}