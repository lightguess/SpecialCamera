package com.lg.specialcamera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class WelcomeActivity extends AppCompatActivity {

    String TAG= WelcomeActivity.class.getSimpleName();

    private static final int MY_PERMISSIONS_REQUEST_CALL_CAMERA = 2;

    String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    List<String> mPermissionList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        mPermissionList.clear();
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(WelcomeActivity.this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);
            }
        }
        if (mPermissionList.isEmpty()) {//未授予的权限为空，表示都授予了
            Log.e(TAG, "mPermissionList.isEmpty()" );
            startActivity(new Intent(this, MainActivity.class));

        } else {//请求权限方法
            Log.e(TAG, "requestPermissions" );

            String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);//将List转为数组
            ActivityCompat.requestPermissions(WelcomeActivity.this, permissions, MY_PERMISSIONS_REQUEST_CALL_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.e(TAG, "onRequestPermissionsResult" );
        boolean showRequestPermission = false;
        if (requestCode == MY_PERMISSIONS_REQUEST_CALL_CAMERA) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    //判断是否勾选禁止后不再询问
                    if (Manifest.permission.CAMERA.equals(permissions[i])) {
                        showRequestPermission = ActivityCompat.shouldShowRequestPermissionRationale(WelcomeActivity.this, permissions[i]);
                        if (showRequestPermission) {
                        }
                    }
                }
            }
        }

        startActivity(new Intent(this, MainActivity.class));

    }
}
