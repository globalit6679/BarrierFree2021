package org.tensorflow.lite.examples.detection;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.util.Locale;

public class GuideActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{
    private static final int NUM_PAGES = 8;
    private ViewPager2 pager;
    private FragmentStateAdapter pagerAdapter;
    TextToSpeech tts;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        tts = new TextToSpeech(getApplicationContext(), this);

        pager=findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(this);
        pager.setAdapter(pagerAdapter);

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position==0){
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            flush_speak("아이쇼핑을 이용해주셔서 감사합니다. 본 어플은 시각장애인을 위한 어플이기에 보이스오버 기능을 활성화 하시고 이용하시면 더욱 편하게 사용하실수 있습니다. 본 화면은 어플 이용에 대한 가이드를 설명해주는 화면입니다.");
                            add_speak("현재 제공되는 상품 종류는 과자,라면,음료수이며 위치는 CU부곡글로리점, GS25부산가톨릭대점만 제공하고 있으며 그 외의 장소에서 실행할경우 CU홈페이지 기반으로 정보를 제공하고 있습니다.");
                        }
                    },1000);
                }
                else if (position==1){
                    flush_speak("본 어플은 두 가지 모드가 있으며 구경모드는 상품 정보를 제공하는 데에 특화된 모드이고 탐색모드는 원하는 상품을 찾는 데에 특화된 모드입니다.");
                    add_speak("위의 노란색버튼은 구경모드진입 버튼이며 밑의 갈색버튼은 탐색모드진입 버튼입니다.");
                }
                else if (position==2){
                    flush_speak("구경모드에 진입하여 상품을 가까이 비추면 해당상품의 상품명, 가격, 행사정보를 음성으로 안내해줍니다.");
                    add_speak("화면에서 왼쪽 파란색화살표의 버튼을 누르면 다시 상품 들기전 인식화면으로 돌아가며, 상품담기를 누르면 잠바구니에 담기며 장바구니 화면으로 넘어갑니다. 또한 밑의 왼쪽 갈색버튼으로 메인화면으로 돌아갈수 있으며 오른쪽 장바구니버튼은 장바구니 화면을 띄워줍니다.");
                }
                else if (position==3){
                    flush_speak("앞의 화면의 상품담기 버튼을 누르면 해당 상품을 장바구니에 담고 총액을 음성으로 안내합니다.");
                }

                else if (position==4){
                    flush_speak("탐색화면은 탐색모드 진입 전 원하는 상품을 입력할수 있는 화면입니다. 음성인식, 타이핑, 즐겨찾기 기능을 통하여 찾고자하는 상품을 입력합니다.");
                    add_speak("가운데의 마이크 그림을 이용하여 음성인식을 이용할수 있고 그 밑은 타이핑 그 다음 밑은 즐겨찾기 목록이 위치해져있습니다.");
                }
                else if (position==5){
                    flush_speak("탐색모드에 진입하기면 찾고자하는 상품의 대략적 위치를 음성으로 안내해줍니다. 상품정보 역시 상품을 들면 음성으로 안내해줍니다.");
                }
                else if (position==6){
                    flush_speak("즐겨찾기 화면은 탐색모드의 원하는 상품을 쉽게 입력하기위해 고안되었습니다.");
                    add_speak("상단의 타이핑과 바로 오른쪽 갈색버튼의 음성인식으로 즐겨찾기를 등록할수있습니다.");
                }
                else if (position==7){
                    flush_speak("장바구니에 담긴 상품정보는 인식화면 외에서도 확인 할 수 있습니다.");
                    add_speak("앞서 설명한 상품인식, 장바구니, 즐겨찾기 그리고 본 화면의 도움말은 하단의 4개의 버튼으로 접근할수 있습니다.");
                    add_speak("도움말은 여기까지 입니다. 화면을 눌러 도움말을 종료해 주십시오.");
                }
            }
        });


    }

    private class ScreenSlidePagerAdapter extends FragmentStateAdapter{
        public ScreenSlidePagerAdapter(FragmentActivity fa){
            super(fa);
        }


        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if(position==0) {
                return new Info01Fragment();
            }
            else if(position==1) {
                return new Info02Fragment();
            }
            else if(position==2) {
                return new Info03Fragment();
            }
            else if(position==3) {
                return new InfoFragment04();
            }
            else if(position==4) {
                return new InfoFragment05();
            }
            else if(position==5) {
                return new InfoFragment06();
            }
            else if(position==6) {
                return new InfoFragment07();
            }
            else {
                return new InfoFragment08();
            }
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
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
}
