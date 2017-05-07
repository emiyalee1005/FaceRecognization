package com.example.lee.face.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import com.google.android.cameraview.CameraView;

import java.util.ArrayList;

import app.emiyalee.Util.Util;

/**
 * Created by Lee on 2017/4/10.
 */

public class EmiyaCameraView extends CameraView {
    private Boolean useViewCapture = false;
    private ArrayList<Callback> callbacks = new ArrayList<>();
    //public ImageView imageView;

    public void enableViewCapture(Boolean useViewCapture) {
        this.useViewCapture = useViewCapture;
    }

    public Boolean usingViewCapture() {
        return useViewCapture;
    }

    public EmiyaCameraView(Context context) {
        super(context);
    }

    public EmiyaCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EmiyaCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void addCallback(@NonNull Callback callback) {
        super.addCallback(callback);
        if (!callbacks.contains(callback))
            callbacks.add(callback);
    }

    @Override
    public void removeCallback(@NonNull Callback callback) {
        super.removeCallback(callback);
        if (callbacks.contains(callback))
            callbacks.remove(callback);
    }

    @Override
    public void takePicture() {
        View view = this.getView();
        if (!useViewCapture || view instanceof SurfaceView) {
            super.takePicture();
        } else {
            Bitmap bitmap;

            TextureView textureView = (TextureView) view;
            bitmap = textureView.getBitmap();
            for (Callback callback : callbacks) {
                try {
                    callback.onPictureTaken(this, Util.getBytesFromBitmap(bitmap));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
