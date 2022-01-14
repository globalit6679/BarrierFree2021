/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.detection;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;

import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;

public abstract class CameraActivity extends AppCompatActivity
    implements OnImageAvailableListener,
        Camera.PreviewCallback,
        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener,
        TextToSpeech.OnInitListener {
  private static final Logger LOGGER = new Logger();

  private static final int PERMISSIONS_REQUEST = 1;

  private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
  protected int previewWidth = 0;
  protected int previewHeight = 0;
  private boolean debug = false;
  private Handler handler;
  private HandlerThread handlerThread;
  private boolean useCamera2API;
  private boolean isProcessingFrame = false;
  private byte[][] yuvBytes = new byte[3][];
  private int[] rgbBytes = null;
  private int yRowStride;
  private Runnable postInferenceCallback;
  private Runnable imageConverter;

  private LinearLayout bottomSheetLayout;
  private LinearLayout gestureLayout;
  private BottomSheetBehavior<LinearLayout> sheetBehavior;

  //protected TextView frameValueTextView, cropValueTextView, inferenceTimeTextView;



  protected ImageView bottomSheetArrowImageView;
  private ImageView plusImageView, minusImageView;
  private SwitchCompat apiSwitchCompat;
  private TextView threadsTextView;

  //수정
  public String serch_product = "";
  public boolean serch_mode = false;
  protected TextView nameText;
  protected TextView priceText;
  protected TextView infoText;
  protected TextView locationText;
  protected TextView bukketText;
  protected TextView allpriceText;
  protected boolean pick_up = false;
  protected LinearLayout productscreen;
  protected LinearLayout bukketscreen;
  private Button cancelButton ;
  private Button bukketButton ;
  private Button mainButton ;
  private Button bukket_back ;

  private Button bsk_Button ;
  protected FrameLayout leftupscreen;
  protected FrameLayout rightupscreen;
  protected FrameLayout leftdownscreen;
  protected FrameLayout rightdownscreen;
  protected TextToSpeech tts;
  protected boolean leftuptts = true;
  protected boolean rightuptts = true;
  protected boolean leftdowntts = true;
  protected boolean rightdowntts = true;
  protected boolean producttts = true;
  protected boolean baskettts = true;


  private Database_SQL shop_database;
  private ArrayList<ShopInfo> shopList = new ArrayList<ShopInfo>();
  int totalPrice = 0;

  public String parsing = "";




  @Override
  protected void onCreate(final Bundle savedInstanceState){
    LOGGER.d("onCreate " + this);
    super.onCreate(null);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setContentView(R.layout.tfe_od_activity_camera);
    //Toolbar toolbar = findViewById(R.id.toolbar);
    //setSupportActionBar(toolbar);
    //getSupportActionBar().setDisplayShowTitleEnabled(false);
    tts = new TextToSpeech(getApplicationContext(), this);

    if (hasPermission()) {
      setFragment();
    } else {
      requestPermission();
    }
    //수정
    nameText = findViewById(R.id.product_name);
    priceText = findViewById(R.id.product_price);
    infoText = findViewById(R.id.product_info);
    locationText = findViewById(R.id.location);
    productscreen = findViewById(R.id.product_screen);
    cancelButton = findViewById(R.id.cancelbutton);
    bukketButton = findViewById(R.id.bukketbutton);
    mainButton = findViewById(R.id.home_button);
    leftupscreen = findViewById(R.id.leftup);
    rightupscreen = findViewById(R.id.rightup);
    leftdownscreen = findViewById(R.id.leftdown);
    rightdownscreen = findViewById(R.id.rightdown);
    bukketscreen = findViewById(R.id.bukket_screen);
    bukket_back = findViewById(R.id.bukket_back);
    bukketText = findViewById(R.id.bukkettext);
    allpriceText = findViewById(R.id.allprice);
    bsk_Button = findViewById(R.id.basket_button);


    final String[] product_name = {""};
    final String[] product_price = {""};
    final String[] product_info = {""};
    threadsTextView = findViewById(R.id.threads);
    plusImageView = findViewById(R.id.plus);
    minusImageView = findViewById(R.id.minus);
    apiSwitchCompat = findViewById(R.id.api_info_switch);
    bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
    gestureLayout = findViewById(R.id.gesture_layout);
    //sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
    bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);
    final boolean[] shop = {false};

    Intent intent = getIntent();
    serch_product = intent.getExtras().getString("name");
    if (intent.getExtras().getString("mode").equals("serch")){
      serch_mode = true;
    }
    else{
      serch_mode = false;
    }


    cancelButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        parsing = "";
        pick_up = false;
        productVisible(false);
        producttts = true;
      }
    });
    mainButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        finish();
      }
    });

    shop_database = Database_SQL.getInstance(getApplicationContext());
    shop_database.open();

    String finalA = product_name[0];
    String finalB = product_price[0];
    String finalC = product_info[0];

    bsk_Button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (!shop[0]){
          shop[0] = true;
          //포커스
          bsk_Button.setBackgroundColor(Color.parseColor("#FFAC00"));
          bukketscreen.setVisibility(View.VISIBLE);
          flush_speak("장바구니에 담겨있는 상품은"+ viewshop() +"이며 총액은 "+viewBookAll2()+"입니다.");
        }
        else {
          bukketscreen.setVisibility(View.INVISIBLE);
          bsk_Button.setBackgroundColor(Color.parseColor("#FFC000"));
          shop[0] = false;
        }

      }
    });

    //final int[] allprice = {0};
    bukketButton.setOnClickListener(new Button.OnClickListener() {
      @Override
      public void onClick(View view) {
        producttts = true;
        product_name[0] = nameText.getText().toString();
        product_price[0] = priceText.getText().toString();
        product_info[0] = infoText.getText().toString();
        String shop_list = "";
        //allprice[0] += Integer.valueOf(product_price[0]);
        shop_database.insertRecord2(product_name[0], product_price[0]);

        /*
        for(int i = 0; i <= shopList.size(); i++){
          Log.d("ddweryeuyrwtqr",""+shopList.size());
          shop_list += ""+shop_database.selectAll().get(i).getName();
          shop_list += " : ";
          shop_list += ""+shop_database.selectAll().get(i).getPrice()+"\n";
          //Log.d("dd",shop_list);
        }*/

        productVisible(false);
        bukketText.setText(viewshop());
        flush_speak(product_name[0]+"상품이 장바구니에 담겼습니다. 총액은"+viewBookAll2()+"입니다.");
        allpriceText.setText("총액 : "+viewBookAll2());
        bukketscreen.setVisibility(View.VISIBLE);
      }
    });

    bukket_back.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        parsing = "";
        pick_up = false;
        product_name[0] = "";
        product_price[0] = "";
        product_info[0] = "";
        bukketscreen.setVisibility(View.INVISIBLE);
      }
    });

    /*
    ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
    vto.addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
              gestureLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            } else {
              gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
            //                int width = bottomSheetLayout.getMeasuredWidth();
            int height = gestureLayout.getMeasuredHeight();

            sheetBehavior.setPeekHeight(height);
          }
        });
    sheetBehavior.setHideable(false);
*/
/*
    sheetBehavior.setBottomSheetCallback(
        new BottomSheetBehavior.BottomSheetCallback() {
          @Override
          public void onStateChanged(@NonNull View bottomSheet, int newState) {
            switch (newState) {
              case BottomSheetBehavior.STATE_HIDDEN:
                break;
              case BottomSheetBehavior.STATE_EXPANDED:
                {
                  bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
                }
                break;
              case BottomSheetBehavior.STATE_COLLAPSED:
                {
                  bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                }
                break;
              case BottomSheetBehavior.STATE_DRAGGING:
                break;
              case BottomSheetBehavior.STATE_SETTLING:
                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                break;
            }
          }

          @Override
          public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
*/
    //frameValueTextView = findViewById(R.id.frame_info);
    //cropValueTextView = findViewById(R.id.crop_info);
    //inferenceTimeTextView = findViewById(R.id.inference_info);

