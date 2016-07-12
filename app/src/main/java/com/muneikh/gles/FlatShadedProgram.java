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

import android.opengl.GLES20;
import android.util.Log;

import java.nio.FloatBuffer;

/**
 * GL program and supporting functions for flat-shaded rendering.
 */
public class FlatShadedProgram {
    private static final String TAG = GlUtil.TAG;

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 aPosition;" +
                    "void main() {" +
                    "    gl_Position = uMVPMatrix * aPosition;" +
                    "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
                    "uniform vec4 uColor;" +
                    "void main() {" +
                    "    gl_FragColor = uColor;" +
                    "}";

    // Handles to the GL program and various components of it.
    private int programHandle = -1;
    private int colorLoc = -1;
    private int mvpMatrixLoc = -1;
    private int positionLoc = -1;


    /**
     * Prepares the program in the current EGL context.
     */
    public FlatShadedProgram() {
        programHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (programHandle == 0) {
            throw new RuntimeException("Unable to create program");
        }
        Log.i(TAG, "Created program " + programHandle);

        // get locations of attributes and uniforms

        positionLoc = GLES20.glGetAttribLocation(programHandle, "aPosition");
        GlUtil.checkLocation(positionLoc, "aPosition");
        mvpMatrixLoc = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix");
        GlUtil.checkLocation(mvpMatrixLoc, "uMVPMatrix");
        colorLoc = GLES20.glGetUniformLocation(programHandle, "uColor");
        GlUtil.checkLocation(colorLoc, "uColor");
    }

    /**
     * Releases the program.
     */
    public void release() {
        GLES20.glDeleteProgram(programHandle);
        programHandle = -1;
    }

    /**
     * Issues the draw call.  Does the full setup on every call.
     *
     * @param mvpMatrix       The 4x4 projection matrix.
     * @param color           A 4-element color vector.
     * @param vertexBuffer    Buffer with vertex data.
     * @param firstVertex     Index of first vertex to use in vertexBuffer.
     * @param vertexCount     Number of vertices in vertexBuffer.
     * @param coordsPerVertex The number of coordinates per vertex (e.g. x,y is 2).
     * @param vertexStride    Width, in bytes, of the data for each vertex (often vertexCount *
     *                        sizeof(float)).
     */
    public void draw(float[] mvpMatrix, float[] color, FloatBuffer vertexBuffer,
                     int firstVertex, int vertexCount, int coordsPerVertex, int vertexStride) {
        GlUtil.checkGlError("draw start");

        // Select the program.
        GLES20.glUseProgram(programHandle);
        GlUtil.checkGlError("glUseProgram");

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(mvpMatrixLoc, 1, false, mvpMatrix, 0);
        GlUtil.checkGlError("glUniformMatrix4fv");

        // Copy the color vector in.
        GLES20.glUniform4fv(colorLoc, 1, color, 0);
        GlUtil.checkGlError("glUniform4fv ");

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(positionLoc);
        GlUtil.checkGlError("glEnableVertexAttribArray");

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(positionLoc, coordsPerVertex,
                GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        GlUtil.checkGlError("glVertexAttribPointer");

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
        GlUtil.checkGlError("glDrawArrays");

        // Done -- disable vertex array and program.
        GLES20.glDisableVertexAttribArray(positionLoc);
        GLES20.glUseProgram(0);
    }
}
