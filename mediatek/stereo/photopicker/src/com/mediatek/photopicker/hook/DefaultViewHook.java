package com.mediatek.photopicker.hook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.mediatek.photopicker.PhotoPicker;
import com.mediatek.photopicker.PhotoPicker.Config;
import com.mediatek.photopicker.data.AlbumItem;
import com.mediatek.photopicker.data.Item;
import com.mediatek.photopicker.utils.Log;
import com.mediatek.photopicker.utils.Utils;

/**
 * Default implements for IViewHook.
 */
public class DefaultViewHook implements IViewHook {
    private static final String TAG = "PhotoPicker/DefaultViewHook";

    private Context mContext;
    private static Drawable sSlotPlaceHolder = null;
    private static Drawable sSlotErrorHolder = null;

    /**
     * DefaultViewHook constructor.
     * @param context context
     */
    public DefaultViewHook(Context context) {
        mContext = context;
    }

    @Override
    public void onDrawSlotCover(Canvas canvas, Item item) {
        if (item instanceof AlbumItem) {
            if (Utils.ISTRACE) {
                Log.d(TAG, "<onDrawSlotCover> " + "Draw label & folder icon on albumItem");
            }
            String subTitle = ((AlbumItem) item).bucketName;
            if (subTitle.length() > Utils.SUBTITLE_SIZE) {
                subTitle = subTitle.substring(0, Utils.SUBTITLE_SIZE).concat("...");
            }
            Utils.drawSlotLabel(mContext, canvas, subTitle);
        } else {
            // Do nothing for fileItem now, maybe do something further
        }
    }

    @Override
    public boolean onTapSlot(Item item, Config newConfig) {
        Log.d(TAG, "<onTapSlot> " + "debugConfig ...");
        Utils.debugConfig(newConfig);

        if (item instanceof AlbumItem) {
            Log.d(TAG, "<onTapSlot> " + "launch again");
            PhotoPicker.launch((Activity) mContext, newConfig);
        } else {
            Intent intent = new Intent();
            intent.setData(item.pathUri);
            Log.d(TAG, "<onTapSlot> " + "item.pathUri: " + item.pathUri + " and call setResult");
            ((Activity) mContext).setResult(PhotoPicker.RESULT_OK, intent);
            ((Activity) mContext).finish();
        }
        return true;
    }

    @Override
    public Bitmap onTransfromSlotContent(Bitmap toTransform, Item item,
            int outWidth, int outHeight) {
        return toTransform;
    }

    @Override
    public Drawable getSlotErrorHolder() {
        if (null == sSlotErrorHolder) {
            sSlotErrorHolder = Utils.getMyDrawable(mContext, "ic_holder");
        }
        return sSlotErrorHolder;
    }

    @Override
    public Drawable getSlotPlaceHolder() {
        if (null == sSlotPlaceHolder) {
            sSlotPlaceHolder = Utils.getMyDrawable(mContext, "ic_holder");
        }
        return sSlotPlaceHolder;
    }
}
