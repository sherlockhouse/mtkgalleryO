package com.mediatek.photopicker.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.view.LayoutInflater;
import android.view.View;

import com.mediatek.photopicker.PhotoPicker;
import com.mediatek.photopicker.PhotoPicker.Config;
import com.mediatek.photopicker.data.AlbumItem;
import com.mediatek.photopicker.data.FileItem;
import com.mediatek.photopicker.data.Item;
import com.mediatek.photopicker.hook.AlbumModelHook;
import com.mediatek.photopicker.hook.AlbumSetModelHook;
import com.mediatek.photopicker.hook.DefaultControlHook;
import com.mediatek.photopicker.hook.DefaultViewHook;
import com.mediatek.photopicker.hook.IControlHook;
import com.mediatek.photopicker.hook.IViewHook;

import java.util.List;

/**
 * Common tools for PhotoPicker.
 */
public class Utils {
    private static final String TAG = "PhotoPicker/Utils";

    private static final String TAG_PPDEBUG = "PPDEBUG";
    public static final String BEGIN = "begin ...";
    public static final String END = "end";
    public static final String PPISSUE = "/sdcard/.photopickerIssue";
    public static final boolean ISTRACE = android.util.Log.isLoggable(TAG_PPDEBUG,
                                          android.util.Log.VERBOSE);
    // For identity ID in Glide
    public static final String ID_PP_GLIDE = "ID_PhotoPicker";
    public static final String PACKAGE_PHOTOPICKER = "com.mediatek.photopicker";
    public static final String ACTION_PP_PICK = "com.mediatek.action.photopicker.PICK";

    // For cursor performance optimization
    public static final Uri URIBASE = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    public static final String ORDER_ASC = "datetaken ASC";
    public static final String ORDER_DESC = "datetaken DESC";
    public static final String CUR_ID = Images.Media._ID;
    public static final String CUR_DATA = Images.Media.DATA;
    public static final String CUR_BUCKET_ID = Images.Media.BUCKET_ID;
    public static final String CUR_BUCKET_NAME = Images.Media.BUCKET_DISPLAY_NAME;

    private static Paint sTextPaint = null;
    private static Paint sStripPaint = null;
    private static Bitmap sFolderIcon = null;

    // Cache size
    public static final int ARRAY_SIZE = 80;
    // Visual item in gridView on screen
    public static final int SCREEN_VIS_SIZE = 18;
    // Show subtitle name under each item
    public static final int SUBTITLE_SIZE = 12;

