/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.freeview3d.renderer;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.mediatek.util.Log;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import javax.microedition.khronos.opengles.GL11;

/*
 * The GL canvas for freeview.
 */
public class GLES20Canvas {
    // ************** Constants **********************
    private static final String TAG = Log.Tag("Fv/GLES20Canvas");

    private static final int FLOAT_SIZE = Float.SIZE / Byte.SIZE;
    private static final float OPAQUE_ALPHA = 0.95f;

    private static final int COORDS_PER_VERTEX = 2;
    private static final int VERTEX_STRIDE = COORDS_PER_VERTEX * FLOAT_SIZE;

    private static final int COUNT_FILL_VERTEX = 4;
    private static final int OFFSET_FILL_RECT = 0;

    public static final int SAVE_FLAG_ALL = 0xFFFFFFFF;
    public static final int SAVE_FLAG_ALPHA = 0x01;
    public static final int SAVE_FLAG_MATRIX = 0x02;

    private static final float[] BOX_COORDINATES = {
            0, 0, // Fill rectangle
            1, 0,
            0, 1,
            1, 1,
            0, 0, // Draw line
            1, 1,
            0, 0, // Draw rectangle outline
            0, 1,
            1, 1,
            1, 0,
    };

    private static final String POSITION_ATTRIBUTE = "aPosition";
    private static final String COLOR_UNIFORM = "uColor";
    private static final String MATRIX_UNIFORM = "uMatrix";
    private static final String TEXTURE_MATRIX_UNIFORM = "uTextureMatrix";
    private static final String TEXTURE_SAMPLER_UNIFORM = "uTextureSampler";
    private static final String ALPHA_UNIFORM = "uAlpha";

    private static final String TEXTURE_VERTEX_SHADER = ""
            + "uniform mat4 " + MATRIX_UNIFORM + ";\n"
            + "uniform mat4 " + TEXTURE_MATRIX_UNIFORM + ";\n"
            + "attribute vec2 " + POSITION_ATTRIBUTE + ";\n"
            + "varying vec2 vTextureCoord;\n"
            + "void main() {\n"
            + "  vec4 pos = vec4(" + POSITION_ATTRIBUTE + ", 0.0, 1.0);\n"
            + "  gl_Position = " + MATRIX_UNIFORM + " * pos;\n"
            + "  vTextureCoord = (" + TEXTURE_MATRIX_UNIFORM + " * pos).xy;\n"
            + "}\n";

    private static final String TEXTURE_FRAGMENT_SHADER = ""
            + "precision mediump float;\n"
            + "varying vec2 vTextureCoord;\n"
            + "uniform float " + ALPHA_UNIFORM + ";\n"
            + "uniform sampler2D " + TEXTURE_SAMPLER_UNIFORM + ";\n"
            + "void main() {\n"
            + "  gl_FragColor = texture2D(" + TEXTURE_SAMPLER_UNIFORM + ", vTextureCoord);\n"
            + "  gl_FragColor *= " + ALPHA_UNIFORM + ";\n"
            + "}\n";

    private static final int INITIAL_RESTORE_STATE_SIZE = 8;
    private static final int MATRIX_SIZE = 16;

    // Keep track of restore state
    private float[] mMatrices = new float[INITIAL_RESTORE_STATE_SIZE * MATRIX_SIZE];
    private float[] mAlphas = new float[INITIAL_RESTORE_STATE_SIZE];
    private IntArray mSaveFlags = new IntArray();

    private int mCurrentAlphaIndex = 0;
    private int mCurrentMatrixIndex = 0;

    // Viewport size
    private int mWidth;
    private int mHeight;

    // Projection matrix
    private float[] mProjectionMatrix = new float[MATRIX_SIZE];

    // Screen size for when we aren't bound to a texture
    private int mScreenWidth;
    private int mScreenHeight;

    private int mTextureProgram;

    // GL buffer containing BOX_COORDINATES
    private int mBoxCoordinates;

    // Handle indices -- common
    private static final int INDEX_POSITION = 0;
    private static final int INDEX_MATRIX = 1;

    // Handle indices -- texture
    private static final int INDEX_TEXTURE_MATRIX = 2;
    private static final int INDEX_TEXTURE_SAMPLER = 3;
    private static final int INDEX_ALPHA = 4;


    private abstract static class ShaderParameter {
        public int handle;
        protected final String mName;

        public ShaderParameter(String name) {
            mName = name;
        }

        public abstract void loadHandle(int program);
    }

    private static class UniformShaderParameter extends ShaderParameter {
        public UniformShaderParameter(String name) {
            super(name);
        }

