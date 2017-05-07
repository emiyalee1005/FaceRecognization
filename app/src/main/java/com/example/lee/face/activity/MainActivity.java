package com.example.lee.face.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.example.lee.face.R;
import com.example.lee.face.dialog.UploadDialog;
import com.example.lee.face.view.EmiyaCameraView;
import com.google.android.cameraview.CameraView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.emiyalee.Activity.Activity;
import app.emiyalee.Http.Header;
import app.emiyalee.Http.Http;
import app.emiyalee.Http.UrlParam;
import app.emiyalee.Permissions.Permissions;
import app.emiyalee.Util.Util;
import mil.nga.tiff.util.TiffConstants;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends Activity {

    private EmiyaCameraView cameraView;
    private Boolean cameraStart = false;
    private ImageView imageView;
    private static int FRONT_ROTATION_DEGREE = 0;
    private static int BACK_ROTATION_DEGREE = 0;
    //    private static int FRONT_ROTATION_DEGREE = 270;
//    private static int BACK_ROTATION_DEGREE = 90;
    private static int sampleInterval = 200;
    private Button btnCapture;
    private Button btnFix;
    private Button btnFacing;
    private Button btnUpload;
    private Button btnLearn;
    private Button btnPhoto;
    private Button btnVideo;
    private TextView result;
    private Thread videoThread = null;
    private CameraView.Callback cameraCallback = null;
    private Map<String, Integer> multiResults = new HashMap<>();
    private long lastSample = new Date().getTime();
    private static int MAX_SIZE = 50 * 4;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraView = (EmiyaCameraView) findViewById(R.id.camera);
        imageView = (ImageView) findViewById(R.id.img);
        btnFacing = (Button) findViewById(R.id.facing);
        btnUpload = (Button) findViewById(R.id.upload);
        btnLearn = (Button) findViewById(R.id.learn);
        btnPhoto = (Button) findViewById(R.id.photo);
        btnVideo = (Button) findViewById(R.id.video);
        result = (TextView) findViewById(R.id.result);
        btnCapture = (Button) findViewById(R.id.capture);
        btnFix = (Button) findViewById(R.id.fix);
        cameraView.enableViewCapture(true);
        //cameraView.imageView = (ImageView) findViewById(R.id.img2);

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraView.usingViewCapture()) {
                    btnCapture.setText("使用屏幕截图");
                    cameraView.enableViewCapture(false);
                } else {
                    btnCapture.setText("使用系统摄像");
                    cameraView.enableViewCapture(true);
                }
            }
        });

        btnFix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity.BACK_ROTATION_DEGREE != 0 || MainActivity.FRONT_ROTATION_DEGREE != 0) {
                    btnFix.setText("启用重力修正");
                    MainActivity.BACK_ROTATION_DEGREE = 0;
                    MainActivity.FRONT_ROTATION_DEGREE = 0;
                } else {
                    btnFix.setText("禁用重力修正");
                    MainActivity.BACK_ROTATION_DEGREE = 90;
                    MainActivity.FRONT_ROTATION_DEGREE = 270;
                }
            }
        });

        btnFacing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraCallback != null) {
                    cameraView.removeCallback(cameraCallback);
                    cameraCallback = null;
                }
                if (videoThread != null) {
                    try {
                        videoThread.interrupt();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        videoThread.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        videoThread.destroy();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    videoThread = null;

                }
                btnVideo.setText("开始视频识别");
                if (cameraView.getFacing() == CameraView.FACING_FRONT)
                    cameraView.setFacing(CameraView.FACING_BACK);
                else
                    cameraView.setFacing(CameraView.FACING_FRONT);
                cameraView.setAutoFocus(true);
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (cameraCallback != null) {
                    cameraView.removeCallback(cameraCallback);
                }

                cameraCallback = new CameraView.Callback() {
                    @Override
                    public void onPictureTaken(CameraView cameraView, byte[] data) {
                        super.onPictureTaken(cameraView, data);
                        cameraView.removeCallback(this);
                        cameraCallback = null;
                        UploadDialog uploadDialog = new UploadDialog(MainActivity.this, data, cameraView.getFacing() == CameraView.FACING_FRONT ? MainActivity.FRONT_ROTATION_DEGREE : MainActivity.BACK_ROTATION_DEGREE, MainActivity.this);
                        uploadDialog.show();
                    }
                };

                cameraView.addCallback(cameraCallback);
                cameraView.setAutoFocus(true);
                cameraView.takePicture();


            }
        });

        btnLearn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                        progressDialog.show();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Header header = new Header();
                                    UrlParam urlParam = new UrlParam();
                                    final String response = Http.getInstance().get("http://122.114.175.20:8080/retrain", urlParam, header);
                                    if (response.trim().equals("retrainSVM is done.")) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(MainActivity.this, "学习成功", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } else {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(MainActivity.this, "学习失败", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(MainActivity.this, "学习接口调用失败", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } finally {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressDialog.dismiss();
                                        }
                                    });
                                }
                            }
                        }).start();

                    }
                });
            }
        });


        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (cameraCallback != null) {
                    cameraView.removeCallback(cameraCallback);
                }
                cameraCallback = new CameraView.Callback() {
                    @Override
                    public void onPictureTaken(CameraView cameraView, final byte[] data) {
                        super.onPictureTaken(cameraView, data);
                        cameraView.removeCallback(this);
                        cameraCallback = null;
                        final CameraView _cv = cameraView;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                                progressDialog.show();


                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Header header = new Header();
                                            UrlParam urlParam = new UrlParam();
                                            Map<String, String> body = new HashMap<String, String>();
                                            body.put("pic", Util.getBase64FromByte(data, MAX_SIZE, _cv.getFacing() == CameraView.FACING_FRONT ? MainActivity.FRONT_ROTATION_DEGREE : MainActivity.BACK_ROTATION_DEGREE));
                                            String json = JSON.toJSONString(body);
                                            final Response response = Http.getInstance().post2("http://122.114.175.20:8080/recognize", json, urlParam, header, null);
                                            if (response.code() == 200) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            String res = response.body().string();
                                                            if (!res.equals("")) {
                                                                //Toast.makeText(MainActivity.this, "识别结果:" + res, Toast.LENGTH_LONG).show();
                                                                String[] result = res.split(",");
                                                                Boolean flag = false;
                                                                for (String s : result) {
                                                                    if (multiResults.containsKey(s)) {
                                                                        multiResults.put(s, multiResults.get(s) + 1);
                                                                    } else {
                                                                        multiResults.put(s, 1);
                                                                        flag = true;
                                                                    }

                                                                }
                                                                if (flag)
                                                                    displayResult();
                                                            } else
                                                                Toast.makeText(MainActivity.this, "无识别结果", Toast.LENGTH_LONG).show();
                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                            Toast.makeText(MainActivity.this, "识别数据解析失败", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                            } else {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(MainActivity.this, "识别失败", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(MainActivity.this, "识别接口调用失败", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } finally {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    progressDialog.dismiss();
                                                }
                                            });
                                        }
                                    }
                                }).start();

                            }
                        });

                    }
                };

                cameraView.addCallback(cameraCallback);
                result.setText("");
                multiResults = new HashMap<String, Integer>();
                cameraView.takePicture();


            }
        });

        btnVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoThread != null) {
                    btnVideo.setText("开始录像识别");
                    cameraView.removeCallback(cameraCallback);
                    cameraCallback = null;
                    stopThread(videoThread);
                    videoThread = null;
                    controlButtons(true);
                    cameraView.setAutoFocus(true);
                    return;
                }
                if (cameraCallback != null) {
                    cameraView.removeCallback(cameraCallback);
                }
                btnVideo.setText("停止录像识别");
                final CameraView _cv = cameraView;
                cameraCallback = new CameraView.Callback() {
                    @Override
                    public void onPictureTaken(CameraView cameraView, final byte[] data) {
                        super.onPictureTaken(cameraView, data);


                        lastSample = new Date().getTime();
                        videoThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Header header = new Header();
                                    UrlParam urlParam = new UrlParam();
                                    Map<String, String> body = new HashMap<String, String>();
                                    body.put("pic", Util.getBase64FromByte(data, MAX_SIZE, _cv.getFacing() == CameraView.FACING_FRONT ? MainActivity.FRONT_ROTATION_DEGREE : MainActivity.BACK_ROTATION_DEGREE));
                                    String json = JSON.toJSONString(body);
                                    final Response response = Http.getInstance().post2("http://122.114.175.20:8080/recognize", json, urlParam, header, null);
                                    if (response.code() == 200) {
                                        try {
                                            String[] result = response.body().string().split(",");
                                            Boolean flag = false;
                                            for (String s : result) {
                                                if (multiResults.containsKey(s)) {
                                                    multiResults.put(s, multiResults.get(s) + 1);
                                                } else {
                                                    multiResults.put(s, 1);
                                                    flag = true;
                                                }

                                            }
                                            if (flag)
                                                displayResult();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } finally {
                                    if (new Date().getTime() - lastSample >= sampleInterval) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                _cv.takePicture();
                                            }
                                        });
                                    } else {
                                        try {
                                            Thread.sleep(sampleInterval - (new Date().getTime() - lastSample));
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        } finally {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    _cv.takePicture();
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        });

                        videoThread.start();


                    }
                };

                cameraView.addCallback(cameraCallback);
                controlButtons(false);
                result.setText("");
                multiResults = new HashMap<String, Integer>();
                if (!cameraView.usingViewCapture())
                    cameraView.setAutoFocus(false);
                cameraView.takePicture();
            }
        });


    }

    private void displayResult() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String s = "";
                for (String key : multiResults.keySet()) {
                    if (!s.equals("")) {
                        s = s + "\n" + key;
                    } else
                        s = s + key;
                    //System.out.println("key= "+ key + " and value= " + map.get(key));
                }
                result.setText(s);
            }
        });
    }

    public void stopThread(Thread thread) {
        if (thread != null) {
            try {
                thread.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                thread.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                thread.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void stopCamera() {
        if (cameraCallback != null) {
            cameraView.removeCallback(cameraCallback);
            cameraCallback = null;
        }
        if (videoThread != null) {
            try {
                videoThread.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                videoThread.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                videoThread.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
            videoThread = null;

        }
        btnVideo.setText("开始视频识别");
        controlButtons(true);
        cameraView.setAutoFocus(true);
        cameraView.stop();
    }


    private void controlButtons(Boolean b) {
        btnFacing.setClickable(b);
        btnUpload.setClickable(b);
        btnLearn.setClickable(b);
        btnPhoto.setClickable(b);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Permissions.isLackPermission(MainActivity.this, Manifest.permission.CAMERA) && !Permissions.isLackPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            cameraView.start();
            cameraView.setAutoFocus(true);
        } else {
            List<String> per = new ArrayList<>();
            per.add(Manifest.permission.CAMERA);
            per.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            Permissions.request(MainActivity.this, per, 100);
        }

    }

    @Override
    protected void onPause() {
        stopCamera();
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            cameraView.start();
            cameraView.setAutoFocus(true);
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//
//                }
//            });
        }

    }
}
