package com.aswlinume.nativebitmapdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Toast;

import com.aswlinume.nativebitmapdemo.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mBinding;

    private Handler mHandler;

    Bitmap mBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mHandler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                Toast.makeText(MainActivity.this, "图像处理完成", Toast.LENGTH_SHORT).show();
                mBinding.ivDemo.setImageBitmap((Bitmap) msg.obj);
            }
        };

        mBitmap = ((BitmapDrawable) mBinding.ivDemo.getDrawable()).getBitmap();
        BitmapProcessingUtils.init(mBitmap);
    }

    public void resetImage(View view) {
        Toast.makeText(MainActivity.this, "开始恢复原图", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                BitmapProcessingUtils.transImage(mBitmap, mHandler, BitmapProcessingUtils.RESET_PROCESSING);
            }
        }).start();
    }

    public void doImageGrayProcessing(View view) {
        Toast.makeText(MainActivity.this, "开始图像处理", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                BitmapProcessingUtils.transImage(mBitmap, mHandler, BitmapProcessingUtils.GRAY_PROCESSING);
            }
        }).start();
    }

    public void doImageInverseProcessing(View view) {
        Toast.makeText(MainActivity.this, "开始图像处理", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                BitmapProcessingUtils.transImage(mBitmap, mHandler, BitmapProcessingUtils.INVERSE_PROCESSING);
            }
        }).start();
    }


}