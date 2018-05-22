package com.mediatek.photopicker.hook;

import android.content.Context;
import android.database.Cursor;

import com.mediatek.photopicker.data.Item;
import com.mediatek.photopicker.utils.Log;
import com.mediatek.photopicker.utils.Utils;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * AlbumModelHook.
 */
public class AlbumModelHook implements IModelHook {
    private static final String TAG = "PhotoPicker/AlbumModelHook";

    private Context mContext;
    private int mBucketId;
    private StringBuilder mSelection;

    /**
     * AlbumModelHook.
     * @param context constructor
     * @param bucketId bucket_id in database
     */
    public AlbumModelHook(Context context, int bucketId) {
        mContext = context;
        mBucketId = bucketId;
        mSelection = new StringBuilder();
    }

    @Override
    public List<Item> getItems(int queryStart, int queryEnd) {
        List<Item> queryList = new ArrayList<Item>();

        String[] projection = null;
        mSelection.setLength(0);
        mSelection.append(Utils.CUR_BUCKET_ID).append(" = ?");
        String[] selectionArgs = new String[] { String.valueOf(mBucketId) };
        Utils.appendFileItems(queryList, mContext,
                              projection, mSelection.toString(), selectionArgs, Utils.ORDER_DESC,
                              queryStart, queryEnd);
        return queryList;
    }

    @Override
    public int getCount() {
        int fileCount = 0;
        String[] projection = { "count(*)" };
        mSelection.setLength(0);
        mSelection.append(Utils.CUR_BUCKET_ID).append(" = ?");
        String[] selectionArgs = new String[] { String.valueOf(mBucketId) };

        Cursor cursor = Utils.getCursor(mContext, Utils.URIBASE,
                        projection, mSelection.toString(), selectionArgs, Utils.ORDER_DESC);
        if (cursor == null) {
            Log.d(TAG, "<getCount> cursor is null.");
            return 0;
        }

        try {
            if (!cursor.moveToFirst()) {
                Log.d(TAG, "<getCount> cursor is not null, but it's empty.");
                return 0;
            }
            fileCount = cursor.getInt(0);
        } finally {
            cursor.close();
        }
        Log.d(TAG, "<getCount> fileCount: " + fileCount);
        return fileCount;
    }
}