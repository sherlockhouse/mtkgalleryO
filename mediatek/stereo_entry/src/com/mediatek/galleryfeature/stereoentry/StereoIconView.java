package com.mediatek.galleryfeature.stereoentry;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.mediatek.gallerybasic.util.Log;

import java.util.ArrayList;

public class StereoIconView {
    private final static String TAG = "StereoIconView";
    private Resources mResource;
    public static ArrayList<Attribute> sViewArrayList = new ArrayList<Attribute>();
    public static int sHorizontalMargin = -1;
    private Paint mTextPaint = null;
    public final static float ALPHA_ABLE_VIEW = 1.0f;
    public final static float ALPHA_DISABLE_VIEW = 0.3f;

    public static class Attribute {
        public int mId;
        public int mTextId;
        public int mDrawableId;
        public int mDrawableDisableId;
        public int mBackgroundId;
        public Bitmap mAbleBitmap;
        public Bitmap mDisableBitmap;

    }

    static {
        Attribute refocusAttribute = new Attribute();
        refocusAttribute.mId = R.id.m_stereo_refocus;
        refocusAttribute.mTextId = R.string.m_stereo_refocus;
        refocusAttribute.mDrawableId = R.drawable.m_refocus;
        refocusAttribute.mDrawableDisableId = R.drawable.m_refocus_disable;
        refocusAttribute.mBackgroundId = R.drawable.m_bottom_button_background;
        sViewArrayList.add(refocusAttribute);

        Attribute backgroundAttribute = new Attribute();
        backgroundAttribute.mId = R.id.m_stereo_background;
        backgroundAttribute.mTextId = R.string.m_stereo_background;
        backgroundAttribute.mDrawableId = R.drawable.m_background;
        backgroundAttribute.mDrawableDisableId = R.drawable.m_background_disable;
        backgroundAttribute.mBackgroundId = R.drawable.m_bottom_button_background;
        sViewArrayList.add(backgroundAttribute);

        Attribute fancyColorAttribute = new Attribute();
        fancyColorAttribute.mId = R.id.m_stereo_fancy_color;
        fancyColorAttribute.mTextId = R.string.m_stereo_fancy_color;
        fancyColorAttribute.mDrawableId = R.drawable.m_fancycolor;
        fancyColorAttribute.mDrawableDisableId = R.drawable.m_fancycolor_disable;
        fancyColorAttribute.mBackgroundId = R.drawable.m_bottom_button_background;
        sViewArrayList.add(fancyColorAttribute);

        Attribute copyPasteAttribute = new Attribute();
        copyPasteAttribute.mId = R.id.m_stereo_copy_paste;
        copyPasteAttribute.mTextId = R.string.m_stereo_copy_paste;
        copyPasteAttribute.mDrawableId = R.drawable.m_copy_paste;
        copyPasteAttribute.mDrawableDisableId = R.drawable.m_copy_paste_disable;
        copyPasteAttribute.mBackgroundId = R.drawable.m_bottom_button_background;
        sViewArrayList.add(copyPasteAttribute);

    }

    public StereoIconView(Resources res) {
        mResource = res;
    }

    public Bitmap createBitmap(int id, boolean isDepthImage) {
        int size = sViewArrayList.size();
        for (int i = 0; i < size; i++) {
            Attribute attribute = sViewArrayList.get(i);
            if (attribute.mId == id) {
                return createBitmap(attribute, isDepthImage);
            }
        }
        return null;
    }

    public Bitmap getDrawableBitmap(int drawableId,
                                    int textId, float alpha) {
        String text = mResource.getString(textId);
        Paint paint = getTextPaint();
        Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        paint.setColor(alpha == 1 ? Color.WHITE : Color.GRAY);
        int lenght = (int) paint.measureText(text);
        int textheight = (int) Math.ceil(fontMetrics.descent
                - fontMetrics.ascent);
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inMutable = true;
        Bitmap bitmap = BitmapFactory.decodeResource(mResource,
                drawableId, option);
        int rightMargin = (sHorizontalMargin - bitmap.getWidth()) / 2;
        Log.d(TAG, " fontMetrics.descent = " + fontMetrics.descent
                + " fontMetrics.ascent = " + fontMetrics.ascent
                + " TextLenght = " + lenght + " rightMargin= " + rightMargin);
        Bitmap newBitmap = Bitmap.createBitmap(
                rightMargin * 2 + bitmap.getWidth(), bitmap.getHeight()
                        + textheight, Bitmap.Config.ARGB_8888);
        Canvas temp = new Canvas(newBitmap);
        temp.drawBitmap(bitmap, rightMargin, 0, null);
        temp.drawText(text, newBitmap.getWidth() / 2, bitmap.getHeight()
                - fontMetrics.ascent, paint);
        return newBitmap;
    }

    public Bitmap createBitmap(Attribute attribute, boolean isDepthImage) {
        if (sHorizontalMargin == -1) {
            sHorizontalMargin = getMaxTextLenght();
            Log.d(TAG, "<calculateTextLenght> sHorizontalMargin = " + sHorizontalMargin);
        }
        if (attribute.mAbleBitmap == null && isDepthImage) {
            attribute.mAbleBitmap = getDrawableBitmap(attribute.mDrawableId,
                    attribute.mTextId, ALPHA_ABLE_VIEW);

        }
        if (attribute.mDisableBitmap == null && !isDepthImage) {
            attribute.mDisableBitmap = getDrawableBitmap(attribute.mDrawableDisableId,
                    attribute.mTextId, ALPHA_DISABLE_VIEW);
        }
        return (isDepthImage ? attribute.mAbleBitmap : attribute.mDisableBitmap);
    }

    protected int calculateTextLenght(int id) {
        String text = mResource.getString(id);
        Paint paint = getTextPaint();
        Log.d(TAG, "<calculateTextLenght> textlenght = " + paint.measureText(text));
        return (int) paint.measureText(text);
    }

    private int getMaxTextLenght() {
        int textLength = 0;
        for (int i = 0; i < sViewArrayList.size(); i++) {
            Attribute attribute = sViewArrayList.get(i);
            textLength = Math.max(textLength, calculateTextLenght(attribute.mTextId));
        }
        return textLength;
    }

    private Paint getTextPaint() {
        if (mTextPaint == null) {
            mTextPaint = new Paint();
            mTextPaint.setAntiAlias(true);
            mTextPaint.setStyle(Paint.Style.FILL);
            mTextPaint.setStrokeWidth(1);
            int textSize = mResource.getDimensionPixelSize(
                    R.dimen.m_stereo_font_size);
            mTextPaint.setTextSize(textSize);
            mTextPaint.setTextAlign(Paint.Align.CENTER);
        }
        return mTextPaint;
    }

}
