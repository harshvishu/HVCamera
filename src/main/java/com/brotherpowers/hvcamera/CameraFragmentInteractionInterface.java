package com.brotherpowers.hvcamera;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Build;

/**
 * Created by harsh_v on 12/8/16.
 */

public interface CameraFragmentInteractionInterface {

    /**
     * Runs on Background Thread
     *
     * @param data  Image Data
     * @param angle Angle at which image is to be rotated
     */
    void onImageCaptured(byte[] data, int angle);

    /**
     * Runs on Background Thread
     *
     * @param bitmap @{@link Bitmap} Image Data
     */
    void onImageCaptured(Bitmap bitmap);

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    void onImageCaptured(Image image);
}
