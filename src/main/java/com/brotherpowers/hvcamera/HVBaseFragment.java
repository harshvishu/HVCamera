package com.brotherpowers.hvcamera;


import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatImageButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 */
public abstract class HVBaseFragment extends Fragment implements View.OnClickListener {

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    protected static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    protected static final int MAX_PREVIEW_HEIGHT = 1080;

    protected CameraFragmentInteractionInterface interactionInterface;
    protected AutoFitTextureView textureView;
    protected AppCompatImageButton buttonFlash;
    protected AppCompatImageButton buttonSwitchCamera;
    protected AppCompatImageButton buttonPicture;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            interactionInterface = (CameraFragmentInteractionInterface) getActivity();

        } catch (ClassCastException e) {
            throw new RuntimeException("Implement CameraFragmentInteractionInterface in activity");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_hvcamera, container, false);

        // Init Views
        textureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        buttonFlash = (AppCompatImageButton) view.findViewById(R.id.flash);
        buttonSwitchCamera = (AppCompatImageButton) view.findViewById(R.id.switch_camera);
        buttonPicture = (AppCompatImageButton) view.findViewById(R.id.picture);

        // OnClick
        buttonFlash.setOnClickListener(this);
        buttonPicture.setOnClickListener(this);
        buttonSwitchCamera.setOnClickListener(this);

        return view;
    }

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    protected HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    protected Handler mBackgroundHandler;

    /**
     * Starts a background thread and its {@link Handler}.
     */
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    protected void stopBackgroundThread() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBackgroundThread.quitSafely();
        }
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
