package com.mediatek.photopicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.mediatek.photopicker.data.Item;
import com.mediatek.photopicker.hook.IViewHook;
import com.mediatek.photopicker.utils.Log;
import com.mediatek.photopicker.utils.Utils;

/**
 * SlotView in gridView which is imageView in effect.
 */
class SlotImageView extends ImageView {
    private static final String TAG = "PhotoPicker/SlotImageView";

    private static int sTargetSize = 0;
    public static final int BACKGROUND_COLOR = 0xFFFFFFFF;
    private Context mContext;
    private Item mItem;
    private IViewHook mViewHook;
    private MyTransformation mMyTransformation;

    public SlotImageView(Context context) {
        super(context);
        mContext = context;
        mViewHook = Utils.getViewHook(mContext);
        mMyTransformation = new MyTransformation(mContext);

        if (0 == sTargetSize) {
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(metrics);
            sTargetSize  = metrics.heightPixels / 5;
        }
    }

    /**
     * The way to modify the output image.
     */
    private class MyTransformation extends BitmapTransformation {
        private Paint mPaint;

        public MyTransformation(Context context) {
            super(context);
            mPaint = new Paint();
            mPaint.setColor(BACKGROUND_COLOR);
        }

        @Override
        protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth,
                                   int outHeight) {
            final Bitmap reuseBitmap = pool.get(outWidth, outHeight,
                                       toTransform.getConfig() != null ? toTransform.getConfig() :
                                                                         Bitmap.Config.ARGB_8888);
            Log.d(TAG, "<MyTransformation> outWidth & outHeight & reuseBitmap" +
                                           outWidth + " " + outHeight + " " + reuseBitmap);
            if (reuseBitmap != null && !pool.put(reuseBitmap)) {
                reuseBitmap.recycle();
            }

            Bitmap target = Bitmap.createBitmap(outWidth, outHeight, getConfig(reuseBitmap));
            Canvas canvas = new Canvas(target);
            canvas.drawPaint(mPaint);
            canvas.drawBitmap(reuseBitmap, 0, 0, mPaint);
            pool.put(toTransform);
            return target;
        }

        @Override
        public String getId() {
            Log.d(TAG, "<MyTransformation> getId: " + Utils.ID_PP_GLIDE);
            return Utils.ID_PP_GLIDE;
        }
    }

    public void updateData(Item item) {
        mItem = item;
        loadItemByGlide(item);
    }

    private void loadItemByGlide(Item item) {
        if (null != item) {
            Glide.with(mContext).load(item.pathUri)
                .asBitmap().dontAnimate()
                .override(sTargetSize, sTargetSize)
                .transform(mMyTransformation)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.RESULT).skipMemoryCache(false)
                .placeholder(mViewHook.getSlotPlaceHolder())
                .error(mViewHook.getSlotErrorHolder())
                .into(this);
        } else {
            Log.d(TAG, "<updateData> " + "item is null, show place holder drawable");
            this.setImageDrawable(mViewHook.getSlotPlaceHolder());
        }
    }

    public static void onDestory(Context context) {
        Log.d(TAG, "onDestory context = " + context);
        Glide.get(context).clearMemory();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mViewHook.onDrawSlotCover(canvas, mItem);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize(0, widthMeasureSpec),
                             getDefaultSize(0, heightMeasureSpec));
        int itemShowWidth = getMeasuredWidth();
        heightMeasureSpec = widthMeasureSpec = MeasureSpec.makeMeasureSpec(itemShowWidth,
                                               MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }
        return config;
    }
}
