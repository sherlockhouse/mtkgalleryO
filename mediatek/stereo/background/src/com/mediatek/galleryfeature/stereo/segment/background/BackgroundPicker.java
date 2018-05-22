package com.mediatek.galleryfeature.stereo.segment.background;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;

import com.mediatek.photopicker.PhotoPicker;
import com.mediatek.photopicker.data.AlbumItem;
import com.mediatek.photopicker.data.FileItem;
import com.mediatek.photopicker.data.Item;
import com.mediatek.photopicker.hook.DefaultViewHook;
import com.mediatek.photopicker.hook.IModelHook;
import com.mediatek.photopicker.utils.Utils;
import com.mediatek.util.Log;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * PhotoPicker to pick background image.
 */
public class BackgroundPicker extends PhotoPicker {
    private static final String TAG = Log.Tag("Bg/BackgroundPicker");

    private static final String BASE_SELECTION = Images.ImageColumns.WIDTH + " >= ? AND "
            + Images.ImageColumns.HEIGHT + " >= ?";
    private static final String[] BASE_SELECTION_ARGS = new String[] {
            "900", "900"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (sConfig != null) {  // TODO Eliminate sConfig in PhotoPicker
            super.onCreate(savedInstanceState);
            return;
        }

        PhotoPicker.Config config = new PhotoPicker.Config();
        IModelHook modelHook = new IModelHook() {
            public int getCount() {
                int albumCount = 0;
                String[] projection = new String[] { "count(distinct bucket_id)" };
                Cursor cursor = Utils.getCursor(BackgroundPicker.this,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, BASE_SELECTION,
                        BASE_SELECTION_ARGS, Utils.ORDER_DESC);
                if (null != cursor) {
                    try {
                        Assert.assertTrue(cursor.moveToNext());
                        albumCount = cursor.getInt(0);
                    } finally {
                        cursor.close();
                    }
                }
                Log.d(TAG, "<getCount> " + "AlbumCount: " + albumCount);
                return albumCount;
            }

            public List<Item> getItems(int queryStart, int queryEnd) {
                List<Item> queryList = new ArrayList<Item>();
                String[] projection = new String[] { Images.Media._ID, Images.Media.DATA,
                        Images.Media.BUCKET_ID, Images.Media.BUCKET_DISPLAY_NAME };
                Utils
                        .appendAlbumItems(queryList, BackgroundPicker.this, projection,
                                BASE_SELECTION, BASE_SELECTION_ARGS, Utils.ORDER_DESC, queryStart,
                                queryEnd);
                return queryList;
            }
        };
        config.modelHook = modelHook;

        // TODO ModelHook in AlbumPage seams awkward (most logic are similar to
        // that in AlbumSetPage
        // Can PhotoPicker do something for this?
        // This is why at that time I brought up with GalleryPicker concept.
        config.viewHook = new DefaultViewHook(BackgroundPicker.this) {
            @Override
            public boolean onTapSlot(Item item, Config newConfig) {
                if (item instanceof FileItem) {
                    return super.onTapSlot(item, newConfig);
                }

                final int bucketId = ((AlbumItem) item).bucketId;
                IModelHook modelHook = new IModelHook() {
                    @Override
                    public List<Item> getItems(int queryStart, int queryEnd) {
                        List<Item> queryList = new ArrayList<Item>();

                        String[] projection = null;
                        StringBuilder selection = new StringBuilder();
                        selection.setLength(0);
                        selection.append(Utils.CUR_BUCKET_ID).append(" = ").append(bucketId)
                                .append(" AND ").append(BackgroundPicker.BASE_SELECTION);
                        Utils.appendFileItems(queryList, BackgroundPicker.this, projection,
                                selection.toString(), BASE_SELECTION_ARGS, Utils.ORDER_DESC,
                                queryStart, queryEnd);
                        return queryList;
                    }

                    @Override
                    public int getCount() {
                        int fileCount = 0;
                        String[] projection = { "count(*)" };
                        StringBuilder selection = new StringBuilder();
                        selection.setLength(0);
                        selection.append(Utils.CUR_BUCKET_ID).append(" = ").append(bucketId)
                                .append(" AND ").append(BackgroundPicker.BASE_SELECTION);
                        Cursor cursor = Utils.getCursor(BackgroundPicker.this, Utils.URIBASE,
                                projection, selection.toString(), BASE_SELECTION_ARGS,
                                Utils.ORDER_DESC);
                        try {
                            Assert.assertTrue(cursor.moveToNext());
                            fileCount = cursor.getInt(0);
                        } finally {
                            cursor.close();
                        }
                        Log.d(TAG, "<getCount> " + "fileCount: " + fileCount);
                        return fileCount;
                    }
                };
                newConfig.modelHook = modelHook;

                return super.onTapSlot(item, newConfig);
            }
        };
        // Launch photoPicker
        Log.d(TAG, "<launchPhotoPicker> " + "launch");
        sConfig = config;
        super.onCreate(savedInstanceState);
    }
}