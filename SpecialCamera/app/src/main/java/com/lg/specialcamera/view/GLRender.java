package com.lg.specialcamera.view;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.Log;
import android.view.View;


import androidx.annotation.RequiresApi;

import com.lg.specialcamera.face.FaceTrack;
import com.lg.specialcamera.filter.BeautyFilter;
import com.lg.specialcamera.filter.BigEyeFilter;
import com.lg.specialcamera.filter.CameraFilter;
import com.lg.specialcamera.filter.ScreenFilter;
import com.lg.specialcamera.filter.StickFilter;
import com.lg.specialcamera.record.MyMediaRecorder;
import com.lg.specialcamera.utils.CameraHelper;
import com.lg.specialcamera.utils.FileUtil;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_STENCIL_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;

public class GLRender implements GLSurfaceView.Renderer , SurfaceTexture.OnFrameAvailableListener , Camera.PreviewCallback {

    String TAG = GLRender.class.getSimpleName();
    private MyMediaRecorder mMediaRecorder;

      private CameraHelper mCameraHelper;
    private final int mCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;

        private SurfaceTexture mSurfaceTexture;
        private int[] mTextures;

    private ScreenFilter mScreenFilter;

    protected GLView mView;
    private FaceTrack mFaceTrack;
    private int mWidth;
    private int mHeight;
    private BigEyeFilter mBigEyeFilter;
    private CameraFilter mCameraFilter;
    private StickFilter mStickFilter;
    private BeautyFilter mBeautyFilter;
   public GLRender(GLView view){
        this.mView=view;
        FileUtil.copyAssets2SDCard(mView.getContext(), "lbpcascade_frontalface.xml",
                "/sdcard/lbpcascade_frontalface.xml");
        FileUtil.copyAssets2SDCard(mView.getContext(), "seeta_fa_v1.1.bin",
                "/sdcard/seeta_fa_v1.1.bin");
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        mCameraHelper = new CameraHelper((Activity) mView.getContext(), mCameraID, 800,
                480);
        mCameraHelper.setPreviewCallback(this);
        mTextures = new int[1];
        GLES20.glGenTextures(mTextures.length,mTextures, 0);
//     BindTexture(GLES20.GL_TEXTURE_2D,textureId)--》 mTextures==mSurfaceTexture-->摄像头采集数据
        mSurfaceTexture = new SurfaceTexture(mTextures[0]);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        mScreenFilter = new ScreenFilter(mView.getContext());//滤镜
        mCameraFilter = new CameraFilter(mView.getContext());

        //create
//        mBigEyeFilter = new BigEyeFilter(mView.getContext());


        EGLContext eglContext = EGL14.eglGetCurrentContext();   //渲染线程的 EGLContext

        mMediaRecorder = new MyMediaRecorder(800, 480, "/sdcard/test.mp4", eglContext,
                mView.getContext());
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        mWidth = width;
        mHeight = height;
        mFaceTrack = new FaceTrack("/sdcard/lbpcascade_frontalface.xml",
                "/sdcard/seeta_fa_v1.1.bin", mCameraHelper);

        //启动跟踪器
        Log.e(TAG, "mFaceTrack.startTrack: " );
        mFaceTrack.startTrack();
        mCameraHelper.startPreview(mSurfaceTexture);
        mCameraFilter.onReady(width, height);
        //mBigEyeFilter.onReady(width, height);
        mScreenFilter.onReady(width, height);
        Log.e(TAG, "onSurfaceChanged finished: " );


    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onDrawFrame(GL10 gl10) {
        //设置清理屏幕的颜色
        glClearColor(255,0,0,0);
        //GL_COLOR_BUFFER_BIT 颜色缓冲区
        //GL_DEPTH_WRITEMASK    深度缓冲区
        //GL_STENCIL_BUFFER_BIT 模型缓冲区
        glClear(GL_STENCIL_BUFFER_BIT);

        //输出摄像头的数据
        //更新纹理
        mSurfaceTexture.updateTexImage();
        float[] mtx = new float[16];
        mSurfaceTexture.getTransformMatrix(mtx);
        mCameraFilter.setMatrix(mtx);

        //mTextureID[0]: 摄像头的, 先渲染到FBO
        int textureId = mCameraFilter.onDrawFrame(mTextures[0]);
        //滤镜特效
        //textureId = xxxFilter.onDrawFrame(textureId);
        //textureId = xxxFilter.onDrawFrame(textureId);
        //......
        if (null != mBigEyeFilter){
            mBigEyeFilter.setFace(mFaceTrack.getFace());
            textureId = mBigEyeFilter.onDrawFrame(textureId);
        }
        if (null != mStickFilter){
            mStickFilter.setFace(mFaceTrack.getFace());
            textureId = mStickFilter.onDrawFrame(textureId);
        }
        if (null != mBeautyFilter){
            textureId = mBeautyFilter.onDrawFrame(textureId);
        }

        mScreenFilter.onDrawFrame(textureId);

        //录制视频（将图像进行编码）
        mMediaRecorder.encodeFrame(textureId, mSurfaceTexture.getTimestamp());
    }

    public void onSurfaceDestroyed() {
        mCameraHelper.stopPreview();
        mFaceTrack.stopTrack();
    }

    /**
     * 开始录制
     *
     * @param speed
     */
    public void startRecording(float speed) {
        Log.e("MyGLRender", "startRecording");
        try {
            mMediaRecorder.start(speed);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止录制
     */
    public void stopRecording() {
        Log.e("MyGLRender", "stopRecording");
        mMediaRecorder.stop();
    }


    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                mView.requestRender();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        mFaceTrack.detector(data);
    }
    public void enableBigEye(final boolean isChecked) {
//        mBigEyeFilter = new BigEyeFilter(mGLSurfaceView.getContext());
//        mBigEyeFilter.onReady(mWidth, mHeight);

        mView.queueEvent(new Runnable() {
            @Override
            public void run() {
                //Opengl线程
                if (isChecked) {
                    mBigEyeFilter = new BigEyeFilter(mView.getContext());
                    mBigEyeFilter.onReady(mWidth, mHeight);
                } else {
                    mBigEyeFilter.release();
                    mBigEyeFilter = null;
                }
            }
        });
    }

    public void enableStick(final boolean isChecked) {
        mView.queueEvent(new Runnable() {
            @Override
            public void run() {
                //Opengl线程
                if (isChecked) {
                    mStickFilter = new StickFilter(mView.getContext());
                    mStickFilter.onReady(mWidth, mHeight);
                } else {
                    mStickFilter.release();
                    mStickFilter = null;
                }
            }
        });
    }

    public void enableBeauty(final boolean isChecked) {
        mView.queueEvent(new Runnable() {
            @Override
            public void run() {
                //Opengl线程
                if (isChecked) {
                    mBeautyFilter = new BeautyFilter(mView.getContext());
                    mBeautyFilter.onReady(mWidth, mHeight);
                } else {
                    mBeautyFilter.release();
                    mBeautyFilter = null;
                }
            }
        });
    }
}
