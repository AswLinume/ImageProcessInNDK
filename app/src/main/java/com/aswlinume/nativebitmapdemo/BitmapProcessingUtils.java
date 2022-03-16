package com.aswlinume.nativebitmapdemo;

import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;

public class BitmapProcessingUtils {
    
    private static final String TAG = BitmapProcessingUtils.class.getSimpleName();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public static final int RESET_PROCESSING = 1;
    public static final int GRAY_PROCESSING = 2;
    public static final int INVERSE_PROCESSING = 3;

    private static long mPtr;

    public static void init(Bitmap bitmap) {
        Log.i(TAG, "init: ");
        mPtr = nativeInit(bitmap);
    }


    public static void transImage(Bitmap bitmap, Handler handler, int processingType) {
        nativeTransImage(mPtr, bitmap, processingType, handler);
    }


    private native static long nativeInit(Bitmap bitmap);

    public native static void nativeTransImage(long ptr, Bitmap bitmap, int processingType, Handler handler);


}
