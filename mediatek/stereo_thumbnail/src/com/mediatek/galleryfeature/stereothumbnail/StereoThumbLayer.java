
package com.mediatek.galleryfeature.stereothumbnail;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mediatek.gallerybasic.base.Layer;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.base.Player;
import com.mediatek.gallerybasic.gl.MGLView;
import com.mediatek.gallerybasic.util.Log;

public class StereoThumbLayer extends Layer {
    private static final String TAG = "MtkGallery2/StereoThumbLayer";

    private Context mContext;
    private Resources mResources;
    private ViewGroup mStereoThumbView;
    private TextView mThumbHintTextView;
    private boolean mIsFilmMode;

    StereoThumbLayer(Context context, Resources res) {
        this.mContext = context;
        this.mResources = res;
    }

    @Override
    public void onCreate(Activity activity, ViewGroup root) {
        Log.i(TAG, "<onCreate>");
        createStereoThumbView(root);
    }

    @Override
    public void onResume(boolean isFilmMode) {
        Log.i(TAG, "<onResume>");
        mIsFilmMode = isFilmMode;
        updateThumbViewVisibility();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "<onPause>");
        if (mStereoThumbView != null) {
            mStereoThumbView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "<onDestroy>");
    }

    @Override
    public void setData(MediaData data) {

    }

    @Override
    public void setPlayer(Player player) {

    }

    @Override
    public View getView() {
        return mStereoThumbView;
    }

    @Override
    public MGLView getMGLView() {
        return null;
    }

    @Override
    public void onChange(Player player, int what, int arg, Object obj) {
    }

    @Override
    public void onFilmModeChange(boolean isFilmMode) {
        mIsFilmMode = isFilmMode;
        updateThumbViewVisibility();
    }

    private void updateThumbViewVisibility() {
        Log.d(TAG, "<updateIndicatorVisibility> mIsFilmMode = " + mIsFilmMode);
        if (mStereoThumbView == null) {
            Log.w(TAG, "<updateIndicatorVisibility> mStereoThumbView is null");
            return;
        }
        if (mIsFilmMode) {
            mStereoThumbView.setVisibility(View.INVISIBLE);
        } else {
            mStereoThumbView.setVisibility(View.VISIBLE);
        }
    }

    private boolean createStereoThumbView(ViewGroup root) {
        Log.d(TAG, "<createStereoThumbView> root = " + root);
        if (mContext == null || mResources == null) {
            Log.w(TAG, "<createStereoThumbView> context or resource is null");
            return false;
        }
        XmlResourceParser parser = null;
        try {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            parser = mResources.getLayout(R.layout.m_stereo_thumb);
            mStereoThumbView = (ViewGroup) inflater.inflate(parser, root, false);
            mStereoThumbView.setVisibility(View.INVISIBLE);
            mThumbHintTextView = (TextView) (mStereoThumbView.findViewById(R.id.thumb_hint));
            mThumbHintTextView.setText(mResources.getString(R.string.m_stereo_thumb_hint));
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
        return true;
    }
}
