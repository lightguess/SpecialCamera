package com.lg.specialcamera.record;


import android.content.Context;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.view.Surface;

import com.lg.specialcamera.filter.ScreenFilter;

import static android.opengl.EGL14.EGL_ALPHA_SIZE;
import static android.opengl.EGL14.EGL_BLUE_SIZE;
import static android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION;
import static android.opengl.EGL14.EGL_DEFAULT_DISPLAY;
import static android.opengl.EGL14.EGL_GREEN_SIZE;
import static android.opengl.EGL14.EGL_NONE;
import static android.opengl.EGL14.EGL_NO_CONTEXT;
import static android.opengl.EGL14.EGL_NO_SURFACE;
import static android.opengl.EGL14.EGL_OPENGL_ES2_BIT;
import static android.opengl.EGL14.EGL_RED_SIZE;
import static android.opengl.EGL14.EGL_RENDERABLE_TYPE;
import static android.opengl.EGL14.eglChooseConfig;
import static android.opengl.EGL14.eglCreateContext;
import static android.opengl.EGL14.eglCreateWindowSurface;
import static android.opengl.EGL14.eglDestroyContext;
import static android.opengl.EGL14.eglDestroySurface;
import static android.opengl.EGL14.eglGetDisplay;
import static android.opengl.EGL14.eglGetError;
import static android.opengl.EGL14.eglInitialize;
import static android.opengl.EGL14.eglMakeCurrent;
import static android.opengl.EGL14.eglReleaseThread;
import static android.opengl.EGL14.eglSwapBuffers;
import static android.opengl.EGL14.eglTerminate;

/**
 *
 * EGL Tools
 * */
public class MyEGL {
        private EGLDisplay mEglDisplay;
        private EGLConfig mEGLConfig;
        private EGLContext mEGLContext;

        private final EGLSurface mEGLSurface;
        private final ScreenFilter mScreenFilter;



    public MyEGL(EGLContext eglContext, Surface surface, Context context, int width, int height){
        //1.create init EGL
            createEGL(eglContext);
        int[] attrib_list = {
                EGL_NONE
        };
        //2.create window
         mEGLSurface = eglCreateWindowSurface(mEglDisplay, mEGLConfig, surface,
                        attrib_list, 0);

        if(!eglMakeCurrent(mEglDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
                   throw new RuntimeException("eglMakeCurrent fail: " + eglGetError());
               }

       //3.draw
        mScreenFilter = new ScreenFilter(context);
        mScreenFilter.onReady(width,height);

    }

    private void createEGL(EGLContext eglContext) {
        //1.getDisplay device
        mEglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);

        //2.init device
        int[] version = new int[2];
        if(!eglInitialize(mEglDisplay,version,0,version,1)){
            throw  new RuntimeException("eglInitialize fail");
        }

        //3.set attribute
        int [] attrib_list= {
                // rgba
                EGL_RED_SIZE,8,
                EGL_GREEN_SIZE,8,
                EGL_BLUE_SIZE, 8,
                EGL_ALPHA_SIZE, 8,
                EGL_RENDERABLE_TYPE,EGL_OPENGL_ES2_BIT,
                EGLExt.EGL_RECORDABLE_ANDROID,1,
                EGL_NONE
        };

        EGLConfig[] configs = new EGLConfig[1];
        int[] num_config = new int [1];
        if(!eglChooseConfig(
            mEglDisplay,
            attrib_list, 0,
            configs,0,
            configs.length,num_config,
            0)){
            throw new RuntimeException("eglChooseConfig fail");

        }

        //4.create context
        mEGLConfig= configs[0];
        int[] ctx_attrib_list = {
                EGL_CONTEXT_CLIENT_VERSION, 2,//context 版本
                EGL_NONE
        };

        mEGLContext = eglCreateContext(
                mEglDisplay,
                mEGLConfig,
                eglContext,
                ctx_attrib_list,
                0);

        if(mEGLContext ==null || mEGLContext == EGL_NO_CONTEXT) {
            mEGLContext = null;
            throw new RuntimeException("eglCreateContext fail");
        }
    }


    public void release(){
        eglMakeCurrent(mEglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        eglDestroySurface(mEglDisplay, mEGLSurface);
        eglDestroyContext(mEglDisplay, mEGLContext);
        eglReleaseThread();
        eglTerminate(mEglDisplay);
    }

    public void draw(int textureID, long timestamp){
        //渲染到虚拟屏幕
        mScreenFilter.onDrawFrame(textureID);
        //刷新mEGLSurface的时间戳
        //如果设置不合理，编码的时候会采取丢帧或以低质量的编码方式进行编码
        EGLExt.eglPresentationTimeANDROID(mEglDisplay, mEGLSurface, timestamp);

        //交换缓冲数据（看资料《EGL接口解析与理解 》eglSwapBuffers接口实现说明）
        eglSwapBuffers(mEglDisplay, mEGLSurface);
    }

}
