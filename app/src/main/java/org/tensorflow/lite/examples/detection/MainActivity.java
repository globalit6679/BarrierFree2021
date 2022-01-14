package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    PageFragment pageFragment;
    FavoriteFragment favoriteFragment;
    SearchFragment searchFragment;
    BasketFragment basketFragment;
    Database_SQL shopDatabase;
    //    DelFragment delFragment;
    TextToSpeech tts;

    FragmentTransaction ft;

    int index = 9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pageFragment = (PageFragment) getSupportFragmentManager().findFragmentById(R.id.ProjectFragment);

        shopDatabase = Database_SQL.getInstance(getApplicationContext());
        shopDatabase.open();
        shopDatabase.createTable();

        Button find_button = (Button) findViewById(R.id.find_button);
        Button basket_button = (Button) findViewById(R.id.basket_button);
        Button favorite_button = (Button) findViewById(R.id.favorite_button);
        Button setting_button = (Button) findViewById(R.id.setting_button);
        if(Build.VERSION.SDK_INT >= 23){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.INTERNET,
                    Manifest.permission.RECORD_AUDIO},1);
        }

        tts = new TextToSpeech(getApplicationContext(), this);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                flush_speak("상품인식 화면입니다.");
            }
        },1200);

        find_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                index = 0;
                onFragmentChanged(index);
                find_button.setBackgroundColor(Color.parseColor("#FFAC00"));
                basket_button.setBackgroundColor(Color.parseColor("#FFC000"));
                favorite_button.setBackgroundColor(Color.parseColor("#FFC000"));
//                Toast.makeText(getApplicationContext(), "상품인식 화면으로 이동합니다.", Toast.LENGTH_SHORT).show();
                flush_speak("상품인식 화면으로 이동합니다.");
            }
        });

        basket_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                index = 3;
                find_button.setBackgroundColor(Color.parseColor("#FFC000"));
                basket_button.setBackgroundColor(Color.parseColor("#FFAC00"));
                favorite_button.setBackgroundColor(Color.parseColor("#FFC000"));

                onFragmentChanged(index);
//                Toast.makeText(getApplicationContext(), "장바구니 화면으로 이동합니다.", Toast.LENGTH_SHORT).show();
                flush_speak("장바구니 화면으로 이동합니다.");
            }
        });

        favorite_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                index = 1;
                onFragmentChanged(index);
                find_button.setBackgroundColor(Color.parseColor("#FFC000"));
                basket_button.setBackgroundColor(Color.parseColor("#FFC000"));
                favorite_button.setBackgroundColor(Color.parseColor("#FFAC00"));
//                Toast.makeText(getApplicationContext(), "즐겨찾기 화면으로 이동합니다.", Toast.LENGTH_SHORT).show();
                flush_speak("즐겨찾기 화면으로 이동합니다.");
            }
        });

        setting_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                index = 4;
                onFragmentChanged(index);
//                Toast.makeText(getApplicationContext(), "설정 화면으로 이동합니다.", Toast.LENGTH_SHORT).show();
                flush_speak("");

            }
        });

        favoriteFragment = new FavoriteFragment();
        searchFragment = new SearchFragment();
        basketFragment = new BasketFragment();

    }

    public int onFragmentChanged(int index) {
        if (index == 0) {
            getSupportFragmentManager().beginTransaction().replace(R.id.linear, pageFragment).commit();
        } else if (index == 1) {
            getSupportFragmentManager().beginTransaction().replace(R.id.linear, favoriteFragment).commit();
        } else if (index == 2){
            getSupportFragmentManager().beginTransaction().replace(R.id.linear, searchFragment).commit();
        } else if (index == 3){
            getSupportFragmentManager().beginTransaction().replace(R.id.linear, basketFragment).commit();
        } else if (index == 4){
            Intent guideIntent = new Intent(getApplicationContext(), GuideActivity.class);
            startActivity(guideIntent);
        }

        return index;
    }

    private long lastTimeBackPressed;
    @Override
    public void onBackPressed() {
        if(System.currentTimeMillis() - lastTimeBackPressed < 1500){
            shopDatabase.close();
            finish();
            return;
        }
        lastTimeBackPressed = System.currentTimeMillis();
//        Toast.makeText(this,"뒤로 버튼을 한 번 더 누르면 종료됩니다.",Toast.LENGTH_SHORT).show();
        flush_speak("뒤로 버튼을 한 번 더 누르면 앱이 종료됩니다.");

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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (tts != null){
            tts.stop();
            tts.shutdown();
        }
    }
}