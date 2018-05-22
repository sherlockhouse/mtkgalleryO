/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. the information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2015. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */
package com.mediatek.freeview3d.animation;

import android.app.Activity;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.mediatek.freeview3d.SynchronizedHandler;
import com.mediatek.freeview3d.animation.GyroSensorEx.GyroPositionListener;
import com.mediatek.util.Log;

/**
 * Touch coordinate and gsensor angle transfer to coordinate in bitmap.
 */
public class AnimationEx implements GyroPositionListener, GestureDetector.OnGestureListener {
    private static final String TAG = Log.Tag("Fv/AnimationEx");

    public static int STEP = 30;
    public  int mFrameCountX;
    public  int mFrameCountY;
    private int mCurrentFrameIndexX = Integer.MAX_VALUE;
    private int mCurrentFrameIndexY = Integer.MAX_VALUE;
    private int mTargetFrameIndexX = Integer.MAX_VALUE;
    private int mTargetFrameIndexY = Integer.MAX_VALUE;
    private GyroSensorEx mGyroSensor = null;
    private GyroPositionCalculator mGsensorAngle = null;
    private Activity mContext;
    public boolean mIsOnTouched = false;
    private float[] mTouchPoints = new float[] {
            0, 0
    };
    private GestureDetector mGestureDetector;
    private Rect mDisPlayRect;
    private float mRateX = 1;
    private float mRateY = 1;
    private SynchronizedHandler mSynchronizedHandler;

    /**
     * Constructor.
     * @param context
     *            the Activity.
     * @param width
     *            the width of image.
     * @param height
     *            the height of image.
     * @param displayRect
     *            the Rect of the image.
     */
    public AnimationEx(Activity context, SynchronizedHandler handler, int width, int height,
            Rect displayRect) {
        mContext = context;
        mSynchronizedHandler = handler;
        mDisPlayRect = displayRect;
        initAnimation(width, height, width / 2, height / 2, width / 2, height / 2);
        startAnimation();
    }

    protected void move(float moveX, float moveY) {
        Log.d(TAG, "<move>, moveX & moveY: " + moveX + " " + moveY);
        mTargetFrameIndexX += moveX;
        mTargetFrameIndexY += moveY;
    }

    /**
     * Get current frame.
     * @return int[2]
     */
    public int[] getCurrentFrame() {
        advanceAnimation();
        return new int[] {
                (int) (mCurrentFrameIndexX), (int) (mCurrentFrameIndexY)
        };
    }

    /**
     * Check current animation is finished or not.
     * @return whether finished or not.
     */
    public boolean isFinished() {
        int dX = mTargetFrameIndexX - mCurrentFrameIndexX;
        int dY = mTargetFrameIndexY - mCurrentFrameIndexY;
        double distance = Math.sqrt(dX * dX + dY * dY);
        return distance <= STEP;
    }

    public void startAnimation() {
        if (mSynchronizedHandler == null) {
            Log.d(TAG, "<startAnimation> mSynchronizedHandler is null, do nothing");
            return;
        }
        mSynchronizedHandler.post(new Runnable() {
            @Override
            public void run() {
                createGyroSensor();
                createGestureDetector();
            }
        });
    }

