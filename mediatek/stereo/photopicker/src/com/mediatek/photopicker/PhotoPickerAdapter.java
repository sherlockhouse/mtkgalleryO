package com.mediatek.photopicker;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView.ScaleType;

import com.mediatek.photopicker.data.Item;
import com.mediatek.photopicker.hook.IModelHook;
import com.mediatek.photopicker.utils.Log;
import com.mediatek.photopicker.utils.Utils;

import java.util.Arrays;
import java.util.List;

/**
 * Data adapter for photoPicker.
 */
class PhotoPickerAdapter extends BaseAdapter {
    private static final String TAG = "PhotoPicker/PhotoPickerAdapter";

    private Context mContext;
    private final Object mLock = new Object();
    private Item[] mContentList = new Item[Utils.ARRAY_SIZE];
    private Item[] mQueryList = new Item[Utils.ARRAY_SIZE];
    // SCREEN_VIS_SIZE-Max size of visible items on screen, DATA_CACHE_SIZE-The cache size.
    public static final int DATA_CACHE_SIZE = (Utils.ARRAY_SIZE - Utils.SCREEN_VIS_SIZE) >> 1;

    private int mQueryStart;
    private int mQueryEnd;
    private IModelHook mModelHook = null;
    private ReloadTask mReloadTask = null;
    private int mCount = 0;

    public PhotoPickerAdapter(Context context) {
        mContext = context;
    }

    /**
     * onCreate() in PhotoPickerView.
     */
    public void onCreate() {
        Arrays.fill(mContentList, null);
        Arrays.fill(mQueryList, null);
    }

    /**
     * onResume() in PhotoPickerView.
     */
    public void onResume() {
        Log.d(TAG, "<onResume>" + Utils.BEGIN);
        mReloadTask = new  ReloadTask();
        mReloadTask.setName("PP-QueryDB");
        mReloadTask.start();
        Log.d(TAG, "<onResume>" + Utils.END);
    }

    /**
     * onPause() in PhotoPickerView.
     */
    public void onPause() {
        Log.d(TAG, "<onPause>");
        mReloadTask.terminate();
    }

    /**
     * onDestroy() in PhotoPickerView.
     */
    public void onDestroy() {
        Log.d(TAG, "<onPause>");
        mContentList = null;
        mQueryList = null;
        SlotImageView.onDestory(mContext);

    }

    /**
     * Query image items from database in another thread.
     */
    private class ReloadTask extends Thread {
        private volatile boolean mActive;
        private volatile boolean mDirty;
        private volatile boolean mState;
        private int mInCount;

        public ReloadTask() {
            mActive = true;
            mState = false;
            mDirty = true;
        }

        @Override
        public void run() {
            Log.d(TAG, "<run> " + "active& state& dirty: " + mActive + "-" + mState + "-" + mDirty);
            while (mActive) {
                synchronized (this) {
                    if (mActive && !mDirty) {
                        waitWithoutInterrupt(this);
                        if (!mActive) {
                            return;
                        }
                    }
                    mDirty = false;
                }

                long timeStart = System.currentTimeMillis();
                mInCount = getCountIn();
                Log.d(TAG, "<run> " + "<debugtime> getCountIn: " +
                                       (System.currentTimeMillis() - timeStart) + "ms");

                timeStart = System.currentTimeMillis();
                // The images in database has changed in background while in PhotoPicker
                int queryTemp = Math.max(mQueryEnd, mQueryStart + DATA_CACHE_SIZE);
                List<Item> queryList = mModelHook.getItems(mQueryStart, queryTemp);
                if (!queryList.isEmpty()) {
                    for (int i = 0; i < queryList.size(); i++) {
                        if (mQueryList != null) {
                            mQueryList[(mQueryStart + i) % Utils.ARRAY_SIZE] = queryList.get(i);
                        }
                    }
                }
                Log.d(TAG, "<run> " + "<debugtime> getItems: " +
                                       (System.currentTimeMillis() - timeStart) + "ms");

                mState = false;
                ((Activity) (mContext)).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (null != mQueryList) {
                            mCount = mInCount;
                            updateUiData();
                            notifyDataSetChanged();
                            mState = true;
                            synchronized (mLock) {
                                mLock.notifyAll();
                            }
                        }
                    }
                });

                synchronized (mLock) {
                    while (!mState) {
                        try {
                            mLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        public synchronized void notifyDirty() {
            mDirty = true;
            notifyAll();
        }

        public synchronized void terminate() {
            mActive = false;
            notifyAll();
        }

        public void waitWithoutInterrupt(Object object) {
            try {
                object.wait();
            } catch (InterruptedException e) {
                Log.e(TAG, "<waitWithoutInterrupt> " + "unexpected interrupt" + new Throwable());
            }
        }
    }

    @Override
    public int getCount() {
        return mCount;
    }

    private int getCountIn() {
        return mModelHook.getCount();
    }

    @Override
    public Object getItem(int position) {
        return mContentList[position % Utils.ARRAY_SIZE];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SlotImageView slotImageView = null;
        if (null == convertView) {
            slotImageView = new SlotImageView(mContext);
        } else {
            slotImageView = (SlotImageView) convertView;
        }
        slotImageView.setScaleType(ScaleType.CENTER_CROP);
        /*
        if (mContentList.length < 1) {
            Log.d(TAG, "<getView> " + "mVisibleList.length < 1");
            return slotImageView;
        }//*/

        slotImageView.updateData((Item) getItem(position));
        return slotImageView;
    }

    public void setActiveWindow(int queryStart, int queryEnd) {
        mQueryStart = queryStart;
        mQueryEnd = Math.min(queryEnd, mCount);
        if (mQueryEnd > mQueryStart) {
            notifyDataDirty();
        }
    }

    public void setConfig(PhotoPicker.Config config) {
        Log.d(TAG, "<setConfig> " + "debugConfig ...");
        Utils.debugConfig(config);

        if (null != config) {
            if (null != config.modelHook) {
                mModelHook = config.modelHook;
            } else {
                mModelHook = Utils.getAlbumSetModelHook(mContext);
            }
        } else {
            mModelHook = Utils.getAlbumSetModelHook(mContext);
        }
    }

    public void notifyDataDirty() {
        mReloadTask.notifyDirty();
    }

    private void updateUiData() {
        Log.d(TAG, "<updateUiData> " + "update UI data");
        mContentList = Arrays.copyOf(mQueryList, mQueryList.length);
    }
}
