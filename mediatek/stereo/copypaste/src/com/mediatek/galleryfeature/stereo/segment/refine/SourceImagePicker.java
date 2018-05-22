package com.mediatek.galleryfeature.stereo.segment.refine;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.widget.Toast;

import com.mediatek.galleryfeature.stereo.segment.SegmentUtils;
import com.mediatek.galleryfeature.stereo.segment.copypaste.R;
import com.mediatek.photopicker.PhotoPicker;
import com.mediatek.photopicker.data.FileItem;
import com.mediatek.photopicker.data.Item;
import com.mediatek.photopicker.hook.DefaultViewHook;
import com.mediatek.photopicker.hook.IModelHook;
import com.mediatek.photopicker.utils.Utils;
import com.mediatek.util.Log;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PhotoPicker to pick the source image for refine/synth.
 */
public class SourceImagePicker extends PhotoPicker {
    private static final String TAG = Log.Tag("Cp/SourceImagePicker");
    public static final String KEY_IS_DEPTH_IMAGE = "key_is_depth_image";

    private static final String STREO_WHERE_CLAUSE = SegmentUtils.CAMERA_REFOCUS + " <> 0";
    private static final int CLIPPINGS_BUCKET_ID = (Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/"
            + "Pictures/Clippings").toLowerCase(Locale.ENGLISH).hashCode();
    private static final String[] COUNT_PROJECTION = { "count(*)" };
    private static final String ORDER_CLAUSE = ImageColumns.DATE_TAKEN + " DESC, "
            + ImageColumns._ID + " DESC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (sConfig != null) { // TODO Eliminate sConfig in PhotoPicker
            super.onCreate(savedInstanceState);
            return;
        }

        PhotoPicker.Config config = new PhotoPicker.Config();
        config.title = getString(R.string.m_pick_photo_to_copy);
        IModelHook modelHook = new IModelHook() {
            public int getCount() {
                int count = 0;

                Item clippingItem = getClippingsAlbumItem();
                if (clippingItem != null) {
                    count = 1;
                }

                Cursor cursor = Utils.getCursor(SourceImagePicker.this, Utils.URIBASE,
                        COUNT_PROJECTION, STREO_WHERE_CLAUSE, null, null);
                if (cursor == null) {
                    return 0;
                }
                try {
                    Assert.assertTrue(cursor.moveToNext());
                    count += cursor.getInt(0);
                } finally {
                    cursor.close();
                }
                Log.d(TAG, "<getCount> " + "AlbumCount: " + count);
                if (count == 0) {
                    SourceImagePicker.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SourceImagePicker.this, R.string.m_no_stereo_image,
                                    Toast.LENGTH_LONG).show();
                            SourceImagePicker.this.finish();
                        }
                    });
                }
                return count;
            }

            public List<Item> getItems(int queryStart, int queryEnd) {
                List<Item> queryList = new ArrayList<Item>();

                Item clippingItem = getClippingsAlbumItem();
                if (clippingItem != null) {
                    if (queryStart == 0) {
                        queryList.add(clippingItem);
                    } else {
                        queryStart--; // cus index 0 => clippings
                    }
                    queryEnd--; // clippings takes 1 account
                }

                Utils.appendFileItems(queryList, SourceImagePicker.this, null,
                        STREO_WHERE_CLAUSE, null, ORDER_CLAUSE, queryStart, queryEnd);
                return queryList;
            }
        };
        config.modelHook = modelHook;

        config.viewHook = new DefaultViewHook(this) {
            @Override
            public boolean onTapSlot(Item item, Config newConfig) {
                if (item instanceof FileItem) {
                    Intent intent = new Intent();
                    intent.setData(item.pathUri);
                    // Accelerate stere judgement by RefineImagePicker user
                    intent.putExtra(KEY_IS_DEPTH_IMAGE, true);
                    Log.d(TAG, "<onTapSlot> " + "item.pathUri: "
                            + item.pathUri + " and call setResult");
                    setResult(PhotoPicker.RESULT_OK, intent);
                    finish();
                    return true;
                } else {
                    return super.onTapSlot(item, newConfig);
                }
            }
        };

        // Launch photoPicker
        Log.d(TAG, "<onCreate> " + "config: " + config);
        sConfig = config;
        super.onCreate(savedInstanceState);
    }

    // TODO bucket id moved inside
    private Item getClippingsAlbumItem() {
        List<Item> queryList = new ArrayList<Item>();

        String[] projection = new String[] { Images.Media._ID, Images.Media.DATA,
                Images.Media.BUCKET_ID, Images.Media.BUCKET_DISPLAY_NAME };
        String whereClause = ImageColumns.BUCKET_ID + "= ?";
        String[] whereClauseArgs = new String[] { String.valueOf(CLIPPINGS_BUCKET_ID) };
        Utils.appendAlbumItems(queryList, SourceImagePicker.this, projection,
                whereClause, whereClauseArgs, ORDER_CLAUSE, 0, 0);

        return (queryList.isEmpty() ? null : queryList.get(0));
    }
}
