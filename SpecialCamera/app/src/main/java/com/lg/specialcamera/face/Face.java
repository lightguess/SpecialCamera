package com.lg.specialcamera.face;

import java.util.Arrays;

public class Face {

    //5 lefteyes、 righteyes 、nose、left mouth、right mouth
    public float[] landmarks;

    //persion face width
    public int width;

    //persion face height
    public int height;

    // face image width
    public int imgWidth;

    //face image height
    public int imgHeight;


    Face(int width, int height, int imgWidth,int imgHeight, float[] landmarks) {
        this.width = width;
        this.height = height;
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
        this.landmarks = landmarks;
    }

    @Override
    public String toString() {
        return "Face{" +
                "landmarks=" + Arrays.toString(landmarks) +
                ", width=" + width +
                ", height=" + height +
                ", imgWidth=" + imgWidth +
                ", imgHeight=" + imgHeight +
                '}';
    }
}
