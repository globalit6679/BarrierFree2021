package org.tensorflow.lite.examples.detection;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Locale;

public class SearchFragment extends Fragment implements TextToSpeech.OnInitListener{

    ListView listView;
    FavoriteAdapter adapter;
    Database_SQL shop_database;

    Intent intent;
    SpeechRecognizer mRecognizer;

    TextToSpeech tts;

    EditText editText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_search, container, false);

        shop_database = Database_SQL.getInstance(getContext());
        shop_database.open();

        listView = (ListView) rootView.findViewById(R.id.listView2);
        adapter = new FavoriteAdapter(getContext());

        editText = (EditText) rootView.findViewById(R.id.search_edit);
        shop_database.selectData2(listView, adapter);
        listView.setAdapter(adapter);
        Button save_button = rootView.findViewById(R.id.addButton2);

        ImageButton mic_button = rootView.findViewById(R.id.micButton2);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                FavoriteItem item = (FavoriteItem) adapter.getItem(position);
//                Toast.makeText(getContext(), "선택 : " + item.getName(), Toast.LENGTH_LONG).show();

                String kor_name = item.getName(); //선택한 아이템 이름 String 값
                flush_speak(item.getName()+"을/를 탐색합니다.");
                String eng_name = shop_database.HashTable(kor_name);
                Toast.makeText(getContext(), item.getName()+"을/를 탐색합니다.",Toast.LENGTH_SHORT).show();
                //선택하면 값 넘기고 카메라 액티비티로
                Intent intent = new Intent(getContext(),DetectorActivity.class);
                intent.putExtra("name",eng_name);
                intent.putExtra("mode","serch");
                startActivity(intent);
            }
        });

        save_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String kor_name = editText.getText().toString().trim();

//                flush_speak(name+"을/를 입력하셨습니다.");

                isData(kor_name);

                editText.setText("");

            }
        });

        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getPackageName()); //여분의 키
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR"); //언어 설정


        tts = new TextToSpeech(getContext(), this);



        mic_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    mRecognizer = SpeechRecognizer.createSpeechRecognizer(getContext());
                    flush_speak("음성인식을 시작합니다.");
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mRecognizer.setRecognitionListener(listener);
                            mRecognizer.startListening(intent); //듣기 시작
                        }
                    },1200);



                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return rootView;
    }


    class FavoriteAdapter extends BaseAdapter {

        ArrayList<FavoriteItem> items = new ArrayList<FavoriteItem>();
        LayoutInflater layoutInflater;

        public FavoriteAdapter favoriteAdapter;


        Context context;

        public FavoriteAdapter(Context context) {
            this.context = context;
            this.layoutInflater = LayoutInflater.from(this.context);
        }

        public FavoriteAdapter getInstance(Context context){
            favoriteAdapter = new FavoriteAdapter(context);
            return favoriteAdapter;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        public void addItem(FavoriteItem item) {
            items.add(item);
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            FavoriteItemView view = new FavoriteItemView(context);

            FavoriteItem item = items.get(position);
            view.setName(item.getName());

            if(view == null){
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = (FavoriteItemView) inflater.inflate(R.layout.favorite_item, viewGroup, false);
            }

            Button delButton = (Button)view.findViewById(R.id.delButton);
            delButton.setVisibility(View.INVISIBLE);

            return view;
        }

    }

    private void isData(String kor_name){ // 파이어베이스의 데이터 판단
        DatabaseReference firebaseDatabase;
        firebaseDatabase = FirebaseDatabase.getInstance().getReference();

        String eng_name = shop_database.HashTable(kor_name);

        if(eng_name == null){
            Toast.makeText(getContext(), "해당 상품은 지원하지 않는 상품입니다", Toast.LENGTH_SHORT).show();
            flush_speak("해당 상품은 지원하지 않는 상품입니다");
        } else {
            Toast.makeText(getContext(), ""+eng_name, Toast.LENGTH_SHORT).show();

            firebaseDatabase.child("cu").child(eng_name).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (!task.isSuccessful()) {
                        Log.e("firebase", "Error getting data", task.getException());
                    }
                    else {
                        if(String.valueOf(task.getResult().getValue()) == "null"){
                            //                        Log.d("firebase", "데이터가 없습니다.");
                            add_speak(kor_name+"은/는 지원하지 않는 상품입니다.");
                            //                        Toast.makeText(getContext(), "해당 상품은 지원하지 않는 상품입니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            //                        Log.d("firebase", name + String.valueOf(task.getResult().getValue()));
                            Toast.makeText(getContext(), kor_name+"을/를 탐색합니다.", Toast.LENGTH_SHORT).show();
                            add_speak(kor_name+"을/를 탐색합니다.");

                            Intent intent = new Intent(getContext(),DetectorActivity.class);
                            intent.putExtra("name",eng_name);
                            intent.putExtra("mode","serch");
                            startActivity(intent);
                        }
                    }
                }
            });
        }
    }

    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
//            Toast.makeText(getContext(),"음성인식을 시작합니다",Toast.LENGTH_SHORT).show();
            flush_speak("");
        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onError(int error) {
            String message;

            switch (error){
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "오디오 에러가 발생했습니다";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "클라이언트 에러가 발생했습니다";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "마이크을 설정할 수 없습니다";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "네트워크 에러 발생했습니다";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "네트워크 에러가 발생했습니다";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "찾을 수 없습니다";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RECOGNIZER 에러가 발생했습니다";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "서버 에러가 발생했습니다";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "시간 초과했습니다";
                    break;
                default:
                    message = "알 수 없는 에러가 발생했습니다";
                    break;
            }
            flush_speak(message);
//            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            String kor_name = "";
            for(int i = 0; i< matches.size(); i++){
                kor_name += matches.get(i);
            }
            isData(kor_name);
            Log.d(kor_name,"dd");

            editText.setText("");

        }

        @Override
        public void onPartialResults(Bundle partialResults) {

        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            //향후 이벤트 추가
        }
    };

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
    public void onDestroy() {

        if (tts != null){
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}