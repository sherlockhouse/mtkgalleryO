package com.mediatek.galleryfeature.stereo.segment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.mediatek.galleryfeature.stereo.segment.refine.SourceImagePicker;
import com.mediatek.util.Log;

/**
 * Activity for guiding the user to pick a source image for copying, <br/>
 * and direct the flow to refine or synth according to users' choice.
 */
public class GuideActivity extends Activity {
    private static final String TAG = Log.Tag("Cp/GuideActivity");

    private static final int REQUEST_PICK_SRC_IMG = 131420;
    private static final int REQUEST_COPY_PASTE = 201324;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, SourceImagePicker.class);
        startActivityForResult(intent, REQUEST_PICK_SRC_IMG);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_SRC_IMG) {
                Uri copySourceUri = data.getData();
                Uri pasteDestUri = getIntent().getData();
                if (copySourceUri == null || pasteDestUri == null) {
                    Log.e(TAG, "<onActivityResult> copySourceUri or pasteDestUri maybe null");
                    finish();
                }
                boolean needRefine = data.getBooleanExtra(
                        SourceImagePicker.KEY_IS_DEPTH_IMAGE, false);
                Log.d(TAG, "<onActivityResult> src=" + copySourceUri.toString() + ", dest="
                        + pasteDestUri.toString() + ", needRefine=" + needRefine);

                Intent intent = new Intent();
                if (needRefine) {
                    intent.setAction("com.mediatek.segment.action.REFINE");
                    intent.setDataAndType(copySourceUri, getIntent().getType()).setFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.putExtra("COPY_DEST_URI", pasteDestUri.toString());
                } else {
                    // TODO use explicit start, since synth is for private usage
                    intent.setAction("com.mediatek.segment.action.SYNTH");
                    intent.putExtra("COPY_SRC_URI", copySourceUri.toString());
                    intent.setDataAndType(pasteDestUri, getIntent().getType()).setFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                startActivityForResult(intent, REQUEST_COPY_PASTE);
            } else if (requestCode == REQUEST_COPY_PASTE) {
                Uri mergedResultUri = data.getData();
                Log.d(TAG, "<onActivityResult> generated image:" + mergedResultUri);
                setResult(RESULT_OK, new Intent().setData(mergedResultUri));
                finish();
            }
        } else {
            Log.d(TAG, "<onActivityResult> canceled");
            finish();
        }
    }
}
