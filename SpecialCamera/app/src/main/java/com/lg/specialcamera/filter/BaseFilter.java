package com.lg.specialcamera.filter;

import android.content.Context;


import com.lg.specialcamera.utils.BufferHelper;
import com.lg.specialcamera.utils.ShaderHelper;
import com.lg.specialcamera.utils.TextResourceReader;

import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

public abstract class BaseFilter {

    private final int mVertexSouceId;
    private final int mFragmentSouceId;
    protected final FloatBuffer vertexData;
    protected final FloatBuffer textureData;
    protected int mProgramId;
    protected int vPosition;
    protected int vCoord;
    protected int vMatrix;
    protected int vTexture;
    protected int mWidth;
    protected int mHeight;

    public BaseFilter(Context context, int vertexSouceId, int fragmentSouceId) {
        mVertexSouceId = vertexSouceId;
        mFragmentSouceId = fragmentSouceId;



        float[] VERTEX = {
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, 1.0f,
        };
        vertexData = BufferHelper.getFloatBuffer(VERTEX);

        float[] TEXTURE = {
                0.0f, 1.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f,
        };
        textureData = BufferHelper.getFloatBuffer(TEXTURE);

        init(context);
        changeTextureData();
    }

    /**
     * ?????????????????? textureData????????????????????????????????????
     */
    protected void changeTextureData(){

    }

    protected void init(Context context) {
        //?????????????????????
        String vertexSource = TextResourceReader.readTextFileFromResource(context,
                mVertexSouceId);
        //?????????????????????
        String fragmentSource = TextResourceReader.readTextFileFromResource(context,
                mFragmentSouceId);
        //?????????????????????id
        int vertexShaderId = ShaderHelper.compileVertexShader(vertexSource);
//        int fragmentShaderId = ShaderHelper.compileVertexShader(fragmentSource);//Bug
        int fragmentShaderId = ShaderHelper.compileFragmentShader(fragmentSource);
        //????????????id
        mProgramId = ShaderHelper.linkProgram(vertexShaderId, fragmentShaderId);
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
        //????????????id??????????????????
        //??????
        vPosition = glGetAttribLocation(mProgramId, "vPosition");
        vCoord = glGetAttribLocation(mProgramId, "vCoord");
        vMatrix = glGetUniformLocation(mProgramId, "vMatrix");
        //??????
        vTexture = glGetUniformLocation(mProgramId, "vTexture");
    }

    public void release(){
        glDeleteProgram(mProgramId);
    }

    public void onReady(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public int onDrawFrame(int textureID) {
        //1??? ????????????
        glViewport(0, 0, mWidth, mHeight);
        //2????????????????????????
        glUseProgram(mProgramId);

        //????????? ??????
        //1???????????????
        vertexData.position(0);

        //int indx,
        //        int size,
        //        int type,
        //        boolean normalized,
        //        int stride,
        //        java.nio.Buffer ptr
        glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, vertexData);//??????
        //???????????????
        glEnableVertexAttribArray(vPosition);

        //2???????????????
        textureData.position(0);
        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, textureData);
        //???????????????
        glEnableVertexAttribArray(vCoord);



        //????????? vTexture
        //????????????
        glActiveTexture(GL_TEXTURE0);
        //??????
        glBindTexture(GL_TEXTURE_2D, textureID);
        //????????????
        glUniform1i(vTexture, 0);


        //??????opengl??????
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        return textureID;
    }
}