    /**
     * Append albums into list after query database.
     * @param list The query result
     * @param context The context
     * @param projection null
     * @param selection null
     * @param selectionArgs null
     * @param sortOrder null
     * @param queryStart Query start position
     * @param queryEnd Query end position
     */
    public static void appendAlbumItems(List<Item> list, Context context,
            String[] projection, String selection, String[] selectionArgs, String sortOrder,
            int queryStart, int queryEnd) {
        Log.d(TAG, "<appendAlbumItems> " + "queryStart-queryEnd: " + queryStart + "-" + queryEnd);
        Uri uri = URIBASE.buildUpon()
                .appendQueryParameter("limit", queryStart + "," + (queryEnd - queryStart + 1))
                .build();
        projection = new String[] { "distinct " + CUR_BUCKET_ID, CUR_BUCKET_NAME };
        Cursor cursor = getCursor(context, uri, projection, selection, selectionArgs, sortOrder);
        if (!checkCursor(cursor)) {
            Log.d(TAG, "<appendAlbumItems> cursor invalid, do nothing.");
            return;
        }

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int bucketId = cursor.getInt(cursor.getColumnIndex(CUR_BUCKET_ID));
            String bucketName = cursor.getString(cursor.getColumnIndex(CUR_BUCKET_NAME));
            Log.d(TAG, "<appendAlbumItems> " + "bucketId & Name: " + bucketId + " " + bucketName);
            if (null != bucketName) {
                AlbumItem albumItem = new AlbumItem();
                albumItem.bucketId = bucketId;
                albumItem.bucketName = bucketName;
                list.add(albumItem);
            }
            cursor.moveToNext();
        }
        if (null != cursor) {
            cursor.close();
        }
        // Do query again
        projection = new String[] { CUR_ID, CUR_DATA, CUR_BUCKET_ID };
        String selectionOfItem = selection + " AND " + CUR_BUCKET_ID + " = ?";
        int size = selectionArgs.length;
        String [] selectionArgsOfItem = new String[size+1];
        for (int index = 0; index < size; index++) {
            selectionArgsOfItem[index] = selectionArgs[index];
        }
        for (Item item : list) {
            Log.d(TAG, "<appendAlbumItems> " + "list size: " + list.size());
            selectionArgsOfItem[size] = String.valueOf(((AlbumItem) item).bucketId);
            uri = URIBASE.buildUpon().appendQueryParameter("limit", 0 + "," + 1).build();
            cursor = getCursor(context, uri, projection,
                    selectionOfItem, selectionArgsOfItem, Utils.ORDER_DESC);
            if (!checkCursor(cursor)) {
                Log.d(TAG, "<appendAlbumItems> cursor invalid, do nothing.");
                return;
            }

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String path = cursor.getString(cursor.getColumnIndex(CUR_DATA));
                if (null != path) {
                    String id = cursor.getString(cursor.getColumnIndex(CUR_ID));
                    Uri uri2 = URIBASE.buildUpon().appendPath(id).build();
                    item.pathUri = uri2;
                }
                cursor.moveToNext();
            }
            if (null != cursor) {
                cursor.close();
            }
        }
    }

    /**
     * Append files into list after query database.
     * @param list The query result
     * @param context The context
     * @param projection null
     * @param selection null
     * @param selectionArgs null
     * @param sortOrder null
     * @param queryStart Query start position
     * @param queryEnd Query end position
     */
    public static void appendFileItems(List<Item> list, Context context,
            String[] projection, String selection, String[] selectionArgs, String sortOrder,
            int queryStart, int queryEnd) {
        Log.d(TAG, "<appendFileItems> queryStart & queryEnd: " + queryStart + " " + queryEnd);
        Uri uri = URIBASE.buildUpon()
                 .appendQueryParameter("limit", queryStart + "," + (queryEnd - queryStart + 1))
                 .build();
        FileItem fileItem = null;
        projection = new String[] { CUR_ID, CUR_DATA };
        Cursor cursor = getCursor(context, uri, projection, selection, selectionArgs, sortOrder);
        if (!checkCursor(cursor)) {
            Log.d(TAG, "<appendFileItems> cursor invalid, do nothing.");
            return;
        }

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String id = cursor.getString(cursor.getColumnIndex(CUR_ID));
            String filePath = cursor.getString(cursor.getColumnIndex(CUR_DATA));
            Uri uri2 = URIBASE.buildUpon().appendPath(id).build();
            Log.d(TAG, "<appendFileItems> uri2 & filePath: " + uri2 + " " + filePath);
            if (null != filePath) {
                fileItem = new FileItem();
                fileItem.pathUri = uri2;
                list.add(fileItem);
            }
            cursor.moveToNext();
        }
        if (null != cursor) {
            cursor.close();
        }
    }

    /**
     * Draw over lay on each item.
     * @param context Current context
     * @param canvas To be drew
     * @param title The text was shown at the corner of each item
     */
    public static void drawSlotLabel(Context context, Canvas canvas, String title) {
        // Basic setup for drawing
        float canvasSize = canvas.getHeight();
        canvas.translate(0, canvasSize);

        // Draw transparent strip
        RectF rect = new RectF(0, -canvasSize / 5, canvasSize, canvasSize);
        canvas.drawRect(rect, getStripPaint(context));

        // Draw icon
        rect = new RectF(0, -canvasSize / 5, canvasSize / 5, 0);
        canvas.drawBitmap(getBitmapIconFolder(context), null, rect, null);

        // Draw text
        canvas.drawText(title, canvasSize / 5, -canvasSize / 15, getTextPaint(context));
    }

    /**
     * Get instance of AlbumSetModelHook.
     * @param context Current context
     * @return AlbumSetModelHook
     */
    public static AlbumSetModelHook getAlbumSetModelHook(Context context) {
        return new AlbumSetModelHook(context);
    }

    /**
     * Get instance of AlbumModeHook.
     * @param context Current context
     * @param bucketId Click bucket_id
     * @return AlbumModeHook
     */
    public static AlbumModelHook getAlbumModelHook(Context context, int bucketId) {
        return new AlbumModelHook(context, bucketId);
    }

    /**
     * Get default viewHook.
     * @param context Current context
     * @return IViewHook
     */
    public static IViewHook getViewHook(Context context) {
        IViewHook viewHook = new DefaultViewHook(context);
        return viewHook;
    }

    /**
     * Get default controlHook.
     * @return IControlHook.
     */
    public static IControlHook getControlHook() {
        return new DefaultControlHook();
    }

    /**
     * Get cursor by context and conditions.
     * @param context Current context
     * @param uri null
     * @param projection null
     * @param selection null
     * @param selectionArgs null
     * @param sortOrder null
     * @return Cursor Return cursor
     */
    public static Cursor getCursor(Context context, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        if (ISTRACE) {
            Log.d(TAG, "<getCursor> " + "cursor infos as follow: " + "\n\t" +
                       "<getCursor> " + "projection: " + projection + "\n\t" +
                       "<getCursor> " + "selection: " + selection + "\n\t" +
                       "<getCursor> " + "selectionArgs: " + selectionArgs + "\n\t" +
                       "<getCursor> " + "sortOrder: " + sortOrder + "\n");
        }

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    sortOrder);
            Log.d(TAG, "<getCursor> cursor: " + cursor);
        } catch (IllegalStateException e) {
            Log.d(TAG, "<getCursor> query IllegalStateException: " + e.getMessage());
        } catch (SQLiteException e) {
            Log.d(TAG, "<getCursor> query SQLiteException: " + e.getMessage());
        }
        return cursor;
    }

    /**
     * For check the state of PhotoPicker.Config.
     * @param config PhotoPicker.Config
     */
    public static void debugConfig(Config config) {
        if (ISTRACE) {
            Log.d(TAG,  "<debugConfig> " + "config infos as follow: " + "\n\t" +
                        "<debugConfig> " + "config.titleId: " + config.title + "\n\t" +
                        "<debugConfig> " + "config.subTitleId: " + config.subTitle + "\n\t" +
                        "<debugConfig> " + "config.controlHook: " + config.controlHook + "\n\t" +
                        "<debugConfig> " + "config.viewHook: " + config.viewHook + "\n\t" +
                        "<debugConfig> " + "config.modelHook: " + config.modelHook + "\n");
        } else {
            Log.d(TAG, "<debugConfig> " + "ISDEBUG: " + ISTRACE + ", show nothing");
        }
    }

    /**
     * Launch PhotoPicker from 3rd apps, Can launch PhotoPicker as follow code.
     * {@code
     *      Intent intent = new Intent(ACTION_PP_PICK);
     *      startActivityForResult(intent, PhotoPicker.REQUEST_CODE);
     * }, Or luanch PhotoPicker by function {@code launchPhotoPicker3rd}
     * @param context Current context
     */
    public static void launchPhotoPicker3rd(Context context) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(PACKAGE_PHOTOPICKER, 0);
        } catch (NameNotFoundException e) {
            packageInfo = null;
            e.printStackTrace();
        }

        if (packageInfo == null) {
            Log.d(TAG, "<launchPhotoPicker3rd> " + PACKAGE_PHOTOPICKER + " haven't installed");
        } else {
            Intent resolveIntent = new Intent(ACTION_PP_PICK, null);
            resolveIntent.setPackage(packageInfo.packageName);
            Log.d(TAG, "<launchPhotoPicker3rd> " + "resolveIntent: " + resolveIntent);
            List<ResolveInfo> apps =
                    context.getPackageManager().queryIntentActivities(resolveIntent, 0);
            ResolveInfo resolveInfo = apps.iterator().next();

            if (resolveInfo != null) {
                Log.d(TAG, "<launchPhotoPicker3rd> " + "resolveInfo: " + resolveInfo);
                String packageNameTemp = resolveInfo.activityInfo.packageName;
                String className = resolveInfo.activityInfo.name;
                Log.d(TAG, "<launchPhotoPicker3rd> " + "packageNameTemp & className: " +
                        packageNameTemp + "-" + className);
                Intent intent = new Intent(ACTION_PP_PICK);

                ComponentName cn = new ComponentName(packageNameTemp, className);
                intent.setComponent(cn);
                ((Activity) context).startActivityForResult(intent, PhotoPicker.REQUEST_CODE);
            }
        }
    }

    /**
     * Return a resource identifier for the given resource name in PhotoPicker package.
     * @param context the Android Context
     * @param name resource name
     * @param defType resource type
     * @return resource identifier
     */
    public static int getMyResId(Context context, String name, String defType) {
        return getMyResources(context).getIdentifier(name, defType, PACKAGE_PHOTOPICKER);
    }

    /**
     * Get view by inflating a layout file in PhotoPicker package.<br/>
     * Warning: layout is unsafe to write in xml file for a library PhotoPicker.<br/>
     * e.g. it's unsafe to use xml properties defined in non-android namespace.
     * @param context the Android Context
     * @param name layout file name
     * @return view for the layout file
     */
    public static View getMyLayout(Context context, String name) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(
                getMyResources(context).getLayout(
                        getMyResId(context, name, "layout")), null);
        return view;
    }

    /**
     * Get drawable in PhotoPicker package.
     * @param context the Android Context
     * @param name drawable name
     * @return drawable of the given name
     */
    public static Drawable getMyDrawable(Context context, String name) {
        int id = getMyResId(context, name, "drawable");
        return getMyResources(context).getDrawable(id);
    }

    /**
     * Get string in PhotoPicker package.
     * @param context the Android Context
     * @param name string name
     * @return string of the given name
     */
    public static String getMyString(Context context, String name) {
        int id = getMyResId(context, name, "string");
        return getMyResources(context).getString(id);
    }

    /**
     * Get dimension in PhotoPicker package.
     * @param context the Android Context
     * @param name dimension name
     * @return dimension of the given name
     */
    public static float getMyDimension(Context context, String name) {
        int id = getMyResId(context, name, "dimen");
        return getMyResources(context).getDimension(id);
    }

    /**
     * Get dimension in PhotoPicker package.
     * @param context the Android Context
     * @param name dimension name
     * @return dimension of the given name
     */
    public static int getMyDimensionPixelSize(Context context, String name) {
        int id = getMyResId(context, name, "dimen");
        return getMyResources(context).getDimensionPixelSize(id);
    }

    /**
     * Get color in PhotoPicker package.
     * @param context the Android Context
     * @param name color name
     * @return color of the given name
     */
    public static int getMyColor(Context context, String name) {
        int id = getMyResId(context, name, "color");
        return getMyResources(context).getColor(id);
    }

    private static Resources getMyResources(Context context) {
        Resources myResources = null;
        PackageManager pm = context.getPackageManager();
        try {
            myResources = pm.getResourcesForApplication(PACKAGE_PHOTOPICKER);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "No resources found in PhotoPicker");
        }
        return myResources;
    }

    private static Paint getTextPaint(Context context) {
        if (null == sTextPaint) {
            sTextPaint = new Paint();
            sTextPaint.setAntiAlias(true);
            sTextPaint.setColor(getMyColor(context, "label_text"));
            sTextPaint.setTextSize(getMyDimension(context, "label_font_size"));
            sTextPaint.setStrokeWidth(1.5f);
        }
        return sTextPaint;
    }

    private static Paint getStripPaint(Context context) {
        if (null == sStripPaint) {
            Resources res = getMyResources(context);
            sStripPaint = new Paint();
            int colorId = getMyResId(context, "label_background", "color");
            sStripPaint.setColor(res.getColor(colorId));
        }
        return sStripPaint;
    }

    private static Bitmap getBitmapIconFolder(Context context) {
        if (null == sFolderIcon) {
            int id = getMyResId(context, "ic_folder", "drawable");
            Resources res = getMyResources(context);
            sFolderIcon = BitmapFactory.decodeResource(res, id);
        }
        return sFolderIcon;
    }

    private static boolean checkCursor(Cursor cursor) {
        if (cursor == null ) {
            Log.d(TAG, "<checkCursor> cursor is null, do nothing");
            return false;
        } else {
            if (!cursor.moveToFirst()) {
                Log.d(TAG, "<checkCursor> cursor is not null, but it's empty.");
                return false;
            }
            return true;
        }
    }
}
