package com.example.litocamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Array;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static android.hardware.camera2.CameraCaptureSession.*;

public class MainActivity extends AppCompatActivity {

    private TextureView textureView;
    private CameraManager camera_manager;
    private CameraDevice camera_device;
    private Surface texture_view_surface;
    private ImageReader imageReader;
    private Surface image_reader_surface;
    private CameraCaptureSession capture_session;
    private CaptureRequest.Builder request_builder;
    private CaptureRequest request;
    private CaptureRequest take_request;
    private ImageView imageView;

//    private static final SparseIntArray ORIENTATION = new SparseIntArray();
//    static {
//        ORIENTATION.append(Surface.ROTATION_0, 270);
//        ORIENTATION.append(Surface.ROTATION_90, 0);
//        ORIENTATION.append(Surface.ROTATION_180, 90);
//        ORIENTATION.append(Surface.ROTATION_270, 180);
//    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageview);
        textureView = findViewById(R.id.textureview);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                open_camera();
                texture_view_surface = new Surface(textureView.getSurfaceTexture());
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            }
        });
    }

    private void open_camera() {
        imageReader = ImageReader.newInstance(200,200, ImageFormat.JPEG,2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener(){

            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.i("cccc", "onImageAvailable: get a img");
                Image image = reader.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                int bytes_n = buffer.remaining();
                byte[] bytes = new byte[bytes_n];
                buffer.get(bytes);

                image.close();

                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes_n);
                imageView.setImageBitmap(bitmap);
            }
        },null);
        image_reader_surface = imageReader.getSurface();

        camera_manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        requestPermissions();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            camera_manager.openCamera(String.valueOf(0), new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    camera_device = camera;
                    try {
                        camera_device.createCaptureSession(Arrays.asList(texture_view_surface, image_reader_surface), new StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                capture_session = session;
                                try {
                                    request_builder = camera_device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                                request_builder.addTarget(texture_view_surface);
                                request = request_builder.build();
                                try {
                                    capture_session.setRepeatingRequest(request,null,null);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void requestPermissions() {
        // checkSelfPermission 判断是否已经申请了此权限
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            //如果应用之前请求过此权限但用户拒绝了请求，shouldShowRequestPermissionRationale将返回 true。
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {

            } else {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA,}, 1);
            }
        }
    }

    public void tackPhoto(View view) {
        try {
            CaptureRequest.Builder take_request_builder = camera_device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            take_request_builder.addTarget(image_reader_surface);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            //take_request_builder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
            take_request_builder.set(CaptureRequest.JPEG_ORIENTATION, 90);

            take_request = take_request_builder.build();
            capture_session.capture(take_request,null,null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}