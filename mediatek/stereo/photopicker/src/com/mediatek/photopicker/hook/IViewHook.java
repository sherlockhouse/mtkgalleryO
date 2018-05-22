package com.mediatek.photopicker.hook;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.mediatek.photopicker.PhotoPicker;
import com.mediatek.photopicker.data.Item;

/**
 * IViewHook for enlarge the scope of PhotoPickerView.
 */
public interface IViewHook {
    /**
     * getSlotPlaceHolder.
     * @return drawable
     */
    public Drawable getSlotPlaceHolder();

    /**
     * getSlotErrorDrawable.
     * @return drawable
     */
    public Drawable getSlotErrorHolder();

    /**
     * onTransfromSlotContent.
     * @param toTransform null
     * @param item null
     * @param outWidth null
     * @param outHeight null
     * @return null
     */
    public Bitmap onTransfromSlotContent(Bitmap toTransform, Item item,
            int outWidth, int outHeight);

    /**
     * onDrawSlotCover.
     * @param canvas null
     * @param item null
     */
    public void onDrawSlotCover(Canvas canvas, Item item);

    /**
     * onTapSlot.
     * @param item null
     * @param newConfig null
     * @return null
     */
    public boolean onTapSlot(Item item, PhotoPicker.Config newConfig);
}