    public void stopAnimation() {
        if (mSynchronizedHandler == null) {
            Log.d(TAG, "<stopAnimation> mSynchronizedHandler is null, do nothing");
            return;
        }
        mSynchronizedHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mGyroSensor != null) {
                    mGyroSensor.removeGyroPositionListener(AnimationEx.this);
                }
                mGyroSensor = null;
            }
        });
    }

    private void resetAnimation(float angleX, float angleY) {
        mTargetFrameIndexX = (int) (angleX * mFrameCountX);
        mTargetFrameIndexY = (int) (angleY * mFrameCountY);
    }

    private void createGestureDetector() {
        mGestureDetector = new GestureDetector(mContext, this);
    }

    private void calculateCurrentFrame() {
        Log.d(TAG, "<calculateCurrentFrame> Before calculate: targetY = " + mTargetFrameIndexY
                + " targetX = " + mTargetFrameIndexX + " currentY = " + mCurrentFrameIndexY
                + " currentX = " + mCurrentFrameIndexX);
        double distanceY = mTargetFrameIndexY - mCurrentFrameIndexY;
        double distanceX = mTargetFrameIndexX - mCurrentFrameIndexX;
        if (distanceX != 0) {
            double angle = Math.atan(distanceY / distanceX);
            double moveY;
            double moveX;
            if (distanceX > 0) {
                moveY = Math.sin(angle) * STEP;
                moveX = Math.cos(angle) * STEP;
            } else {
                moveY = Math.sin(Math.PI + angle) * STEP;
                moveX = Math.cos(Math.PI + angle) * STEP;
            }
            Log.d(TAG, " moveX = " + moveX + " moveY = " + moveY);
            mCurrentFrameIndexY += Math.round(moveY * mRateY);
            mCurrentFrameIndexX += Math.round(moveX * mRateX);
        } else {
            int dValueY = mTargetFrameIndexY - mCurrentFrameIndexY;
            if (dValueY > 0) {
                mCurrentFrameIndexY += STEP * mRateY;
            } else if (dValueY < 0) {
                mCurrentFrameIndexY -= STEP * mRateX;
            }
        }
        Log.d(TAG, "<calculateCurrentFrame> After calculate: targetY = " + mTargetFrameIndexY
                + " targetX = " + mTargetFrameIndexX + " currentY = " + mCurrentFrameIndexY
                + " currentX = " + mCurrentFrameIndexX);
    }

    private void initAnimation(int frameCountX, int frameCountY, int currentFrameIndexX,
                               int currentFrameIndexY, int targetFrameX, int targetFrameY) {
        mFrameCountX = frameCountX;
        mFrameCountY = frameCountY;
        mTargetFrameIndexX = targetFrameX;
        mTargetFrameIndexY = targetFrameY;
        mCurrentFrameIndexX = currentFrameIndexX;
        mCurrentFrameIndexY = currentFrameIndexY;
        if (mFrameCountX < mFrameCountY) {
            mRateX = 1;
            mRateY = (float) mFrameCountY / mFrameCountX;
        } else {
            mRateX = (float) mFrameCountX / mFrameCountY;
            mRateY = 1;
        }
        Log.d(TAG, "<initAnimation> mRateX & mRateY: " + mRateX + " " + mRateY);
    }

    @Override
    public float[] onCalculateAngle(long newTimestamp, float eventValues0,
                                    float eventValues1, int newRotation) {
        if (mIsOnTouched) {
            return null;
        }
        float[] angles = null;
        if (mGsensorAngle != null) {
            angles = mGsensorAngle.calculateAngle(newTimestamp, eventValues0,
                    eventValues1, newRotation);
        }
        if (angles != null) {
            resetAnimation(angles[0], angles[1]);
        }
        return angles;
    }

    private void advanceAnimation() {
        Log.d(TAG, "<advanceAnimation>  mCurrentFrameIndexX =" + mCurrentFrameIndexX
                + " mCurrentFrameIndexY = " + mCurrentFrameIndexY + " mTargetFrameIndexX="
                + mTargetFrameIndexX + " mTargetFrameIndexY " + mTargetFrameIndexY
                + " mFrameCountX=" + mFrameCountX);
        if (mCurrentFrameIndexX == Integer.MAX_VALUE || mTargetFrameIndexY == Integer.MAX_VALUE) {
            return;
        }
        calculateCurrentFrame();
    }

    private void createGyroSensor() {
        if (mGyroSensor == null) {
            mGyroSensor = new GyroSensorEx(mContext);
        }
        if (mGyroSensor.hasGyroSensor()) {
            mGyroSensor.setGyroPositionListener(this);
            mGsensorAngle = new GyroPositionCalculator();
        }
    }

    private boolean onUp() {
        mIsOnTouched = false;
        mTouchPoints[0] = 0;
        mTouchPoints[1] = 0;
        Log.d(TAG, "<onUp> onUp");
        return true;
    }

    private boolean onScroll(float dx, float dy, float totalX, float totalY) {
        float x = mTouchPoints[0] + totalX;
        float y = mTouchPoints[1] + totalY;
        if (mDisPlayRect != null && mDisPlayRect.contains((int) x, (int) y)) {
            move(-dx, dy);
        }
        return false;
    }

    @Override
    public boolean onDown(MotionEvent arg0) {
        mIsOnTouched = true;
        mTouchPoints[0] = arg0.getX();
        mTouchPoints[1] = arg0.getY();
        Log.d(TAG, "<onDown> x & y: " + arg0.getX() + " " + arg0.getY());
        return true;
    }

    /**
     * Get touch event.
     * @param event
     *            the touch event.
     * @return whether consume the event or not.
     */
    public boolean touchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            onUp();
        }
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(event);
        }
        return true;
    }

    @Override
    public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
        if (e1 != null && e2 != null) {
            onScroll(dx, dy, e2.getX() - e1.getX(), e2.getY() - e1.getY());
        }
        return false;
    }

    @Override
    public void onShowPress(MotionEvent arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean onSingleTapUp(MotionEvent arg0) {
        return false;
    }
}
