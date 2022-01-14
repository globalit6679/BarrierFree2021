package org.tensorflow.lite.examples.detection;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.NotNull;

import java.util.ArrayList;

import javax.security.auth.callback.Callback;

public class FireBaseServer {
    Context context;

    public FireBaseServer(Context context){
        this.context = context;
    }

    public static FireBaseServer getInstance(Context context){
        FireBaseServer fireBaseServer = new FireBaseServer(context);
        return fireBaseServer;
    }



    String name;
    //readChip을 ShopInfo에 넣어서 모프 과제 한 거처럼 textView에 표시될 수 있도록!


}
