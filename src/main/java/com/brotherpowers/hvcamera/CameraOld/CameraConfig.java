package com.brotherpowers.hvcamera.CameraOld;

import android.hardware.Camera;

/**
 * Created by harsh_v on 12/9/16.
 */

class CameraConfig {

    private static final int CAM_DEF_ID = -1;

    private int currentCameraId = CAM_DEF_ID;
    private int frontCameraId = CAM_DEF_ID;
    private int backCameraId = CAM_DEF_ID;

    private FlashMode flashMode = FlashMode.AUTO;
    private boolean frontCameraSupported;
    private boolean autoFocus = true;

    /**
     * @return Current camera ID
     */
    public int getCurrentCameraId() {
        return currentCameraId;
    }

    /**
     * @return true is using front camera false otherwise
     */
    boolean isUsingFrontCamera() {
        return currentCameraId == frontCameraId &&
                frontCameraId != CAM_DEF_ID;
    }

    /**
     * @return true is using back camera false otherwise
     */
    boolean isUsingBackCamera() {
        return currentCameraId == backCameraId &&
                backCameraId != CAM_DEF_ID;
    }


    enum FlashMode {
        AUTO(0, Camera.Parameters.FLASH_MODE_AUTO), ON(1, Camera.Parameters.FLASH_MODE_ON), OFF(2, Camera.Parameters.FLASH_MODE_OFF);

        final int value;
        final String param;

        FlashMode(int value, String param) {
            this.value = value;
            this.param = param;
        }

        // get Enum for integer value
        static FlashMode self(int value) {
            switch (value) {
                case 0:
                    return AUTO;
                case 1:
                    return ON;
                case 2:
                    return OFF;
                default:
                    return OFF;
            }
        }
    }

}