//    apiSwitchCompat.setOnCheckedChangeListener(this);

//    plusImageView.setOnClickListener(this);
//    minusImageView.setOnClickListener(this);
  }

  private int viewBookAll2(){
    ShopInfo shopInfo = null;
    shopList = shop_database.selectAll();

    totalPrice = 0;
    for(int i = 0; i < shopList.size(); i++){
      shopInfo = shopList.get(i);
      String strPrice = shopInfo.getPrice();
      String intPrice = strPrice.replaceAll("[^0-9]","");
      totalPrice += Integer.parseInt(intPrice);
    }
    return totalPrice;
  }
  private String viewshop(){
    ShopInfo shopInfo = null;
    shopList = shop_database.selectAll();

    String shop_list = "";
    for(int i = 0; i < shopList.size(); i++){
      shopInfo = shopList.get(i);
      String strPrice = shopInfo.getPrice();
      shop_list += ""+shopInfo.getName();
      shop_list += " : ";
      shop_list += ""+shopInfo.getPrice()+"\n";
    }
    return shop_list;
  }

  protected int[] getRgbBytes() {
    imageConverter.run();
    return rgbBytes;
  }

  protected int getLuminanceStride() {
    return yRowStride;
  }

  protected byte[] getLuminance() {
    return yuvBytes[0];
  }

  /** Callback for android.hardware.Camera API */
  @Override
  public void onPreviewFrame(final byte[] bytes, final Camera camera) {
    if (isProcessingFrame) {
      LOGGER.w("Dropping frame!");
      return;
    }

    try {
      // Initialize the storage bitmaps once when the resolution is known.
      if (rgbBytes == null) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        previewHeight = previewSize.height;
        previewWidth = previewSize.width;
        rgbBytes = new int[previewWidth * previewHeight];
        onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
      }
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      return;
    }

    isProcessingFrame = true;
    yuvBytes[0] = bytes;
    yRowStride = previewWidth;

    imageConverter =
        new Runnable() {
          @Override
          public void run() {
            ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
          }
        };

    postInferenceCallback =
        new Runnable() {
          @Override
          public void run() {
            camera.addCallbackBuffer(bytes);
            isProcessingFrame = false;
          }
        };
    processImage();
  }

  /** Callback for Camera2 API */
  @Override
  public void onImageAvailable(final ImageReader reader) {
    // We need wait until we have some size from onPreviewSizeChosen
    if (previewWidth == 0 || previewHeight == 0) {
      return;
    }
    if (rgbBytes == null) {
      rgbBytes = new int[previewWidth * previewHeight];
    }
    try {
      final Image image = reader.acquireLatestImage();

      if (image == null) {
        return;
      }

      if (isProcessingFrame) {
        image.close();
        return;
      }
      isProcessingFrame = true;
      Trace.beginSection("imageAvailable");
      final Plane[] planes = image.getPlanes();
      fillBytes(planes, yuvBytes);
      yRowStride = planes[0].getRowStride();
      final int uvRowStride = planes[1].getRowStride();
      final int uvPixelStride = planes[1].getPixelStride();

      imageConverter =
          new Runnable() {
            @Override
            public void run() {
              ImageUtils.convertYUV420ToARGB8888(
                  yuvBytes[0],
                  yuvBytes[1],
                  yuvBytes[2],
                  previewWidth,
                  previewHeight,
                  yRowStride,
                  uvRowStride,
                  uvPixelStride,
                  rgbBytes);
            }
          };

      postInferenceCallback =
          new Runnable() {
            @Override
            public void run() {
              image.close();
              isProcessingFrame = false;
            }
          };

      processImage();
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      Trace.endSection();
      return;
    }
    Trace.endSection();
  }

  @Override
  public synchronized void onStart() {
    LOGGER.d("onStart " + this);
    super.onStart();
  }

  @Override
  public synchronized void onResume() {
    LOGGER.d("onResume " + this);
    super.onResume();

    handlerThread = new HandlerThread("inference");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  @Override
  public synchronized void onPause() {
    LOGGER.d("onPause " + this);

    handlerThread.quitSafely();
    try {
      handlerThread.join();
      handlerThread = null;
      handler = null;
    } catch (final InterruptedException e) {
      LOGGER.e(e, "Exception!");
    }

    super.onPause();
  }

  @Override
  public synchronized void onStop() {
    LOGGER.d("onStop " + this);
    super.onStop();
  }

  @Override
  public synchronized void onDestroy() {
    LOGGER.d("onDestroy " + this);
    super.onDestroy();
  }

  protected synchronized void runInBackground(final Runnable r) {
    if (handler != null) {
      handler.post(r);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      final int requestCode, final String[] permissions, final int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == PERMISSIONS_REQUEST) {
      if (allPermissionsGranted(grantResults)) {
        setFragment();
      } else {
        requestPermission();
      }
    }
  }

  private static boolean allPermissionsGranted(final int[] grantResults) {
    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private boolean hasPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
    } else {
      return true;
    }
  }

  private void requestPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
        Toast.makeText(
                CameraActivity.this,
                "Camera permission is required for this demo",
                Toast.LENGTH_LONG)
            .show();
      }
      requestPermissions(new String[] {PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
    }
  }

  // Returns true if the device supports the required hardware level, or better.
  private boolean isHardwareLevelSupported(
      CameraCharacteristics characteristics, int requiredLevel) {
    int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
    if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
      return requiredLevel == deviceLevel;
    }
    // deviceLevel is not LEGACY, can use numerical sort
    return requiredLevel <= deviceLevel;
  }

  private String chooseCamera() {
    final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    try {
      for (final String cameraId : manager.getCameraIdList()) {
        final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

        // We don't use a front facing camera in this sample.
        final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
          continue;
        }

        final StreamConfigurationMap map =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
          continue;
        }

        // Fallback to camera1 API for internal cameras that don't have full support.
        // This should help with legacy situations where using the camera2 API causes
        // distorted or otherwise broken previews.
        useCamera2API =
            (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                || isHardwareLevelSupported(
                    characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        LOGGER.i("Camera API lv2?: %s", useCamera2API);
        return cameraId;
      }
    } catch (CameraAccessException e) {
      LOGGER.e(e, "Not allowed to access camera");
    }

    return null;
  }

  protected void setFragment() {
    String cameraId = chooseCamera();

    Fragment fragment;
    if (useCamera2API) {
      CameraConnectionFragment camera2Fragment =
          CameraConnectionFragment.newInstance(
              new CameraConnectionFragment.ConnectionCallback() {
                @Override
                public void onPreviewSizeChosen(final Size size, final int rotation) {
                  previewHeight = size.getHeight();
                  previewWidth = size.getWidth();
                  CameraActivity.this.onPreviewSizeChosen(size, rotation);
                }
              },
              this,
              getLayoutId(),
              getDesiredPreviewFrameSize());

      camera2Fragment.setCamera(cameraId);
      fragment = camera2Fragment;
    } else {
      fragment =
          new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
    }

    getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
  }

  protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (int i = 0; i < planes.length; ++i) {
      final ByteBuffer buffer = planes[i].getBuffer();
      if (yuvBytes[i] == null) {
        LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
        yuvBytes[i] = new byte[buffer.capacity()];
      }
      buffer.get(yuvBytes[i]);
    }
  }

  public boolean isDebug() {
    return debug;
  }

  protected void readyForNextImage() {
    if (postInferenceCallback != null) {
      postInferenceCallback.run();
    }
  }

  protected int getScreenOrientation() {
    switch (getWindowManager().getDefaultDisplay().getRotation()) {
      case Surface.ROTATION_270:
        return 270;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_90:
        return 90;
      default:
        return 0;
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    setUseNNAPI(isChecked);
    if (isChecked) apiSwitchCompat.setText("NNAPI");
    else apiSwitchCompat.setText("TFLITE");
  }

  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.plus) {
      String threads = threadsTextView.getText().toString().trim();
      int numThreads = Integer.parseInt(threads);
      if (numThreads >= 9) return;
      numThreads++;
      threadsTextView.setText(String.valueOf(numThreads));
      setNumThreads(numThreads);
    } else if (v.getId() == R.id.minus) {
      String threads = threadsTextView.getText().toString().trim();
      int numThreads = Integer.parseInt(threads);
      if (numThreads == 1) {
        return;
      }
      numThreads--;
      threadsTextView.setText(String.valueOf(numThreads));
      setNumThreads(numThreads);
    }
  }
  protected void product_nameText(String name) {
    nameText.setText(name);
  }
  protected void product_priceText(String price) {
    priceText.setText(price);
  }
  protected void product_infoText(String info) {
    infoText.setText(info);
  }
  protected void location_Text(String location) {
    locationText.setText(location);
  }

  protected void productVisible(boolean visible) {
    if (visible){
      productscreen.setVisibility(View.VISIBLE);
    }
    else {
      productscreen.setVisibility(View.INVISIBLE);
    }

  }


/*
  protected void showFrameInfo(String frameInfo) {
    frameValueTextView.setText(frameInfo);
  }

  protected void showCropInfo(String cropInfo) {
    cropValueTextView.setText(cropInfo);
  }

  protected void showInference(String inferenceTime) {
    inferenceTimeTextView.setText(inferenceTime);
  }
*/
  protected abstract void processImage();

  protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

  protected abstract int getLayoutId();

  protected abstract Size getDesiredPreviewFrameSize();

  protected abstract void setNumThreads(int numThreads);

  protected abstract void setUseNNAPI(boolean isChecked);

  public void onInit(int status) {
    if(status == TextToSpeech.SUCCESS){
      int result = tts.setLanguage(Locale.KOREA);
      tts.setSpeechRate(1);
      tts.setPitch(1);
      if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
        Log.d("TTS", "Language not supported");
      }
    }
    else {
      Log.d("TextToSpeech","Initialization failed");
    }

  }

  private void flush_speak(String message) {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
    }
  }

  private void add_speak(String message){
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      tts.speak(message, TextToSpeech.QUEUE_ADD, null, null);
    }
  }
}

