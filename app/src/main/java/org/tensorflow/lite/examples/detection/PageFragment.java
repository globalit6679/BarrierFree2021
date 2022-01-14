package org.tensorflow.lite.examples.detection;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import java.util.Locale;

public class PageFragment extends Fragment implements TextToSpeech.OnInitListener {

    Button button,button2;
    TextToSpeech tts;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_page, container, false);
        tts = new TextToSpeech(getContext(), this);

        MainActivity activity = (MainActivity) getActivity();



        button = (Button) rootView.findViewById(R.id.searchMode);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flush_speak("탐색화면입니다.원하시는 상품을 입력해주세요.");
                activity.onFragmentChanged(2);
            }
        });
        button2 = (Button) rootView.findViewById(R.id.lookingMode);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //구경모드 버튼 이벤트
                flush_speak("구경모드로 안내하겠습니다.");
                Intent intent = new Intent(getContext(),DetectorActivity.class);
                intent.putExtra("mode","view");
                startActivity(intent);
            }
        });
        return rootView;
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
