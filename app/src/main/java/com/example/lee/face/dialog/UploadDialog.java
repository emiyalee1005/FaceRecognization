package com.example.lee.face.dialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.example.lee.face.R;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import app.emiyalee.Activity.Activity;
import app.emiyalee.Http.Header;
import app.emiyalee.Http.Http;
import app.emiyalee.Http.UrlParam;
import app.emiyalee.Util.Util;


/**
 * Created by Lee on 2017/2/16.
 */

public class UploadDialog extends Dialog {
    private Activity activity;
    private byte[] data;
    private int degreeFix;
    private EditText editText;
    private Button upload;

    public UploadDialog(@NonNull Context context) {
        super(context);
    }

    public UploadDialog(@NonNull Context context, byte[] data, int degreeFix, Activity activity) {
        super(context);
        this.activity = activity;
        this.data = data;
        this.degreeFix = degreeFix;
    }

    public UploadDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
    }

    public UploadDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.uploaddialog);
        editText = (EditText) findViewById(R.id.username);
        ((ImageView) findViewById(R.id.img)).setImageBitmap(Util.getBitmapFromByte(data, degreeFix));
        final String pic = Util.getBase64FromByte(data, 50, degreeFix);
        upload=(Button)findViewById(R.id.confirm);
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editText.getText().toString().trim().equals("")) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(UploadDialog.this.getContext(), "请输入用户名", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final ProgressDialog progressDialog=new ProgressDialog(UploadDialog.this.getContext());
                            progressDialog.show();

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Header header = new Header();
                                        UrlParam urlParam = new UrlParam();
                                        Map<String, String> data = new HashMap<String, String>();
                                        data.put("pic", pic);
                                        String json = JSON.toJSONString(data);
                                        String response = Http.getInstance().post("http://122.114.175.20:8080/faces/" + editText.getText().toString(), json, urlParam, header, null);
                                        if (response.equals("ok")) {
                                            activity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(UploadDialog.this.getContext(), "上传成功", Toast.LENGTH_SHORT).show();
                                                    UploadDialog.this.dismiss();
                                                }
                                            });
                                        } else {
                                            activity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(UploadDialog.this.getContext(), "上传失败", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(UploadDialog.this.getContext(), "上传接口调用失败", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }finally {
                                        activity.runOnUiThread(new Runnable() {
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
            }
        });

        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UploadDialog.this.dismiss();
            }
        });

    }
}
