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
 * AlbumSetModelHook.
 */
public class AlbumSetModelHook implements IModelHook {
    private static final String TAG = "PhotoPicker/AlbumSetModelHook";
    private Context mContext;

    /**
     * AlbumSetModelHook constructor.
     * @param context Current context.
     */
    public AlbumSetModelHook(Context context) {
        mContext = context;
    }

    @Override
    public List<Item> getItems(int queryStart, int queryEnd) {
        List<Item> queryList = new ArrayList<Item>();

        String[] projection = null;
        String selection = null;
        String[] selectionArgs = null;
        Utils.appendAlbumItems(queryList, mContext,
                               projection, selection, selectionArgs, Utils.ORDER_DESC,
                               queryStart, queryEnd);
        return queryList;
    }

    @Override
    public int getCount() {
        int albumCount = 0;
        String[] projection = new String[] {"count(distinct bucket_id)"};
        String selection = null;
        String[] selectionArgs = null;
        Cursor cursor = Utils.getCursor(mContext, Utils.URIBASE,
                projection, selection, selectionArgs, Utils.ORDER_DESC);
        if (cursor == null) {
            Log.d(TAG, "<getCount> cursor is null.");
            return 0;
        }

        try {
            if (!cursor.moveToFirst()) {
                Log.d(TAG, "<getCount> cursor is not null, but it's empty.");
                return 0;
            }
            albumCount = cursor.getInt(0);
        } finally {
            cursor.close();
        }
        Log.d(TAG, "<getCount> AlbumCount: " + albumCount);
        return albumCount;
    }
}
