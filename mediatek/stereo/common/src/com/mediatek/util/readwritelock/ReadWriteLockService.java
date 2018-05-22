package com.mediatek.util.readwritelock;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import com.mediatek.util.Log;

public class ReadWriteLockService extends Service {
    private static final String TAG = Log.Tag(ReadWriteLockService.class.getSimpleName());
    private ReadWriteLockImpl loclImpl = new ReadWriteLockImpl();

    public class ReadWriteLockImpl extends IReadWriteLock.Stub
    {

        @Override
        public void readLock(String filePath) throws RemoteException {
            FileReadWriteLock.getReadWriteLock().readLock(filePath);
        }

        @Override
        public void readUnlock(String filePath) throws RemoteException {
            FileReadWriteLock.getReadWriteLock().readUnlock(filePath);
        }

        @Override
        public void writeLock(String filePath) throws RemoteException {
            FileReadWriteLock.getReadWriteLock().writeLock(filePath);
        }

        @Override
        public void writeUnlock(String filePath) throws RemoteException {
            FileReadWriteLock.getReadWriteLock().writeUnlock(filePath);
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        Log.d(TAG, "<onBind>");
         return loclImpl;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "<onUnbind>");
        loclImpl = null;
        return false;
    }
}
