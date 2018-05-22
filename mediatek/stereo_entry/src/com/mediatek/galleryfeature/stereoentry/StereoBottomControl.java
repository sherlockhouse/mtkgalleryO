package com.mediatek.galleryfeature.stereoentry;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.mediatek.gallerybasic.base.BackwardBottomController;
import com.mediatek.gallerybasic.base.IBottomControl;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.util.Log;

/**
 * If you want to add more buttons at bottom controller in single image view, implement this
 * interface. It provides chances to add buttons, click buttons, and check whether to display
 * buttons.
 */
public class StereoBottomControl implements IBottomControl, View.OnClickListener {
    private final static String TAG = "StereoBottomControl";
    public final static String TYPE_REFOCUS = "camera_refocus";
    public final static String TYPE_JPEG = "image/jpeg";

    public static final int REQUEST_FANCY_COLOR = 80;
    public static final int REQUEST_BACKGROUND = 81;
    public static final int REQUEST_COPY_PAST = 82;
    public static final int REQUEST_REFOCUS = 83;
    public static final int REQUEST_FREE_VIEW = 84;
    private final static String KEY_FILE_PATH = "filePath";
    private final static String KEY_SRC_BMP_WIDTH = "srcBmpWidth";
    private final static String KEY_SRC_BMP_HEIGHT = "srcBmpHeight";
    private BackwardBottomController mBackwardBottomController;
    private Resources mResource;
    private Context mContext;
    private Drawable mFreeviewMenu;
    private int mFreeviewMenuID;
    private ViewGroup mStereoMenu;
    private Drawable mStereoEntryMenu;
    private int mStereoEntryMenuID;
    private StereoIconView mStereoIconView;
    private boolean mInStereoMenu;
    private MediaData mMediaData;
    private ViewGroup mViewRoot;

    public StereoBottomControl(Context context, Resources res) {
        Log.d(TAG, "<StereoBottomControl> " + this);
        mContext = context;
        mResource = res;
        mStereoIconView = new StereoIconView(mResource);
    }

    @Override
    public void onBottomControlCreated() {
        if (StereoField.sSupportStereo) {
            mStereoEntryMenu = getDrawable(R.drawable.m_refocus_imagerefocus);
            mStereoEntryMenuID = mBackwardBottomController.addButton(mStereoEntryMenu);
            mFreeviewMenu = getDrawable(R.drawable.m_freeview_menu);
            mFreeviewMenuID = mBackwardBottomController.addButton(mFreeviewMenu);
            mInStereoMenu = false;
        }
        Log.d(TAG, "<onBottomControlCreated> mStereoEntryMenuID = "
                + mStereoEntryMenuID + " mFreeviewMenuID = " + mFreeviewMenuID);
    }

    @Override
    public boolean onUpPressed() {
        Log.d(TAG, "<onUpPressed> mInStereoMenu = " + mInStereoMenu);
        if (mInStereoMenu) {
            int size = mStereoMenu.getChildCount();
            for (int i = size - 1; i >= 0; i--) {
                View child = mStereoMenu.getChildAt(i);
                child.setVisibility(View.INVISIBLE);
                child.setOnClickListener(null);
            }
            ViewGroup parentView = (ViewGroup) mStereoMenu.getParent();
            parentView.removeView(mStereoMenu);
            mInStereoMenu = false;
            mBackwardBottomController.refresh(false);
            return true;
        }
        return false;
    }

