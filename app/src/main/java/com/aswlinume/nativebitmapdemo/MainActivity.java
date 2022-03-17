package com.aswlinume.nativebitmapdemo;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.aswlinume.nativebitmapdemo.databinding.ActivityMainBinding;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    Bitmap mBitmap;
    private ActivityMainBinding mBinding;
    private Handler mHandler;
    private ExecutorService mExecutorService;

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

        mExecutorService = Executors.newSingleThreadExecutor();

        mBitmap = ((BitmapDrawable) mBinding.ivDemo.getDrawable()).getBitmap();
        mExecutorService.submit(() -> {
            BitmapProcessingUtils.init(mBitmap);
        });
    }

    public void resetImage(View view) throws ExecutionException, InterruptedException {
        Toast.makeText(MainActivity.this, "开始恢复原图", Toast.LENGTH_SHORT).show();
        Future<Integer> future = mExecutorService.submit(() -> {
            BitmapProcessingUtils
                    .transImage(mBitmap, mHandler, BitmapProcessingUtils.RESET_PROCESSING);
        }, 1);
        Log.i(TAG, "resetImage: " + future.get());

    }

    public void doImageGrayProcessing(View view) {
        Toast.makeText(MainActivity.this, "开始图像处理", Toast.LENGTH_SHORT).show();
        mExecutorService.execute(() -> {
            BitmapProcessingUtils
                    .transImage(mBitmap, mHandler, BitmapProcessingUtils.GRAY_PROCESSING);
        });
    }

    public void doImageInverseProcessing(View view) {
        Toast.makeText(MainActivity.this, "开始图像处理", Toast.LENGTH_SHORT).show();
        mExecutorService.execute(() -> {
            BitmapProcessingUtils
                    .transImage(mBitmap, mHandler, BitmapProcessingUtils.INVERSE_PROCESSING);
        });
    }


}