        @Override
        public void loadHandle(int program) {
            handle = GLES20.glGetUniformLocation(program, mName);
            checkError();
        }
    }

    private static class AttributeShaderParameter extends ShaderParameter {
        public AttributeShaderParameter(String name) {
            super(name);
        }

        @Override
        public void loadHandle(int program) {
            handle = GLES20.glGetAttribLocation(program, mName);
            checkError();
        }
    }

    ShaderParameter[] mDrawParameters = {
            new AttributeShaderParameter(POSITION_ATTRIBUTE), // INDEX_POSITION
            new UniformShaderParameter(MATRIX_UNIFORM), // INDEX_MATRIX
            new UniformShaderParameter(COLOR_UNIFORM), // INDEX_COLOR
    };
    ShaderParameter[] mTextureParameters = {
            new AttributeShaderParameter(POSITION_ATTRIBUTE), // INDEX_POSITION
            new UniformShaderParameter(MATRIX_UNIFORM), // INDEX_MATRIX
            new UniformShaderParameter(TEXTURE_MATRIX_UNIFORM), // INDEX_TEXTURE_MATRIX
            new UniformShaderParameter(TEXTURE_SAMPLER_UNIFORM), // INDEX_TEXTURE_SAMPLER
            new UniformShaderParameter(ALPHA_UNIFORM), // INDEX_ALPHA
    };

    private final IntArray mUnboundTextures = new IntArray();

    // Buffer for framebuffer IDs -- we keep track so we can switch the attached
    // texture.
    private int[] mFrameBuffer = new int[1];

    // Bound textures.
    private ArrayList<BasicTexture> mTargetTextures = new ArrayList<BasicTexture>();

    // Temporary variables used within calculations
    private final float[] mTempMatrix = new float[32];
    private final RectF mTempSourceRect = new RectF();
    private final RectF mTempTargetRect = new RectF();
    private final float[] mTempTextureMatrix = new float[MATRIX_SIZE];
    private final int[] mTempIntArray = new int[1];

