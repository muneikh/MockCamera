/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.muneikh.gles;

import android.opengl.Matrix;

/**
 * Base class for a 2d object.  Includes position, scale, rotation, and flat-shaded color.
 */
public class Sprite2d {
    private static final String TAG = GlUtil.TAG;

    private Drawable2d drawable;
    private float color[];
    private int textureId;
    private float angle;
    private float scaleX, scaleY;
    private float posX, posY;

    private float[] modelViewMatrix;
    private boolean matrixReady;

    private float[] scratchMatrix = new float[16];

    public Sprite2d(Drawable2d drawable) {
        this.drawable = drawable;
        color = new float[4];
        color[3] = 1.0f;
        textureId = -1;

        modelViewMatrix = new float[16];
        matrixReady = false;
    }

    /**
     * Re-computes modelViewMatrix, based on the current values for rotation, scale, and
     * translation.
     */
    private void recomputeMatrix() {
        float[] modelView = modelViewMatrix;

        Matrix.setIdentityM(modelView, 0);
        Matrix.translateM(modelView, 0, posX, posY, 0.0f);
        if (angle != 0.0f) {
            Matrix.rotateM(modelView, 0, angle, 0.0f, 0.0f, 1.0f);
        }
        Matrix.scaleM(modelView, 0, scaleX, scaleY, 1.0f);
        matrixReady = true;
    }

    /**
     * Returns the sprite scale along the X axis.
     */
    public float getScaleX() {
        return scaleX;
    }

    /**
     * Returns the sprite scale along the Y axis.
     */
    public float getScaleY() {
        return scaleY;
    }

    /**
     * Sets the sprite scale (size).
     */
    public void setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        matrixReady = false;
    }

    /**
     * Gets the sprite rotation angle, in degrees.
     */
    public float getRotation() {
        return angle;
    }

    /**
     * Sets the sprite rotation angle, in degrees.  Sprite will rotate counter-clockwise.
     */
    public void setRotation(float angle) {
        // Normalize.  We're not expecting it to be way off, so just iterate.
        while (angle >= 360.0f) {
            angle -= 360.0f;
        }
        while (angle <= -360.0f) {
            angle += 360.0f;
        }
        this.angle = angle;
        matrixReady = false;
    }

    /**
     * Returns the position on the X axis.
     */
    public float getPositionX() {
        return posX;
    }

    /**
     * Returns the position on the Y axis.
     */
    public float getPositionY() {
        return posY;
    }

    /**
     * Sets the sprite position.
     */
    public void setPosition(float posX, float posY) {
        this.posX = posX;
        this.posY = posY;
        matrixReady = false;
    }

    /**
     * Returns the model-view matrix.
     * <p/>
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    public float[] getModelViewMatrix() {
        if (!matrixReady) {
            recomputeMatrix();
        }
        return modelViewMatrix;
    }

    /**
     * Sets color to use for flat-shaded rendering.  Has no effect on textured rendering.
     */
    public void setColor(float red, float green, float blue) {
        color[0] = red;
        color[1] = green;
        color[2] = blue;
    }

    /**
     * Sets texture to use for textured rendering.  Has no effect on flat-shaded rendering.
     */
    public void setTexture(int textureId) {
        this.textureId = textureId;
    }

    /**
     * Returns the color.
     * <p/>
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    public float[] getColor() {
        return color;
    }

    /**
     * Draws the rectangle with the supplied program and projection matrix.
     */
    public void draw(FlatShadedProgram program, float[] projectionMatrix) {
        // Compute model/view/projection matrix.
        Matrix.multiplyMM(scratchMatrix, 0, projectionMatrix, 0, getModelViewMatrix(), 0);

        program.draw(scratchMatrix, color, drawable.getVertexArray(), 0,
                drawable.getVertexCount(), drawable.getCoordsPerVertex(),
                drawable.getVertexStride());
    }

    /**
     * Draws the rectangle with the supplied program and projection matrix.
     */
    public void draw(Texture2dProgram program, float[] projectionMatrix) {
        // Compute model/view/projection matrix.
        Matrix.multiplyMM(scratchMatrix, 0, projectionMatrix, 0, getModelViewMatrix(), 0);

        program.draw(scratchMatrix, drawable.getVertexArray(), 0,
                drawable.getVertexCount(), drawable.getCoordsPerVertex(),
                drawable.getVertexStride(), GlUtil.IDENTITY_MATRIX, drawable.getTexCoordArray(),
                textureId, drawable.getTexCoordStride());
    }

    @Override
    public String toString() {
        return "[Sprite2d pos=" + posX + "," + posY +
                " scale=" + scaleX + "," + scaleY + " angle=" + angle +
                " color={" + color[0] + "," + color[1] + "," + color[2] +
                "} drawable=" + drawable + "]";
    }
}
