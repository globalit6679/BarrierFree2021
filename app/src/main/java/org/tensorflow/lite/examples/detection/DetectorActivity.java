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

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.customview.OverlayView.DrawCallback;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Detector;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();

  // Configuration values for the prepackaged SSD model.
  private static final int TF_OD_API_INPUT_SIZE = 300;
  private static final boolean TF_OD_API_IS_QUANTIZED = true;
  private static final String TF_OD_API_MODEL_FILE = "model.tflite";
  private static final String TF_OD_API_LABELS_FILE = "labelmap.txt";
  private static final DetectorMode MODE = DetectorMode.TF_OD_API;
  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
  private static final boolean MAINTAIN_ASPECT = false;
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;
  OverlayView trackingOverlay;
  private Integer sensorOrientation;

  private Detector detector;

  private long lastProcessingTimeMs;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;

  private boolean computingDetection = false;

  private long timestamp = 0;

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  private MultiBoxTracker tracker;

  private BorderedText borderedText;

  //수정
  private String class_name  = "";
  private String parsing_xy = "";

  private String rocation_info = "";
  private int[] rocation_xy = new int[50]; // [x1,y1,x2,y2]
  private double[] center_xy = new double[2];
  private String parsing_pre = "";






  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    tracker = new MultiBoxTracker(this);
    //center_xy[0] = 0;
    //center_xy[1] = 0;


    int cropSize = TF_OD_API_INPUT_SIZE;

    try {
      detector =
          TFLiteObjectDetectionAPIModel.create(
              this,
              TF_OD_API_MODEL_FILE,
              TF_OD_API_LABELS_FILE,
              TF_OD_API_INPUT_SIZE,
              TF_OD_API_IS_QUANTIZED);
      cropSize = TF_OD_API_INPUT_SIZE;
    } catch (final IOException e) {
      e.printStackTrace();
      LOGGER.e(e, "Exception initializing Detector!");
      Toast toast =
          Toast.makeText(
              getApplicationContext(), "Detector could not be initialized", Toast.LENGTH_SHORT);
      toast.show();
      finish();
    }

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

    frameToCropTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropSize, cropSize,
            sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);

    trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
    trackingOverlay.addCallback(
        new DrawCallback() {
          @Override
          public void drawCallback(final Canvas canvas) {
            tracker.draw(canvas);
            if (isDebug()) {
              tracker.drawDebug(canvas);
            }
          }
        });

    tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
  }

  @Override
  protected void processImage() {
    ++timestamp;
    final long currTimestamp = timestamp;
    trackingOverlay.postInvalidate();

    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage();
      return;
    }
    computingDetection = true;
    LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

    readyForNextImage();

    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap);
    }

    runInBackground(
        new Runnable() {
          @Override
          public void run() {
            LOGGER.i("Running detection on image " + currTimestamp);
            final long startTime = SystemClock.uptimeMillis();
            final List<Detector.Recognition> results = detector.recognizeImage(croppedBitmap);
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            final Canvas canvas = new Canvas(cropCopyBitmap);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Style.STROKE);
            paint.setStrokeWidth(2.0f);

            float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
            switch (MODE) {
              case TF_OD_API:
                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                break;
            }

            final List<Detector.Recognition> mappedRecognitions =
                new ArrayList<Detector.Recognition>();

            for (final Detector.Recognition result : results) {
              final RectF location = result.getLocation();

            if (serch_mode){
              if (location != null && result.getConfidence() >= minimumConfidence && result.getTitle().equals(serch_product)) { //이거다
                canvas.drawRect(location, paint);

                class_name = result.getTitle(); //클래스 가져오기
                parsing = result.getLocation().toString();


                cropToFrameTransform.mapRect(location);
                result.setLocation(location);
                mappedRecognitions.add(result);
              }

            }
            else{
              if (location != null && result.getConfidence() >= minimumConfidence) { //이거다
                canvas.drawRect(location, paint);

                class_name = result.getTitle(); //클래스 가져오기
                parsing = result.getLocation().toString();

                cropToFrameTransform.mapRect(location);
                result.setLocation(location);
                mappedRecognitions.add(result);
            }

            }}


            tracker.trackResults(mappedRecognitions, currTimestamp);
            trackingOverlay.postInvalidate();

            computingDetection = false;

            runOnUiThread( //ui제공
                new Runnable() {
                  @Override
                  public void run() {

                    // 좌표
                    int j = 0;
                    int a = 0;

                    if (parsing.equals(parsing_pre)){
                      parsing = "";
                    }
                    else {
                      parsing_pre = parsing;
                    }

                    try {
                      if (parsing != ""){
                        for (int i = 0; i <= parsing.length()-1; i++) {
                          if (Character.isDigit(parsing.charAt(i)) || parsing.charAt(i) == ',' || parsing.charAt(i) == ')') {
                            if (parsing.charAt(i) == ','|| parsing.charAt(i) == ')') {
                              rocation_xy[j] = Integer.valueOf(parsing_xy)/10;
                              j++;
                              parsing_xy = "";
                            }
                            else {
                              parsing_xy = parsing_xy + parsing.charAt(i);
                            }
                          }
                        }j = 0;
                      }
                      else {
                        Log.d("111","dddd");
                        rocation_xy[0] = 0;
                        rocation_xy[1] = 0;
                        rocation_xy[2] = 0;
                        rocation_xy[3] = 0;
                      }
                    } catch (NumberFormatException e) {
                      e.printStackTrace();
                    }


                    int alpa = 200;

                    center_xy[0] = 0;
                    center_xy[1] = 0;
                    center_xy[0] = (rocation_xy[0]+rocation_xy[2])/2;
                    center_xy[1] = (rocation_xy[1]+rocation_xy[3])/2;

                    if (serch_mode){ //탐색모드
                      //상품 들때
                      if ((center_xy[0] >= 100 && center_xy[0] <= 200) && (center_xy[1] >= 100 && center_xy[1] <= 200) &&(rocation_xy[2]-rocation_xy[0]) * (rocation_xy[3]-rocation_xy[1]) >= alpa * alpa){ //들었을때
                        if (class_name.equals(serch_product)){
                          if (!pick_up){
                            location_Text("");
                            pick_up = true;
                            readChip(class_name);
                            productVisible(true);
                            leftuptts = true;
                            rightuptts = true;
                            leftdowntts = true;
                            rightdowntts = true;
                            if (producttts){
                              producttts = false;
                              Handler handler = new Handler();
                              handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                  nameText.getText().toString();
                                  priceText.getText().toString();
                                  infoText.getText().toString();
                                  flush_speak("지금 집으신 상품은"+nameText.getText().toString()+"이며 가격은"+priceText.getText().toString()+"입니다.");
                                  if (!infoText.getText().toString().equals("NULL")){
                                    add_speak("해당상품은 현재"+ infoText.getText().toString()+ "행사상품입니다.");
                                  }
                                }
                              },2000);
                            }
                          }
                        }
                        else {
                          location_Text("다른거");
                        }
                      }
                      else{
                        // 대략적 위치 제공  ///////아닌거 지우기
                        if (!pick_up){ // 위치에서 사라지면 초기화가 안댐
                          //location_Text(""+parsing);
                          //location_Text(""+center_xy[0]+"   "+center_xy[1]);
                          //location_Text(""+rocation_xy[0]+"   "+rocation_xy[1]+"   "+rocation_xy[2]+"   "+rocation_xy[3]);

                          if (center_xy[0] == 0 && center_xy[1] == 0){ //초기화
                            rocation_info = "";
                            location_Text(rocation_info);
                            leftupscreen.setBackgroundResource(R.color.non_screen);
                            rightupscreen.setBackgroundResource(R.color.non_screen);
                            leftdownscreen.setBackgroundResource(R.color.non_screen);
                            rightdownscreen.setBackgroundResource(R.color.non_screen);
                            leftuptts = true;
                            rightuptts = true;
                            leftdowntts = true;
                            rightdowntts = true;
                          }
                          else if (center_xy[0] <= 150 && center_xy[1] <= 150){ //1사분면
                            rocation_info = "좌측 상단";
                            location_Text(rocation_info);
                            leftupscreen.setBackgroundResource(R.color.focus_screen);
                            rightupscreen.setBackgroundResource(R.color.non_screen);
                            leftdownscreen.setBackgroundResource(R.color.non_screen);
                            rightdownscreen.setBackgroundResource(R.color.non_screen);
                            if (leftuptts){
                              flush_speak("좌측상단에 위치해져있습니다.");
                              leftuptts = false;
                              rightuptts = true;
                              leftdowntts = true;
                              rightdowntts = true;
                            }

                          }
                          else if (center_xy[0] >= 150 && center_xy[1] <= 150){ //2사분면
                            rocation_info = "우측 상단";
                            location_Text(rocation_info);
                            leftupscreen.setBackgroundResource(R.color.non_screen);
                            rightupscreen.setBackgroundResource(R.color.focus_screen);
                            leftdownscreen.setBackgroundResource(R.color.non_screen);
                            rightdownscreen.setBackgroundResource(R.color.non_screen);
                            if (rightuptts){
                              flush_speak("우측상단에 위치해져있습니다.");
                              leftuptts = true;
                              rightuptts = false;
                              leftdowntts = true;
                              rightdowntts = true;
                            }
                          }
                          else if (center_xy[0] <= 150 && center_xy[1] >= 150){ //3사분면
                            rocation_info = "좌측 하단";
                            location_Text(rocation_info);
                            leftupscreen.setBackgroundResource(R.color.non_screen);
                            rightupscreen.setBackgroundResource(R.color.non_screen);
                            leftdownscreen.setBackgroundResource(R.color.focus_screen);
                            rightdownscreen.setBackgroundResource(R.color.non_screen);
                            if (leftdowntts){
                              flush_speak("좌측하단에 위치해져있습니다.");
                              leftuptts = true;
                              rightuptts = true;
                              leftdowntts = false;
                              rightdowntts = true;
                            }
                          }
                          else if (center_xy[0] >= 150 && center_xy[1] >= 150){ //4사분면
                            rocation_info = "우측 하단";
                            location_Text(rocation_info);
                            leftupscreen.setBackgroundResource(R.color.non_screen);
                            rightupscreen.setBackgroundResource(R.color.non_screen);
                            leftdownscreen.setBackgroundResource(R.color.non_screen);
                            rightdownscreen.setBackgroundResource(R.color.focus_screen);
                            if (rightdowntts){
                              flush_speak("우측하단에 위치해져있습니다.");
                              leftuptts = true;
                              rightuptts = true;
                              leftdowntts = true;
                              rightdowntts = false;
                            }
                          }
                        }
                      }
                    }
                    else{
                      if ((center_xy[0] >= 100 && center_xy[0] <= 200) && (center_xy[1] >= 100 && center_xy[1] <= 200) &&(rocation_xy[2]-rocation_xy[0]) * (rocation_xy[3]-rocation_xy[1]) >= alpa * alpa){ //들었을때
                        if (!pick_up){
                          pick_up = true;
                          readChip(class_name);
                          productVisible(pick_up);
                        }
                      }
                    }
                    //testtext(class_name);
                    //testtext2(rocation_info);
                    //showFrameInfo(previewWidth + "x" + previewHeight);
                    //showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                    //showInference(lastProcessingTimeMs + "ms");
                  }
                });
          }
        });
  }

  @Override
  protected int getLayoutId() {
    return R.layout.tfe_od_camera_connection_fragment_tracking;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  @Override
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


  // Which detection model to use: by default uses Tensorflow Object Detection API frozen
  // checkpoints.
  private enum DetectorMode {
    TF_OD_API;
  }

  @Override
  protected void setUseNNAPI(final boolean isChecked) {
    runInBackground(
        () -> {
          try {
            detector.setUseNNAPI(isChecked);
          } catch (UnsupportedOperationException e) {
            LOGGER.e(e, "Failed to set \"Use NNAPI\".");
            runOnUiThread(
                () -> {
                  Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
          }
        });
  }

  @Override
  protected void setNumThreads(final int numThreads) {
    runInBackground(() -> detector.setNumThreads(numThreads));
  }
  public void readChip(String product_name){ //서버 연동
    ArrayList<String> arrayList = new ArrayList<String>();
    FirebaseDatabase.getInstance().getReference().child("cu").child(product_name).child("name").addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot snapshot) {
        Object value = snapshot.getValue(Object.class);
        product_nameText(value.toString());
      }

      @Override
      public void onCancelled(@NonNull DatabaseError error) { }
    });

    FirebaseDatabase.getInstance().getReference().child("cu").child(product_name).child("price").addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot snapshot) {
        Object value = snapshot.getValue(Object.class);
        product_priceText(value.toString());
      }
      @Override
      public void onCancelled(@NonNull DatabaseError error) { }
    });

    FirebaseDatabase.getInstance().getReference().child("cu").child(product_name).child("info").addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot snapshot) {
        Object value = snapshot.getValue(Object.class);
        product_infoText(value.toString());
      }

      @Override
      public void onCancelled(@NonNull DatabaseError error) { }
    });
  }

}
