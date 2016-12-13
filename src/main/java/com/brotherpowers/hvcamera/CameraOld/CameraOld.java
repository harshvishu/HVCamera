package com.brotherpowers.hvcamera.CameraOld;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.brotherpowers.hvcamera.CameraFragmentInteractionInterface;
import com.brotherpowers.hvcamera.HVBaseFragment;
import com.brotherpowers.hvcamera.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CameraOld#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CameraOld extends HVBaseFragment {
    private static final String ARG_FRONT_CAMERA_SUPPORTED = "front_camera_supported";
    private static final String ARG_IS_FRONT_CAMERA = "is_front_camera";
    private static final String ARG_FLASH_MODE = "flash_mode";
    private static final String ARG_CAMERA_ID = "camera_id";


    private static final int REQ_CAM_PERMISSION = 0x233;
    private int cameraId = -1;
    private Camera.Size previewSize;
    private CameraConfig.FlashMode flashMode = CameraConfig.FlashMode.AUTO;


    public CameraOld() {
        // Required empty public constructor
    }

    /**
     * @return A new instance of fragment CameraOld.
     */
    public static CameraOld newInstance(boolean frontCameraSupported, boolean isFrontCamera) {
        CameraOld fragment = new CameraOld();
        Bundle args = new Bundle();
        args.putBoolean(ARG_FRONT_CAMERA_SUPPORTED, frontCameraSupported);
        args.putBoolean(ARG_IS_FRONT_CAMERA, isFrontCamera);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    private Camera camera;

    @Override
    public void onViewCreated(View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                System.out.println(">>>>> TEXTURE VIEW TOUCH");
                if (camera != null) {
                    camera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean b, Camera camera) {
                            System.out.println(" AF CALLBACK");
                        }
                    });
                    return true;
                }
                return false;
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    private void openCamera(int width, int height) {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();

            Toast.makeText(getContext(), "UNABLE TO OPEN CAMERA. PERMISSION DENIED", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            setUpCameraOutputs(width, height);
        } catch (Exception e) {
            Log.e("CAMERA OLD", "ERROR SETTING CAMERA OUTPUT");
            Log.e("CAMERA OLD", e.getMessage());
        }

        try {
            configureTransform(width, height);
        } catch (Exception e) {
            Log.e("CAMERA OLD", "ERROR SETTING CONFIGURE TRANSFORM");
            Log.e("CAMERA OLD", e.getMessage());
        }
        try {
            camera.setPreviewTexture(textureView.getSurfaceTexture());
            camera.startPreview();

        } catch (Exception e) {
            Log.e("CAMERA OLD", "ERROR STARTING PREVIEW");
            Log.e("CAMERA OLD", e.getMessage());
        }

    }

    private void setUpCameraOutputs(int width, int height) throws Exception {
        Activity activity = getActivity();

        // Using back camera
        cameraId = CameraUtil.getBackCameraId();
        camera = Camera.open(cameraId);

        Camera.Parameters parameters = camera.getParameters();

        /*
        * returned parameters
        * Set flash mode
        * */
        setFlash(parameters);

        /*
        * Set auto focus
        * */
        setCameraAF(parameters);


        /*
        * Fix display orientation
        * */
        camera.setDisplayOrientation(CameraUtil.getPortraitCameraDisplayOrientation(activity,
                cameraId, false));


        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);
        int mSensorOrientation = cameraInfo.orientation;

        boolean swappedDimensions = false;
        int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (displayRotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                    swappedDimensions = true;
                }
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                    swappedDimensions = true;
                }
                break;
            default:
                Log.e("CAMERA OLD", "Display rotation is invalid: " + displayRotation);
        }

        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        Camera.Size largest = Collections.max(sizes, new CompareSizesByArea());

        Point displaySize = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
        int rotatedPreviewWidth = width;
        int rotatedPreviewHeight = height;
        int maxPreviewWidth = displaySize.x;
        int maxPreviewHeight = displaySize.y;

        if (swappedDimensions) {
            rotatedPreviewWidth = height;
            rotatedPreviewHeight = width;
            maxPreviewWidth = displaySize.y;
            maxPreviewHeight = displaySize.x;
        }

        if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
            maxPreviewWidth = MAX_PREVIEW_WIDTH;
        }

        if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
            maxPreviewHeight = MAX_PREVIEW_HEIGHT;
        }

        previewSize = chooseOptimalSize(sizes, rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                maxPreviewHeight, largest);
        parameters.setPreviewSize(previewSize.width, previewSize.height);

        // We fit the aspect ratio of TextureView to the previewSize of preview we picked.
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            textureView.setAspectRatio(previewSize.width, previewSize.height);
        } else {
            textureView.setAspectRatio(previewSize.height, previewSize.width);
        }


        // SET THE PARAMETERS
        camera.setParameters(parameters);

    }

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    @Override
    public void onPause() {
        super.onPause();
        stopBackgroundThread();

        try {
            camera.stopPreview();
            camera.release();
        } catch (Exception e) {
            Log.e("CAMERA OLD", "ERROR CLOSING CAMERA");
            Log.e("CAMERA OLD", e.getMessage());
        }
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();

        // Button Flash
        if (id == R.id.flash) {
//            actionFlash();
        }
        // Button Picture
        else if (id == R.id.picture) {
            actionTakePicture();
        }
        // Button Camera Switch
        else if (id == R.id.switch_camera) {
//            actionSwitchCamera();
        }

    }

    private void actionTakePicture() {

        try {
            camera.takePicture(null, null, pictureCallback);
        } catch (Exception e) {
            Log.e("CAMERA OLD", "ERROR TAKING PICTURE");
            Log.e("CAMERA OLD", e.getMessage());
        }
    }

    /**
     * @param flashMode {@link CameraConfig.FlashMode }
     *                  <p>
     *                  set the icon for flash mode
     */
    private void setFlashButtonResource(CameraConfig.FlashMode flashMode) {
        switch (flashMode) {
            case AUTO:
                buttonFlash.setImageResource(R.drawable.ic_flash_auto);
                break;
            case ON:
                buttonFlash.setImageResource(R.drawable.ic_flash_on);
                break;
            case OFF:
                buttonFlash.setImageResource(R.drawable.ic_flash_off);
                break;
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            interactionInterface = (CameraFragmentInteractionInterface) getActivity();

        } catch (ClassCastException e) {
            throw new RuntimeException("Implement CameraFragmentInteractionInterface in activity");
        }


    }


    /**
     * Picture Callback
     * <p>
     * Called after {@link Camera#takePicture(Camera.ShutterCallback, Camera.PictureCallback, Camera.PictureCallback, Camera.PictureCallback)}
     */
    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] bytes, Camera camera) {

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    buttonSwitchCamera.setEnabled(true);
                }
            });

            buttonSwitchCamera.setEnabled(false);

            try {
                mBackgroundHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        Matrix matrix = new Matrix();

                        int angle = CameraUtil.getPortraitCameraDisplayOrientation(getContext(), cameraId, false);
                        matrix.postRotate(angle);

                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                        interactionInterface.onImageCaptured(bitmap);

                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                // Catch any exception
            }

            camera.startPreview();

        }
    };


    /**
     * Request camera permission
     */
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_CAM_PERMISSION);
    }

    /**
     * @param parameters set camera flash
     */
    private Camera.Parameters setFlash(Camera.Parameters parameters) {
        if (parameters.getSupportedFlashModes() != null) {
            switch (flashMode) {
                case AUTO:
                    if (parameters.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    }
                    break;
                case ON:
                    if (parameters.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_ON)) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                    }
                    break;
                case OFF:
                    if (parameters.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_OFF)) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    }
                    break;
            }
        }
        return parameters;
    }

    /**
     * Set focus ode auto focus for camera preview
     *
     * @param parameters
     */
    private Camera.Parameters setCameraAF(Camera.Parameters parameters) {
        if (parameters.getSupportedFocusModes() != null) {
            if (parameters.getSupportedFlashModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
        }
        return parameters;
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview previewSize is determined in
     * setUpCameraOutputs and also the previewSize of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
//        Activity activity = getActivity();
//        if (null == textureView || null == previewSize || null == activity) {
//            return;
//        }
//        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
//        Matrix matrix = new Matrix();
//        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
//        RectF bufferRect = new RectF(0, 0, previewSize.height, previewSize.width);
//        float centerX = viewRect.centerX();
//        float centerY = viewRect.centerY();
//        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
//            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
//            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
//            float scale = Math.max(
//                    (float) viewHeight / previewSize.height,
//                    (float) viewWidth / previewSize.width);
//            matrix.postScale(scale, scale, centerX, centerY);
//            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
//        } else if (Surface.ROTATION_180 == rotation) {
//            matrix.postRotate(180, centerX, centerY);
//        }
//
//        textureView.setTransform(matrix);
    }


    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Camera.Size chooseOptimalSize(List<Camera.Size> choices, int textureViewWidth,
                                                 int textureViewHeight, int maxWidth, int maxHeight, Camera.Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Camera.Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Camera.Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.width;
        int h = aspectRatio.height;
        for (Camera.Size option : choices) {
            if (option.width <= maxWidth && option.height <= maxHeight &&
                    option.height == option.width * h / w) {
                if (option.width >= textureViewWidth &&
                        option.height >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e("CAMERA OLD", "Couldn't find any suitable preview size");
            return choices.get(0);
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Camera.Size> {

        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.width * lhs.height -
                    (long) rhs.width * rhs.height);
        }

    }
}
