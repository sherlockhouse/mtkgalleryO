package com.mediatek.photopicker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.mediatek.photopicker.PhotoPicker.Config;
import com.mediatek.photopicker.data.AlbumItem;
import com.mediatek.photopicker.data.Item;
import com.mediatek.photopicker.hook.IViewHook;
import com.mediatek.photopicker.utils.Log;
import com.mediatek.photopicker.utils.Utils;

/**
 * View for PickPhoto.
 */
public class PhotoPickerView extends GridView implements OnScrollListener, OnItemClickListener {
    private static final String TAG = "PhotoPicker/PhotoPickerView";

    private PhotoPickerAdapter mPickerAdapter;
    private IViewHook mViewHook;
    private Context mContext;

    private int mVisibleStartLast;
    // [mVisibleStart, mVisibleEnd] in visibleList
    private int mVisibleStart;
    private int mVisibleEnd;
    // [mQueryStart, mQueryEnd] in queryList
    private int mQueryStart;
    private int mQueryEnd;

    private Config mConfig = null;
    private boolean mDirty;
    private boolean mIsResumed;

    /**
     * Constructor of PhotoPickerView.
     * @param context current context
     * @param attrs attributeSet used in super class
     */
    public PhotoPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setColumnWidth(Utils.getMyDimensionPixelSize(context, "itemSize"));
        setHorizontalSpacing(Utils.getMyDimensionPixelSize(context, "itemSpace"));
        setVerticalSpacing(Utils.getMyDimensionPixelSize(context, "itemSpace"));
        setBackgroundColor(Utils.getMyColor(context, "color_background"));
    }

    /**
     * Init for PhotoPickerView.
     */
    public void initPhotoPickerView(Config config) {
        Log.d(TAG, "<onCreate>" + Utils.BEGIN);
        mIsResumed = false;
        mDirty = true;

        mPickerAdapter = new PhotoPickerAdapter(mContext);
        mPickerAdapter.onCreate();
        setConfigInView(config);
        mPickerAdapter.setConfig(mConfig);
        setAdapter(mPickerAdapter);

        setOnScrollListener(this);
        setOnItemClickListener(this);
        Log.d(TAG, "<onCreate>" + Utils.END);
    }

    /**
     * onResume() in PhotoPickerView.
     */
    public void onResume() {
        Log.d(TAG, "<onResume>");
        mPickerAdapter.onResume();
        mIsResumed = true;
    }

    /**
     * onPause() in PhotoPickerView.
     */
    public void onPause() {
        Log.d(TAG, "<onPause>");
        mPickerAdapter.onPause();
        mIsResumed = false;
    }

    /**
     * onDestroy() in PhotoPickerView.
     */
    public void onDestroy() {
        mPickerAdapter.onDestroy();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        /*
        if (OnScrollListener.SCROLL_STATE_IDLE == scrollState) {
            mPickerAdapter.notifyDataDirty();
        }//*/
        super.onSaveInstanceState();
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        long timeStart = System.currentTimeMillis();
        if (!mIsResumed || (0 == visibleItemCount)) {
            return;
        }

        mVisibleStart = firstVisibleItem;
        mVisibleEnd = mVisibleStart + visibleItemCount;
        Log.d(TAG, "<onScroll> " + "mVisibleStart & mVisibleEnd: " +
                                   mVisibleStart + " - " + mVisibleEnd);
        mQueryStart = Math.max(mVisibleStart - mPickerAdapter.DATA_CACHE_SIZE, 0);
        mQueryEnd = Math.min(mVisibleStart + visibleItemCount + mPickerAdapter.DATA_CACHE_SIZE,
                             totalItemCount);

        boolean isOutOfBound = Math.abs(mVisibleStart - mVisibleStartLast) >=
                                        mPickerAdapter.DATA_CACHE_SIZE;
        if (Utils.ISTRACE) {
            Log.d(TAG, "<onScroll> " + "mVisibleStart & mVisibleStartLast & CACHE_SIZE: " +
                  mVisibleStart + "-" + mVisibleStartLast + "-" + mPickerAdapter.DATA_CACHE_SIZE);
            Log.d(TAG, "<onScroll> " + "isOutOfBound & mDirty: " + isOutOfBound + "-" + mDirty);
            Log.d(TAG, "<onScroll> " + "mQueryStart & mQueryEnd: " + mQueryStart + "-" + mQueryEnd);
        }
        if (isOutOfBound || mDirty) {
            mVisibleStartLast = mVisibleStart;
            mPickerAdapter.setActiveWindow(mQueryStart, mQueryEnd);
            mDirty = false;
        }
        Log.d(TAG, "<onScroll> " + "<debugtime> onScroll time: " +
                                   (System.currentTimeMillis() - timeStart) + "ms");
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Item item = (Item) mPickerAdapter.getItem(position % Utils.ARRAY_SIZE);
        Config newConfig = new Config();
        if (item instanceof AlbumItem) {
            newConfig.subTitle = ((AlbumItem) item).bucketName;
            newConfig.modelHook = Utils.getAlbumModelHook(mContext, ((AlbumItem) item).bucketId);
        } else {
            //newConfig.modelHook = Utils.getAlbumSetModelHook(mContext);
        }
        if (Utils.ISTRACE) {
            Log.d(TAG, "<onItemClick> " + "click's item & config.modelHook: " +
                                           item + "-" + newConfig.modelHook);
        }
        mViewHook.onTapSlot(item, newConfig);
    }

    /**
     * Set config for PhotoPickerView.
     * @param config
     */
    public void setConfigInView(PhotoPicker.Config config) {
        Log.d(TAG, "<setConfigInView> " + "debugConfig ...");
        Utils.debugConfig(config);

        mViewHook = Utils.getViewHook(mContext);
        if (null != config && null == config.viewHook) {
            config.viewHook = mViewHook;
        }
        if (null != config && null != config.viewHook) {
            mViewHook = config.viewHook;
        }
        Log.d(TAG, "<setConfigInView> " + "mViewHook: " + mViewHook);
        mConfig = config;
    }

    void reload() {
        mPickerAdapter.notifyDataDirty();
    }
}
