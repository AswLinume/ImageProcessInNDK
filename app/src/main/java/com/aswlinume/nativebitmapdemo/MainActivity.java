package com.aswlinume.nativebitmapdemo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.aswlinume.nativebitmapdemo.databinding.ActivityMainBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_FOR_SELECT_IMAGE = 100;
    private static final int REQUEST_CODE_FOR_PERMISSION_READ_STORAGE = 101;

    private static final String[] PERMISSIONS_READ_STORAGE = {"android.permission.READ_EXTERNAL_STORAGE"};

    private Bitmap mBitmap;
    private ActivityMainBinding mBinding;
    private Handler mHandler;
    private ExecutorService mExecutorService;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mHandler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                mBinding.ivDemo.invalidate();
                dismissProgressDialog();
                Toast.makeText(MainActivity.this, "图像处理完成", Toast.LENGTH_SHORT).show();
            }
        };

        mExecutorService = Executors.newSingleThreadExecutor();

        mBitmap = ((BitmapDrawable) mBinding.ivDemo.getDrawable()).getBitmap();
        mExecutorService.submit(() -> {
            BitmapProcessingUtils.init(mBitmap);
        });
    }

    public void loadImageFromLocal(View view) {
        if (!checkReadStoragePermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_READ_STORAGE,
                    REQUEST_CODE_FOR_PERMISSION_READ_STORAGE);
            return;
        }
        startSelectImage();
    }

    public void startSelectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "选择待处理的图片"), REQUEST_CODE_FOR_SELECT_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FOR_SELECT_IMAGE && null != data) {
            loadImageBySelect(data.getData());
            Log.i(TAG, "onActivityResult: ");
            mExecutorService.execute(() -> {
                BitmapProcessingUtils.init(mBitmap);
            });
        }
    }

    private void loadImageBySelect(Uri uri) {
        String imagePath = null;
        if (uri != null) {
            if ("file".equals(uri.getScheme())) {
                imagePath = uri.getPath();
            } else if ("content".equals(uri.getScheme())) {
                String[] filePaths = {MediaStore.Images.Media.DATA};
                Cursor c = getContentResolver().query(uri, filePaths, null, null, null);
                if (c != null) {
                    if (c.moveToFirst()) {
                        int index = c.getColumnIndex(filePaths[0]);
                        imagePath = c.getString(index);
                    }
                    c.close();
                }
            }
        }
        if (!TextUtils.isEmpty(imagePath)) {
            if (mBitmap != null) {
                mBitmap.recycle();
            }
            mBitmap = BitmapFactory.decodeFile(imagePath);
            mBinding.ivDemo.setImageBitmap(mBitmap);
        }
    }

    //DownScale image
    private Bitmap getBitmapFromPath(String imagePath) {
        Log.i(TAG, "getBitmapFromPath: " + imagePath);
        BitmapFactory.Options bfo = new BitmapFactory.Options();
        bfo.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath);
        int width = bfo.outWidth, height = bfo.outHeight;
        int scale = 1;
        while (true) {
            if (width <= 640 && height <= 480) {
                break;
            }
            width /= 2;
            height /= 2;
            scale *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = scale;
        options.outWidth = width;
        options.outHeight = height;
        return BitmapFactory.decodeFile(imagePath, options);
    }

    public void loadImageFromNet(View view) {
        final EditText editText = new EditText(this);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("请输入网络图片URL")
                .setView(editText)
                .setPositiveButton("加载图片", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        getImageFromNetByUrl(editText.getText().toString());
                    }
                }).create();
        dialog.show();
    }

    private void getImageFromNetByUrl(String url) {
        Log.i(TAG, "getImageFromNetByUrl: " + url);
        Glide.with(this)
                .load(url)
                .placeholder(R.mipmap.demo)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Toast.makeText(MainActivity.this, "图片加载失败，请检查图片地址是否正确", Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        if (mBitmap != null) {
                            mBitmap.recycle();
                        }
                        mBitmap = ((BitmapDrawable) (resource)).getBitmap();
                        mExecutorService.execute(() -> {
                            BitmapProcessingUtils.init(mBitmap);
                        });
                        mBinding.ivDemo.invalidate();
                        return false;
                    }
                })
                .into(mBinding.ivDemo);

    }

    public void resetImage(View view) throws ExecutionException, InterruptedException {
        //Toast.makeText(MainActivity.this, "开始恢复原图", Toast.LENGTH_SHORT).show();
        mExecutorService.execute(() -> {
            BitmapProcessingUtils
                    .transImage(mBitmap, mHandler, BitmapProcessingUtils.RESET_PROCESSING);
        });
        showProgressDialog();
    }

    public void doImageGrayProcessing(View view) {
        //Toast.makeText(MainActivity.this, "开始图像处理", Toast.LENGTH_SHORT).show();
        mExecutorService.execute(() -> {
            BitmapProcessingUtils
                    .transImage(mBitmap, mHandler, BitmapProcessingUtils.GRAY_PROCESSING);
        });
        showProgressDialog();
    }

    public void doImageInverseProcessing(View view) {
        //Toast.makeText(MainActivity.this, "开始图像处理", Toast.LENGTH_SHORT).show();
        mExecutorService.execute(() -> {
            BitmapProcessingUtils
                    .transImage(mBitmap, mHandler, BitmapProcessingUtils.INVERSE_PROCESSING);
        });
        showProgressDialog();
    }

    private boolean checkReadStoragePermissions() {
        return ActivityCompat.checkSelfPermission(this, PERMISSIONS_READ_STORAGE[0])
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_FOR_PERMISSION_READ_STORAGE) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSelectImage();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("正在处理中");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(true);
        }
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}