package com.lg.specialcamera.filter;

import android.content.Context;
import android.util.Log;


import com.lg.specialcamera.R;
import com.lg.specialcamera.face.Face;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform2fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

public class BigEyeFilter extends BaseFrameFilter {

    String TAG= BigEyeFilter.class.getSimpleName();

    private final int left_eye;
    private final int right_eye;
    private FloatBuffer left;
    private FloatBuffer right;
    private Face mFace;


    public BigEyeFilter(Context context) {
        super(context, R.raw.base_vertex, R.raw.bigeye_fragment);
        //眼睛坐标的属性索引
        Log.e(TAG, "BigEyeFilter mProgram ="+mProgramId );
        left_eye = glGetUniformLocation(mProgramId, "left_eye");
        right_eye = glGetUniformLocation(mProgramId, "right_eye");

        left = ByteBuffer.allocateDirect(2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        right = ByteBuffer.allocateDirect(2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    @Override
    protected void changeTextureData() {
        float[] TEXTURE = {
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
        };
        textureData.clear();
        textureData.put(TEXTURE);
    }

    @Override
    public int onDrawFrame(int textureID) {
        if (null == mFace){
            return textureID;
        }
        //1， 设置视窗
        glViewport(0, 0, mWidth, mHeight);
        //这里是因为要渲染到FBO缓存中，而不是直接显示到屏幕上
        glBindFramebuffer(GL_FRAMEBUFFER, mFrameBuffers[0]);

        //2，使用着色器程序
        glUseProgram(mProgramId);

        //渲染 传值
        //1，顶点数据

        vertexData.position(0);

        glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, vertexData);//传值
        //传值后激活
        glEnableVertexAttribArray(vPosition);

        //2，纹理坐标
        textureData.position(0);
        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, textureData);
        //传值后激活
        glEnableVertexAttribArray(vCoord);

        //传 mFace 眼睛坐标 给着色器
        float[] landmarks =  mFace.landmarks;

        //左眼 换算到纹理坐标 0-1
        float x = landmarks[2] / mFace.imgWidth;
        float y = landmarks[3] / mFace.imgHeight;
        left.clear();
        left.put(x);
        left.put(y);
        left.position(0);
        glUniform2fv(left_eye, 1, left);

        //右眼
        x = landmarks[4] / mFace.imgWidth;
        y = landmarks[5] / mFace.imgHeight;
        right.clear();
        right.put(x);
        right.put(y);
        right.position(0);
        glUniform2fv(right_eye, 1, right);

        //片元， vTexture
        //激活图层
        glActiveTexture(GL_TEXTURE0);
        //绑定
        glBindTexture(GL_TEXTURE_2D, textureID);
        //传递参数
        glUniform1i(vTexture, 0);

        //通知opengl绘制
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        //解绑fbo
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        //        return textureID;//Bug
        return mFrameBufferTextures[0];//返回fbo的纹理id
    }

    public void setFace(Face mFace) {
        this.mFace = mFace;
    }
}
