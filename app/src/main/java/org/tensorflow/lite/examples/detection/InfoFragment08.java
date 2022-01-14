package org.tensorflow.lite.examples.detection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.fragment.app.Fragment;

public class InfoFragment08 extends Fragment {

    public InfoFragment08(){

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_info08, container, false);

        //이미지 버튼 아이디 수정
        Button button = (Button) rootView.findViewById(R.id.info_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //다음 번 앱 실행 때는 실행되지 않도록 true로 변경
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                editor.putBoolean("isFirst",true);
                editor.apply();

                Intent intent = new Intent(getContext(), MainActivity.class);
                startActivity(intent);

            }
        });

        return rootView;
    }
}