    public GLES20Canvas() {
        Matrix.setIdentityM(mTempTextureMatrix, 0);
        Matrix.setIdentityM(mMatrices, mCurrentMatrixIndex);
        mAlphas[mCurrentAlphaIndex] = 1f;
        mTargetTextures.add(null);

        FloatBuffer boxBuffer = createBuffer(BOX_COORDINATES);
        mBoxCoordinates = uploadBuffer(boxBuffer);

        int textureVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, TEXTURE_VERTEX_SHADER);
        int textureFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, TEXTURE_FRAGMENT_SHADER);

        mTextureProgram = assembleProgram(textureVertexShader, textureFragmentShader,
                mTextureParameters);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        checkError();
    }

    private static FloatBuffer createBuffer(float[] values) {
        // First create an nio buffer, then create a VBO from it.
        int size = values.length * FLOAT_SIZE;
        FloatBuffer buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(values, 0, values.length).position(0);
        return buffer;
    }

    private int assembleProgram(int vertexShader, int fragmentShader, ShaderParameter[] params) {
        int program = GLES20.glCreateProgram();
        checkError();
        if (program == 0) {
            throw new RuntimeException("Cannot create GL program: " + GLES20.glGetError());
        }
        GLES20.glAttachShader(program, vertexShader);
        checkError();
        GLES20.glAttachShader(program, fragmentShader);
        checkError();
        GLES20.glLinkProgram(program);
        checkError();
        int[] mLinkStatus = mTempIntArray;
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, mLinkStatus, 0);
        if (mLinkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        for (int i = 0; i < params.length; i++) {
            params[i].loadHandle(program);
        }
        return program;
    }

    private static int loadShader(int type, String shaderCode) {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        checkError();
        GLES20.glCompileShader(shader);
        checkError();

        return shader;
    }

    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
        GLES20.glViewport(0, 0, mWidth, mHeight);
        checkError();
        Matrix.setIdentityM(mMatrices, mCurrentMatrixIndex);
        Matrix.orthoM(mProjectionMatrix, 0, 0, width, 0, height, -1, 1);
        if (getTargetTexture() == null) {
            mScreenWidth = width;
            mScreenHeight = height;
            Matrix.translateM(mMatrices, mCurrentMatrixIndex, 0, height, 0);
            Matrix.scaleM(mMatrices, mCurrentMatrixIndex, 1, -1, 1);
        }
    }

    public float getAlpha() {
        return mAlphas[mCurrentAlphaIndex];
    }

    public void setAlpha(float alpha) {
        mAlphas[mCurrentAlphaIndex] = alpha;
    }

    public void multiplyAlpha(float alpha) {
        setAlpha(getAlpha() * alpha);
    }

    public void translate(float x, float y, float z) {
        Matrix.translateM(mMatrices, mCurrentMatrixIndex, x, y, z);
    }

    // This is a faster version of translate(x, y, z) because
    // (1) we knows z = 0, (2) we inline the Matrix.translateM call,
    // (3) we unroll the loop
    public void translate(float x, float y) {
        int index = mCurrentMatrixIndex;
        float[] m = mMatrices;
        m[index + 12] += m[index + 0] * x + m[index + 4] * y;
        m[index + 13] += m[index + 1] * x + m[index + 5] * y;
        m[index + 14] += m[index + 2] * x + m[index + 6] * y;
        m[index + 15] += m[index + 3] * x + m[index + 7] * y;
    }

    public void scale(float sx, float sy, float sz) {
        Matrix.scaleM(mMatrices, mCurrentMatrixIndex, sx, sy, sz);
    }

    public void rotate(float angle, float x, float y, float z) {
        if (angle == 0f) {
            return;
        }
        float[] temp = mTempMatrix;
        Matrix.setRotateM(temp, 0, angle, x, y, z);
        float[] matrix = mMatrices;
        int index = mCurrentMatrixIndex;
        Matrix.multiplyMM(temp, MATRIX_SIZE, matrix, index, temp, 0);
        System.arraycopy(temp, MATRIX_SIZE, matrix, index, MATRIX_SIZE);
    }


    public void save() {
        save(SAVE_FLAG_ALL);
    }

    public void save(int saveFlags) {
        boolean saveAlpha = (saveFlags & SAVE_FLAG_ALPHA) == SAVE_FLAG_ALPHA;
        if (saveAlpha) {
            float currentAlpha = getAlpha();
            mCurrentAlphaIndex++;
            if (mAlphas.length <= mCurrentAlphaIndex) {
                mAlphas = Arrays.copyOf(mAlphas, mAlphas.length * 2);
            }
            mAlphas[mCurrentAlphaIndex] = currentAlpha;
        }
        boolean saveMatrix = (saveFlags & SAVE_FLAG_MATRIX) == SAVE_FLAG_MATRIX;
        if (saveMatrix) {
            int currentIndex = mCurrentMatrixIndex;
            mCurrentMatrixIndex += MATRIX_SIZE;
            if (mMatrices.length <= mCurrentMatrixIndex) {
                mMatrices = Arrays.copyOf(mMatrices, mMatrices.length * 2);
            }
            System.arraycopy(mMatrices, currentIndex, mMatrices, mCurrentMatrixIndex, MATRIX_SIZE);
        }
        mSaveFlags.add(saveFlags);
    }

    public void restore() {
        int restoreFlags = mSaveFlags.removeLast();
        boolean restoreAlpha = (restoreFlags & SAVE_FLAG_ALPHA) == SAVE_FLAG_ALPHA;
        if (restoreAlpha) {
            mCurrentAlphaIndex--;
        }
        boolean restoreMatrix = (restoreFlags & SAVE_FLAG_MATRIX) == SAVE_FLAG_MATRIX;
        if (restoreMatrix) {
            mCurrentMatrixIndex -= MATRIX_SIZE;
        }
    }

    private void enableBlending(boolean enableBlending) {
        if (enableBlending) {
            GLES20.glEnable(GLES20.GL_BLEND);
            checkError();
        } else {
            GLES20.glDisable(GLES20.GL_BLEND);
            checkError();
        }
    }

    private void setPosition(ShaderParameter[] params, int offset) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mBoxCoordinates);
        checkError();
        GLES20.glVertexAttribPointer(params[INDEX_POSITION].handle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, VERTEX_STRIDE, offset * VERTEX_STRIDE);
        checkError();
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        checkError();
    }

    private void draw(ShaderParameter[] params, int type, int count, float x, float y, float width,
            float height) {
        setMatrix(params, x, y, width, height);
        int positionHandle = params[INDEX_POSITION].handle;
        GLES20.glEnableVertexAttribArray(positionHandle);
        checkError();
        GLES20.glDrawArrays(type, 0, count);
        checkError();
        GLES20.glDisableVertexAttribArray(positionHandle);
        checkError();
    }

    private void setMatrix(ShaderParameter[] params, float x, float y, float width, float height) {
        Matrix.translateM(mTempMatrix, 0, mMatrices, mCurrentMatrixIndex, x, y, 0f);
        Matrix.scaleM(mTempMatrix, 0, width, height, 1f);
        Matrix.multiplyMM(mTempMatrix, MATRIX_SIZE, mProjectionMatrix, 0, mTempMatrix, 0);
        GLES20.glUniformMatrix4fv(params[INDEX_MATRIX].handle, 1, false, mTempMatrix, MATRIX_SIZE);
        checkError();
    }

    public void drawTexture(BasicTexture texture, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        copyTextureCoordinates(texture, mTempSourceRect);
        mTempTargetRect.set(x, y, x + width, y + height);
        convertCoordinate(mTempSourceRect, mTempTargetRect, texture);
        drawTextureRect(texture, mTempSourceRect, mTempTargetRect);
    }

    private static void copyTextureCoordinates(BasicTexture texture, RectF outRect) {
        int left = 0;
        int top = 0;
        int right = texture.getWidth();
        int bottom = texture.getHeight();
        outRect.set(left, top, right, bottom);
    }

    public void drawTexture(BasicTexture texture, RectF source, RectF target) {
        if (target.width() <= 0 || target.height() <= 0) {
            return;
        }
        mTempSourceRect.set(source);
        mTempTargetRect.set(target);

        convertCoordinate(mTempSourceRect, mTempTargetRect, texture);
        drawTextureRect(texture, mTempSourceRect, mTempTargetRect);
    }

    public void drawTexture(BasicTexture texture, float[] textureTransform, int x, int y, int w,
            int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        mTempTargetRect.set(x, y, x + w, y + h);
        drawTextureRect(texture, textureTransform, mTempTargetRect);
    }

    private void drawTextureRect(BasicTexture texture, RectF source, RectF target) {
        setTextureMatrix(source);
        drawTextureRect(texture, mTempTextureMatrix, target);
    }

    private void setTextureMatrix(RectF source) {
        mTempTextureMatrix[0] = source.width();
        mTempTextureMatrix[5] = source.height();
        mTempTextureMatrix[12] = source.left;
        mTempTextureMatrix[13] = source.top;
    }

    // This function changes the source coordinate to the texture coordinates.
    // It also clips the source and target coordinates if it is beyond the
    // bound of the texture.
    private static void convertCoordinate(RectF source, RectF target, BasicTexture texture) {
        int width = texture.getWidth();
        int height = texture.getHeight();
        int texWidth = texture.getTextureWidth();
        int texHeight = texture.getTextureHeight();
        // Convert to texture coordinates
        source.left /= texWidth;
        source.right /= texWidth;
        source.top /= texHeight;
        source.bottom /= texHeight;

        // Clip if the rendering range is beyond the bound of the texture.
        float xBound = (float) width / texWidth;
        if (source.right > xBound) {
            target.right = target.left + target.width() * (xBound - source.left) / source.width();
            source.right = xBound;
        }
        float yBound = (float) height / texHeight;
        if (source.bottom > yBound) {
            target.bottom = target.top + target.height() * (yBound - source.top) / source.height();
            source.bottom = yBound;
        }
    }

    private void drawTextureRect(BasicTexture texture, float[] textureMatrix, RectF target) {
        ShaderParameter[] params = prepareTexture(texture);
        setPosition(params, OFFSET_FILL_RECT);
        GLES20.glUniformMatrix4fv(params[INDEX_TEXTURE_MATRIX].handle, 1, false, textureMatrix, 0);
        checkError();
        draw(params, GLES20.GL_TRIANGLE_STRIP, COUNT_FILL_VERTEX, target.left, target.top,
                target.width(), target.height());
    }

    private ShaderParameter[] prepareTexture(BasicTexture texture) {
        ShaderParameter[] params;
        int program;
        params = mTextureParameters;
        program = mTextureProgram;
        prepareTexture(texture, program, params);
        return params;
    }

    private void prepareTexture(BasicTexture texture, int program, ShaderParameter[] params) {
        GLES20.glUseProgram(program);
        checkError();
        enableBlending(getAlpha() < OPAQUE_ALPHA);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        checkError();
        texture.onBind(this);
        GLES20.glBindTexture(texture.getTarget(), texture.getId());
        checkError();
        GLES20.glUniform1i(params[INDEX_TEXTURE_SAMPLER].handle, 0);
        checkError();
        GLES20.glUniform1f(params[INDEX_ALPHA].handle, getAlpha());
        checkError();
    }

    public boolean unloadTexture(BasicTexture texture) {
        boolean unload = texture.isLoaded();
        if (unload) {
            synchronized (mUnboundTextures) {
                mUnboundTextures.add(texture.getId());
            }
        }
        return unload;
    }

    public void deleteRecycledResources() {
        synchronized (mUnboundTextures) {
            IntArray ids = mUnboundTextures;
            if (mUnboundTextures.size() > 0) {
                glDeleteTextures(null, ids.size(), ids.getInternalArray(), 0);
                ids.clear();
            }
        }
    }

    public void endRenderTarget() {
        BasicTexture oldTexture = mTargetTextures.remove(mTargetTextures.size() - 1);
        BasicTexture texture = getTargetTexture();
        setRenderTarget(oldTexture, texture);
        restore(); // restore matrix and alpha
    }

    public void beginRenderTarget(BasicTexture texture) {
        save(); // save matrix and alpha and blending
        BasicTexture oldTexture = getTargetTexture();
        mTargetTextures.add(texture);
        setRenderTarget(oldTexture, texture);
    }

    private BasicTexture getTargetTexture() {
        return mTargetTextures.get(mTargetTextures.size() - 1);
    }

    private void setRenderTarget(BasicTexture oldTexture, BasicTexture texture) {
        if (oldTexture == null && texture != null) {
            GLES20.glGenFramebuffers(1, mFrameBuffer, 0);
            checkError();
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer[0]);
            checkError();
        } else if (oldTexture != null && texture == null) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            checkError();
            GLES20.glDeleteFramebuffers(1, mFrameBuffer, 0);
            checkError();
        }

        if (texture == null) {
            setSize(mScreenWidth, mScreenHeight);
        } else {
            setSize(texture.getWidth(), texture.getHeight());

            if (!texture.isLoaded()) {
                texture.prepare(this);
            }

            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    texture.getTarget(), texture.getId(), 0);
            checkError();

            checkFramebufferStatus();
        }
    }

    private static void checkFramebufferStatus() {
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            String msg = "";
            switch (status) {
                case GLES20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                    msg = "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
                    break;
                case GLES20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
                    msg = "GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS";
                    break;
                case GLES20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                    msg = "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
                    break;
                case GLES20.GL_FRAMEBUFFER_UNSUPPORTED:
                    msg = "GL_FRAMEBUFFER_UNSUPPORTED";
                    break;
            }
            throw new RuntimeException(msg + ":" + Integer.toHexString(status));
        }
    }

    public void setTextureParameters(BasicTexture texture) {
        int target = texture.getTarget();
        GLES20.glBindTexture(target, texture.getId());
        checkError();
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    }

    public void initializeTextureSize(BasicTexture texture, int format, int type) {
        int target = texture.getTarget();
        GLES20.glBindTexture(target, texture.getId());
        checkError();
        int width = texture.getTextureWidth();
        int height = texture.getTextureHeight();
        GLES20.glTexImage2D(target, 0, format, width, height, 0, format, type, null);
    }

    public void initializeTexture(BasicTexture texture, Bitmap bitmap) {
        int target = texture.getTarget();
        GLES20.glBindTexture(target, texture.getId());
        checkError();
        GLUtils.texImage2D(target, 0, bitmap, 0);
    }

    public void texSubImage2D(BasicTexture texture, int xOffset,
        int yOffset, Bitmap bitmap, int format, int type) {
        int target = texture.getTarget();
        GLES20.glBindTexture(target, texture.getId());
        checkError();
        GLUtils.texSubImage2D(target, 0, xOffset, yOffset, bitmap, format, type);
    }

    public int uploadBuffer(FloatBuffer buf) {
        return uploadBuffer(buf, FLOAT_SIZE);
    }

    public int uploadBuffer(ByteBuffer buf) {
        return uploadBuffer(buf, 1);
    }

    private int uploadBuffer(Buffer buffer, int elementSize) {
        glGenBuffers(1, mTempIntArray, 0);
        checkError();
        int bufferId = mTempIntArray[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId);
        checkError();
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, buffer.capacity() * elementSize, buffer,
                GLES20.GL_STATIC_DRAW);
        checkError();
        return bufferId;
    }

    public static void checkError() {
        int error = GLES20.glGetError();
        if (error != 0) {
            Throwable t = new Throwable();
            Log.e(TAG, "GL error: " + error, t);
        }
    }

    public int generateTexture() {
        GLES20.glGenTextures(1, mTempIntArray, 0);
        GLES20Canvas.checkError();
        return mTempIntArray[0];
    }

    public void glGenBuffers(int n, int[] buffers, int offset) {
        GLES20.glGenBuffers(n, buffers, offset);
        GLES20Canvas.checkError();
    }

    public void glDeleteTextures(GL11 gl, int n, int[] textures, int offset) {
        GLES20.glDeleteTextures(n, textures, offset);
        GLES20Canvas.checkError();
    }

    public Bitmap saveTexture(int texture, int width, int height) {
        ByteBuffer buffer = ByteBuffer.allocate(width * height * 4);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                buffer);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }
}
