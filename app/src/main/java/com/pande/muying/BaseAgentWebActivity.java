package com.pande.muying;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.pande.muying.utils.X5WebView;
import com.tencent.smtt.export.external.extension.interfaces.IX5WebViewExtension;
import com.tencent.smtt.export.external.interfaces.GeolocationPermissionsCallback;
import com.tencent.smtt.export.external.interfaces.JsPromptResult;
import com.tencent.smtt.export.external.interfaces.JsResult;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class BaseAgentWebActivity extends AppCompatActivity {
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mUploadCallbackAboveL;
    private final static int PHOTO_REQUEST = 100;
    private final static int VIDEO_REQUEST = 120;
    private final static String url = "http://www.pandamuying.com/crm";
    //    private final static String url = "http://192.168.1.85:8077/crm";http://www.pandamuying.com/attachs/2018/09/28/5bad984018de5.png
    //    private final static String url = "http://soft.imtt.qq.com/browser/tes/feedback.html";
    private boolean videoFlag = false;
    private X5WebView webView;
    private String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        webView = findViewById(R.id.certifi_webview);
        if (Build.VERSION.SDK_INT >= 23) {
            int checkPermission = ContextCompat.checkSelfPermission(BaseAgentWebActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
            if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(BaseAgentWebActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                ActivityCompat.requestPermissions(BaseAgentWebActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }


            int checkPermission1 = ContextCompat.checkSelfPermission(BaseAgentWebActivity.this, Manifest.permission.CAMERA);
            if (checkPermission1 != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(BaseAgentWebActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
//                ActivityCompat.requestPermissions(BaseAgentWebActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
        initWebViewSetting();
    }

    private void initWebViewSetting() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(false);
        settings.setLoadWithOverviewMode(true);
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setAllowContentAccess(true); // 是否可访问Content Provider的资源，默认值 true
        settings.setAllowFileAccess(true);    // 是否可访问本地文件，默认值 true
        // 是否允许通过file url加载的Javascript读取本地文件，默认值 false
        if (Build.VERSION.SDK_INT >= 16) {

            settings.setAllowFileAccessFromFileURLs(false);
            // 是否允许通过file url加载的Javascript读取全部资源(包括文件,http,https)，默认值 false
            settings.setAllowUniversalAccessFromFileURLs(false);
        }

        if (Build.VERSION.SDK_INT >= 19) {
            settings.setUseWideViewPort(true);
        }
        //设置定位的数据库路径
        String dir = this.getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
//        settings.setGeolocationDatabasePath(dir);
        settings.setGeolocationEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setAppCacheMaxSize(Long.MAX_VALUE);
        settings.setAppCachePath(this.getDir("appcache", 0).getPath());
        settings.setGeolocationDatabasePath(dir);
        settings.setDomStorageEnabled(true);

        //开启JavaScript支持
        // 支持缩放
        settings.setSupportZoom(true);
        //辅助WebView设置处理关于页面跳转，页面请求等操作
        webView.setWebViewClient(new MyWebViewClient());
        //辅助WebView处理图片上传操作
        webView.setWebChromeClient(new MyChromeWebClient());
        //加载地址
        webView.loadUrl(url);
        IX5WebViewExtension ix5 = webView.getX5WebViewExtension();
        if (null != ix5) {
            ix5.setScrollBarFadingEnabled(false);
        }
    }


    //自定义 WebViewClient 辅助WebView设置处理关于页面跳转，页面请求等操作【处理tel协议和视频通讯请求url的拦截转发】
    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.i("url", url);

//            Logger.e(url);
//            view.loadUrl(url);
            if (!TextUtils.isEmpty(url)) {
                videoFlag = url.contains("vedio");
            }
            if (url.trim().startsWith("tel")) {//特殊情况tel，调用系统的拨号软件拨号【<a href="tel:1111111111">1111111111</a>】
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            } else if (url.startsWith("qqmap://map/routeplan")) {
                openTencent(url);
            } else {
                String port = url.substring(url.lastIndexOf(":") + 1, url.lastIndexOf("/"));//尝试要拦截的视频通讯url格式(808端口)：【http://xxxx:808/?roomName】
                if (port.equals("808")) {//特殊情况【若打开的链接是视频通讯地址格式则调用系统浏览器打开】
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                } else {//其它非特殊情况全部放行
                    view.loadUrl(url);
                }
            }
            return true;
        }
    }

    /**
     * 打开腾讯地图（公交出行，起点位置使用地图当前位置）
     * <p>
     * 公交：type=bus，policy有以下取值
     * 0：较快捷 、 1：少换乘 、 2：少步行 、 3：不坐地铁
     * 驾车：type=drive，policy有以下取值
     * 0：较快捷 、 1：无高速 、 2：距离短
     * policy的取值缺省为0
     */
    private void openTencent(String s) {
        if (checkMapAppsIsExist(this, "com.tencent.map")) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(s));
            startActivity(intent);
        } else {
            Toast.makeText(BaseAgentWebActivity.this, "腾讯地图未安装", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 检测地图应用是否安装
     *
     * @param context
     * @param packagename
     * @return
     */
    public boolean checkMapAppsIsExist(Context context, String packagename) {
        //获取packagemanager
        final PackageManager packageManager = context.getPackageManager();
        //获取所有已安装程序的包信息
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
        //用于存储所有已安装程序的包名
        List<String> packageNames = new ArrayList<>();
        //从pinfo中将包名字逐一取出，压入pName list中
        if (packageInfos != null) {
            for (int i = 0; i < packageInfos.size(); i++) {
                String packName = packageInfos.get(i).packageName;
                packageNames.add(packName);
            }
        }
        //判断packageNames中是否有目标程序的包名，有TRUE，没有FALSE
        return packageNames.contains(packagename);

    }

    /**
     * 自定义压缩存储地址
     *
     * @return
     */
    private String getPath() {
        String path = Environment.getExternalStorageDirectory() + "/Luban/image/";
        File file = new File(path);
        if (file.mkdirs()) {
            return path;
        }
        return path;
    }

    private File fileUri = new File(Environment.getExternalStorageDirectory().getPath() + "/" + System.currentTimeMillis() + ".jpg");
    private Uri imageUri;

    //自定义 WebChromeClient 辅助WebView处理图片上传操作【<input type=file> 文件上传标签】
    public class MyChromeWebClient extends WebChromeClient {
        // For Android 3.0-
        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            Log.d("TAG", "openFileChoose(ValueCallback<Uri> uploadMsg)");
            mUploadMessage = uploadMsg;
            if (videoFlag) {
                recordVideo();
            } else {
                takePhoto();
            }

        }

        // For Android 3.0+
        public void openFileChooser(ValueCallback uploadMsg, String acceptType) {
            Log.d("TAG", "openFileChoose( ValueCallback uploadMsg, String acceptType )");
            mUploadMessage = uploadMsg;
            if (videoFlag) {
                recordVideo();
            } else {
                takePhoto();
            }
        }

        //For Android 4.1
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            Log.d("TAG", "openFileChoose(ValueCallback<Uri> uploadMsg, String acceptType, String capture)");
            mUploadMessage = uploadMsg;
            if (videoFlag) {
                recordVideo();
            } else {
               /* new AlertDialog.Builder(BaseAgentWebActivity.this)
                        .setItems(new String[]{"拍照", "相册"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        takePhoto();

                                        break;
                                    case 1:
                                        PhotoUtils.openPic(BaseAgentWebActivity.this, PHOTO_REQUEST);

                                        break;
                                }
                            }
                        })
                        .setCancelable(false)
                        .create().show();*/
                PictureSelector.create(BaseAgentWebActivity.this)
                        .openGallery(PictureMimeType.ofImage())//全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()、音频.ofAudio()
                        .theme(R.style.picture_default_style)//主题样式(不设置为默认样式) 也可参考demo values/styles下 例如：R.style.picture.white.style
                        .imageSpanCount(4)// 每行显示个数 int
                        .selectionMode(PictureConfig.SINGLE)// 多选 or 单选 PictureConfig.MULTIPLE or PictureConfig.SINGLE
                        .previewImage(true)// 是否可预览图片 true or false
                        .isCamera(true)// 是否显示拍照按钮 true or false
                        .imageFormat(PictureMimeType.PNG)// 拍照保存图片格式后缀,默认jpeg
                        .isZoomAnim(true)// 图片列表点击 缩放效果 默认true
                        .sizeMultiplier(0.5f)// glide 加载图片大小 0~1之间 如设置 .glideOverride()无效
                        .setOutputCameraPath("/muying")// 自定义拍照保存路径,可不填
                        .compress(true)// 是否压缩 true or false
                        .glideOverride(1000, 1000)// int glide 加载宽高，越小图片列表越流畅，但会影响列表图片浏览的清晰度
                        .isGif(false)// 是否显示gif图片 true or false
                        .compressSavePath(getPath())//压缩图片保存地址
                        .previewEggs(true)// 预览图片时 是否增强左右滑动图片体验(图片滑动一半即可看到上一张是否选中) true or false
                        .minimumCompressSize(100)// 小于100kb的图片不压缩
                        .synOrAsy(true)//同步true或异步false 压缩 默认同步
                        .forResult(PictureConfig.CHOOSE_REQUEST);//结果回调onActivityResult code
            }
        }

        @Override
        public boolean onJsPrompt(WebView webView, String s, String s1, String s2, JsPromptResult jsPromptResult) {
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView webView, String s, String s1, final JsResult jsResult) {
            new AlertDialog.Builder(BaseAgentWebActivity.this).setTitle(s).setMessage(s1)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            jsResult.confirm();
                        }
                    }).create().show();
            return true;
        }

        @Override
        public boolean onJsAlert(WebView webView, String s, String s1, JsResult jsResult) {

            return true;
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(final String s, final GeolocationPermissionsCallback geolocationPermissionsCallback) {
            Log.i("TAG", "dingwei");
            geolocationPermissionsCallback.invoke(s, true, true);
           /* geolocationPermissionsCallback.invoke(s, true, false);*/
           /* final boolean remember = true;
            AlertDialog.Builder builder = new AlertDialog.Builder(BaseAgentWebActivity.this);
            builder.setTitle("位置信息");
            builder.setMessage(s + "允许获取您的地理位置信息吗？").setCancelable(true).setPositiveButton("允许",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int id) {
                            geolocationPermissionsCallback.invoke(s, true, remember);
                        }
                    })
                    .setNegativeButton("不允许",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    geolocationPermissionsCallback.invoke(s, false, remember);
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();*/
            super.onGeolocationPermissionsShowPrompt(s, geolocationPermissionsCallback);
        }
        /*@Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            if (Build.VERSION.SDK_INT >= 23) {
                int checkPermission = ContextCompat.checkSelfPermission(BaseAgentWebActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
                if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(BaseAgentWebActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                    ActivityCompat.requestPermissions(BaseAgentWebActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    Log.d("TTTT", "弹出提示");
                }
            } else {
                callback.invoke(origin, true, false);
            }
            super.onGeolocationPermissionsShowPrompt(origin, callback);
        }*/

        // For Android 5.0+
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            Log.d("TAG", "onShowFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture)");
            mUploadCallbackAboveL = filePathCallback;
            if (videoFlag) {
                recordVideo();
            } else {
                /*new AlertDialog.Builder(BaseAgentWebActivity.this)
                        .setItems(new String[]{"拍照", "相册"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        takePhoto();

                                        break;
                                    case 1:
                                        PhotoUtils.openPic(BaseAgentWebActivity.this, PHOTO_REQUEST);

                                        break;
                                }
                            }
                        })
                        .setCancelable(false)
                        .create().show();*/

                // 进入相册 以下是例子：用不到的api可以不写
                PictureSelector.create(BaseAgentWebActivity.this)
                        .openGallery(PictureMimeType.ofImage())//全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()、音频.ofAudio()
                        .theme(R.style.picture_default_style)//主题样式(不设置为默认样式) 也可参考demo values/styles下 例如：R.style.picture.white.style
                        .imageSpanCount(4)// 每行显示个数 int
                        .selectionMode(PictureConfig.SINGLE)// 多选 or 单选 PictureConfig.MULTIPLE or PictureConfig.SINGLE
                        .previewImage(true)// 是否可预览图片 true or false
                        .isCamera(true)// 是否显示拍照按钮 true or false
                        .imageFormat(PictureMimeType.PNG)// 拍照保存图片格式后缀,默认jpeg
                        .isZoomAnim(true)// 图片列表点击 缩放效果 默认true
                        .sizeMultiplier(0.5f)// glide 加载图片大小 0~1之间 如设置 .glideOverride()无效
                        .setOutputCameraPath("/muying")// 自定义拍照保存路径,可不填
                        .compress(true)// 是否压缩 true or false
                        .glideOverride(1000, 1000)// int glide 加载宽高，越小图片列表越流畅，但会影响列表图片浏览的清晰度
                        .isGif(false)// 是否显示gif图片 true or false
                        .compressSavePath(getPath())//压缩图片保存地址
                        .previewEggs(true)// 预览图片时 是否增强左右滑动图片体验(图片滑动一半即可看到上一张是否选中) true or false
                        .minimumCompressSize(100)// 小于100kb的图片不压缩
                        .synOrAsy(true)//同步true或异步false 压缩 默认同步
                        .forResult(PictureConfig.CHOOSE_REQUEST);//结果回调onActivityResult code
            }
            return true;
        }
    }

    /**
     * 拍照
     */
    private void takePhoto() {
        imageUri = Uri.fromFile(fileUri);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            imageUri = FileProvider.getUriForFile(BaseAgentWebActivity.this, getPackageName() + ".fileprovider", fileUri);//通过FileProvider创建一个content类型的Uri

        }
        PhotoUtils.takePicture(BaseAgentWebActivity.this, imageUri, PHOTO_REQUEST);
    }

    /**
     * 录像
     */
    private void recordVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        //限制时长
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10);
        //开启摄像机
        startActivityForResult(intent, VIDEO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PHOTO_REQUEST) {
            if (null == mUploadMessage && null == mUploadCallbackAboveL) return;
            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
            if (mUploadCallbackAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, data);
            } else if (mUploadMessage != null) {
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }
        } else if (requestCode == VIDEO_REQUEST) {
            if (null == mUploadMessage && null == mUploadCallbackAboveL) return;

            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
            if (mUploadCallbackAboveL != null) {
                if (resultCode == RESULT_OK) {
                    mUploadCallbackAboveL.onReceiveValue(new Uri[]{result});
                    mUploadCallbackAboveL = null;
                } else {
                    mUploadCallbackAboveL.onReceiveValue(new Uri[]{});
                    mUploadCallbackAboveL = null;
                }

            } else if (mUploadMessage != null) {
                if (resultCode == RESULT_OK) {
                    mUploadMessage.onReceiveValue(result);
                    mUploadMessage = null;
                } else {
                    mUploadMessage.onReceiveValue(Uri.EMPTY);
                    mUploadMessage = null;
                }

            }
        } else if (requestCode == PictureConfig.CHOOSE_REQUEST) {
            if (null == mUploadMessage && null == mUploadCallbackAboveL) return;
            if (resultCode == RESULT_OK) {
                if (data != null) {

                    Uri[] results = null;
                    List<LocalMedia> selectList = PictureSelector.obtainMultipleResult(data);
                    if (selectList.size() > 0) {
                        String path = selectList.get(0).getCompressPath();
                        results = new Uri[]{Uri.parse(path)};
                        if (mUploadCallbackAboveL != null) {
                            mUploadCallbackAboveL.onReceiveValue(results);
                            mUploadCallbackAboveL = null;
                        } else if (mUploadMessage != null) {
                            mUploadMessage.onReceiveValue(Uri.parse(path));
                            mUploadMessage = null;
                        }
                    } else {
                        setNull();
                    }
                } else {
                    setNull();
                }

            } else if (resultCode == RESULT_CANCELED){
                setNull();
            }
        }
    }

    private void setNull() {
        if (mUploadCallbackAboveL != null) {
            mUploadCallbackAboveL.onReceiveValue(new Uri[]{});
            mUploadCallbackAboveL = null;
        } else if (mUploadMessage != null) {
            mUploadMessage.onReceiveValue(Uri.EMPTY);
            mUploadMessage = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent data) {
        if (requestCode != PHOTO_REQUEST || mUploadCallbackAboveL == null) {
            return;
        }
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                Log.i("imageUri", imageUri.toString());
                results = new Uri[]{imageUri};
            } else {
                String dataString = data.getDataString();
                ClipData clipData = data.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }

                if (dataString != null)
                    results = new Uri[]{Uri.parse(dataString)};
            }
        }
        mUploadCallbackAboveL.onReceiveValue(results);
        mUploadCallbackAboveL = null;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //如果按下的是回退键且历史记录里确实还有页面
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        } else {
            onBackPressed();
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.setWebViewClient(null);
            webView.setWebChromeClient(null);
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.clearHistory();
//            ((ViewGroup) webView.getParent()).removeView(webView);
            webView.destroy();
            webView = null;
        }
    }
}
