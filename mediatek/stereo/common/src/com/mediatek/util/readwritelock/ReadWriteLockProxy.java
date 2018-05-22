package com.mediatek.util.readwritelock;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.os.RemoteException;

import com.mediatek.util.Log;

public class ReadWriteLockProxy {
    private static String TAG = Log.Tag(ReadWriteLockProxy.class.getSimpleName());
    private static IReadWriteLock mReadWriteLock;
    private static boolean mHasStartService = false;
    private static Context mContext;
    private static ConditionVariable mWaitConnetion = new ConditionVariable();

    public static void startService(Context context) {
        Log.d(TAG, "<startService>");
        if (mHasStartService) {
            Log.d(TAG, "<startService> mHasStartService:" + mHasStartService);
            return;
        }
        Intent intent = new Intent();
        intent.setAction("com.mediatek.util.readwritelock.ReadWriteLockService");
        intent.setPackage("com.mediatek.refocus");
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mContext = context;
        mHasStartService = true;
    }

    public static void stopService() {
        if (!mHasStartService) {
            Log.d(TAG, "<stopService> mHasStartService:" + mHasStartService);
            return;
        }
        mHasStartService = false;
        mContext.unbindService(mConnection);
    }

    public static void readLock(String filePath) {
        Log.d(TAG, "<readLock>filePath:" + filePath + "thread:" +
                                 Thread.currentThread().getId());
        if (!waitConnection()) {
            return;
        }

        try {
            mReadWriteLock.readLock(filePath);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "<readLock> end, thread:" + Thread.currentThread().getId());
    }

    public static void readUnlock(String filePath) {
        Log.d(TAG, "<readUnlock>filePath:" + filePath + "thread:" +
                Thread.currentThread().getId());
        if (!waitConnection()) {
            return;
        }

        try {
            mReadWriteLock.readUnlock(filePath);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "<readUnlock> end, thread:" + Thread.currentThread().getId());
    }
    public static void writeLock(String filePath) {
        Log.d(TAG, "<writeLock>filePath:" + filePath + "thread:" +
                Thread.currentThread().getId());
        if (!waitConnection()) {
            return;
        }

        try {
            mReadWriteLock.writeLock(filePath);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "<writeLock> end, thread:" + Thread.currentThread().getId());
    }

    public static void writeUnlock(String filePath) {
        Log.d(TAG, "<writeUnlock>filePath:" + filePath + "thread:" +
                Thread.currentThread().getId());
        if (!waitConnection()) {
            return;
        }

        try {
            mReadWriteLock.writeUnlock(filePath);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "<writeUnlock> end, thread:" + Thread.currentThread().getId());
    }

    private static boolean waitConnection() {
        // Log.d(TAG, "<waitConnection> start");
        if (!mHasStartService) {
            Log.d(TAG, "<waitConnection>Please start service!!");
            return false;
        }
        mWaitConnetion.close();
        if (mReadWriteLock != null) {
            // Log.d(TAG, "<waitConnection>mReadWriteLock not null!");
            return true;
        }

        if (Thread.currentThread().getId() == mContext.getMainLooper().getThread().getId()) {
            Log.d(TAG, "<waitConnection>Main thread!!!!!");
            return false;
        }
        mWaitConnetion.block();
        // Log.d(TAG, "<waitConnection> end");
        return (mReadWriteLock != null);
    }

    private static ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder) {
            mReadWriteLock = IReadWriteLock.Stub.asInterface(binder);
            mWaitConnetion.open();
            Log.d(TAG, "<onServiceConnected> connect success!");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mReadWriteLock = null;
            Log.d(TAG, "<onServiceConnected> disconnect!");
        }
    };
}