    @Override
    public boolean onBackPressed() {
        return onUpPressed();
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "<onActivityResult> requestCode = " + requestCode + " resultCode = " +
                resultCode);
        switch (requestCode) {
            case REQUEST_REFOCUS:
            case REQUEST_COPY_PAST:
            case REQUEST_FANCY_COLOR:
            case REQUEST_FREE_VIEW:
            case REQUEST_BACKGROUND:
                if (resultCode == Activity.RESULT_OK) {
                    onUpPressed();
                } else if (resultCode == Activity.RESULT_CANCELED) {
                }
                break;
            default:
        }
        return false;
    }

    @Override
    public boolean onBottomControlButtonClicked(int id, MediaData data) {
        Log.d(TAG, "<onBottomControlButtonClicked> = " + id + " mFreeviewMenuID = "
                + mFreeviewMenuID + " || " + data);
        if (id == mStereoEntryMenuID) {
            return createStereoActionMenu(data);
        } else if (id == mFreeviewMenuID) {
            startFreeViewActivity();
        }
        return true;
    }

    @Override
    public int canDisplayBottomControls() {
        return mInStereoMenu ? DISPLAY_FALSE : DISPLAY_TRUE;
    }

    @Override
    public int canDisplayBottomControlButton(int id, MediaData data) {
        Log.d(TAG, "<canDisplayBottomControlButton> id = " + id + " data = " + data);
        if (mFreeviewMenuID != id && mStereoEntryMenuID != id) {
            return DISPLAY_IGNORE;
        }
        if (data == null || !StereoField.sSupportStereo) {
            return DISPLAY_FALSE;
        }
        mMediaData = data;
        boolean isDepthImage = false;
        boolean isStereoThumbImage = false;
        if (data.extFileds != null && null != data.extFileds.getImageField(TYPE_REFOCUS)) {
            isDepthImage = (1 == (int) data.extFileds.getImageField(TYPE_REFOCUS));
            isStereoThumbImage = (2 == (int) data.extFileds.getImageField(TYPE_REFOCUS));
        }
        boolean isJpeg = TYPE_JPEG.equals(data.mimeType);
        if (mStereoEntryMenuID == id && isJpeg && !isStereoThumbImage) {
            return DISPLAY_TRUE;
        }
        if (mFreeviewMenuID == id && isDepthImage) {
            return DISPLAY_TRUE;
        }
        return DISPLAY_FALSE;
    }

    @Override
    public void init(ViewGroup viewRoot, BackwardBottomController
            controller) {
        Log.d(TAG, " <init> viewRoot = " + viewRoot + " controller = " +
                controller);
        mViewRoot = viewRoot;
        mBackwardBottomController = controller;
    }

    private Drawable getDrawable(int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mResource.getDrawable(id, null);
        } else {
            return mResource.getDrawable(id);
        }
    }

    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick Id: " + view.getId());
        switch (view.getId()) {
            case R.id.m_stereo_fancy_color:
                startFancyColorActivity(mMediaData.uri);
                break;
            case R.id.m_stereo_refocus:
                startRefocusActivity(mMediaData.uri);
                break;
            case R.id.m_stereo_background:
                startBackgroundActivity(mMediaData.uri);
                break;
            case R.id.m_stereo_copy_paste:
                startCopyPasteActivity(mMediaData.uri);
                break;
            default:

        }
    }

    private boolean createStereoActionMenu(MediaData data) {
        if (data.extFileds == null) {
            return false;
        }
        mInStereoMenu = true;
        XmlResourceParser parser = null;
        try {
            boolean available = (1 == (int) data.extFileds.getImageField(TYPE_REFOCUS));
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            parser = mResource.getLayout(R.layout.m_stereo);
            mStereoMenu = (ViewGroup) inflater.inflate(parser, mViewRoot, false);
            mStereoMenu.setVisibility(View.VISIBLE);
            int size = StereoIconView.sViewArrayList.size();
            for (int i = 0; i < size; i++) {
                StereoIconView.Attribute attribute
                        = StereoIconView.sViewArrayList.get(i);
                ImageView stereoMenu = (ImageView) mStereoMenu.findViewById(attribute.mId);
                stereoMenu.setVisibility(View.VISIBLE);
                stereoMenu.setBackground(getDrawable(attribute.mBackgroundId));
                if (available || attribute.mId == R.id.m_stereo_copy_paste) {
                    stereoMenu.setImageBitmap(mStereoIconView.createBitmap
                            (attribute.mId, true));
                    stereoMenu.setClickable(true);
                    stereoMenu.setOnClickListener(this);
                } else {
                    stereoMenu.setImageBitmap(mStereoIconView.createBitmap
                            (attribute.mId, false));
                    stereoMenu.setEnabled(false);
                    stereoMenu.setOnClickListener(null);
                }
                mBackwardBottomController.refresh(true);
            }
            mViewRoot.addView(mStereoMenu);
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
        mViewRoot.requestLayout();
        return true;
    }

    private void startFreeViewActivity() {
        if (mMediaData != null) {
            Intent intent = new Intent("com.android.gallery3d.action.FREEVIEW");
            intent.setDataAndType(mMediaData.uri, mMediaData.mimeType).setFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Bundle bundle = new Bundle();
            bundle.putString(KEY_FILE_PATH, mMediaData.filePath);
            bundle.putInt(KEY_SRC_BMP_WIDTH, mMediaData.width);
            bundle.putInt(KEY_SRC_BMP_HEIGHT, mMediaData.height);
            intent.putExtras(bundle);
            Log.d(TAG, "<startFreeViewActivity> intent: " + intent);
            ((Activity) mContext).startActivityForResult(intent, REQUEST_FREE_VIEW);
        }
    }

    private void startRefocusActivity(Uri uri) {
        Intent intent = new Intent("com.mediatek.refocus.action.REFOCUS");
        intent.setDataAndType(uri, "image/*");
        Log.d(TAG, "<startRefocusActivity> intent: " + intent);
        ((Activity) mContext).startActivityForResult(intent, REQUEST_REFOCUS);
    }

    private void startFancyColorActivity(Uri uri) {
        Intent intent = new Intent("com.mediatek.fancycolor.action.FANCYCOLOR");
        intent.setDataAndType(uri, "image/*");
        Log.d(TAG, "<startRefocusActivity> intent: " + intent);
        ((Activity) mContext).startActivityForResult(intent, REQUEST_FANCY_COLOR);
    }

    private void startCopyPasteActivity(Uri uri) {
        Intent intent = new Intent("com.mediatek.segment.action.COPY_PASTE");
        intent.setDataAndType(uri, "image/*");
        Log.d(TAG, "<startCopyPasteActivity> intent: " + intent);
        ((Activity) mContext).startActivityForResult(intent, REQUEST_COPY_PAST);
    }

    private void startBackgroundActivity(Uri uri) {
        Intent intent = new Intent("com.mediatek.background.action.BACKGROUND");
        intent.setDataAndType(uri, "image/*").setFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Log.d(TAG, "<startBackgroundActivity> intent: " + intent);
        ((Activity) mContext).startActivityForResult(intent, REQUEST_BACKGROUND);
    }
}
