package org.tensorflow.lite.examples.detection;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Locale;

public class BasketFragment extends Fragment implements TextToSpeech.OnInitListener{
    ListView basketList;
    BasketAdapter adapter;
    Database_SQL shop_database;

    TextToSpeech tts;

    TextView textView;
    ArrayList<ShopInfo> shopList = new ArrayList<ShopInfo>();
    int totalPrice = 0;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_basket, container, false);

        shop_database = Database_SQL.getInstance(getContext());
        shop_database.open();

        tts = new TextToSpeech(getContext(), this);

        basketList = (ListView) rootView.findViewById(R.id.basketList);
        textView = (TextView) rootView.findViewById(R.id.totalPriceText);
        adapter = new BasketAdapter(getContext());
        shop_database.selectData3(adapter);
        basketList.setAdapter(adapter);

        totalPrice = viewBookAll();
        textView.setText("총 금액 : " + totalPrice + "원");

        add_speak("현재 장바구니의 총 가격은 "+totalPrice+"원입니다.");

        //Toast.makeText(getContext(), "현재 장바구니의 총 가격은 "+totalPrice+"원입니다.", Toast.LENGTH_SHORT).show();

        return rootView;
    }

    class BasketAdapter extends BaseAdapter {

        ArrayList<BasketItem> items = new ArrayList<BasketItem>();
        LayoutInflater layoutInflater;

        public BasketAdapter basketAdapter;

        Context context;

        public BasketAdapter(Context context) {
            this.context = context;
            this.layoutInflater = LayoutInflater.from(this.context);
        }

        public BasketAdapter getInstance(Context context){
            basketAdapter = new BasketAdapter(context);
            return basketAdapter;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        public void addItem(BasketItem item) {
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
            BasketItemView view = new BasketItemView(context);

            BasketItem item = items.get(position);
            view.setName(item.getName());
            view.setPrice(item.getPrice());


            if(view == null){
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = (BasketItemView) inflater.inflate(R.layout.basket_item, viewGroup, false);
            }

            Button delButton = (Button)view.findViewById(R.id.delButton2);

            delButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(getContext(), item.getName()+"이/가 삭제되었습니다", Toast.LENGTH_SHORT).show();
                    items.remove(position);
                    flush_speak(item.getName()+"이/가 삭제되었습니다");
                    shop_database.delData2(item.getName());

                    String preTotalPrice = textView.getText().toString();
                    int nowPrice = minusPrice(item.getPrice(), preTotalPrice);
                    textView.setText("총 금액 : " + nowPrice + "원");

                    flush_speak("현재 총 가격은 "+nowPrice+"원입니다.");
                    Toast.makeText(getContext(), "현재 총 가격은 "+nowPrice+"원입니다.", Toast.LENGTH_SHORT).show();

                    adapter.notifyDataSetChanged();
                }
            });
            return view;
        }

    }

    private int viewBookAll(){
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

    private int minusPrice(String price, String prePrice){
        prePrice = prePrice.replaceAll("[^0-9]","");
        int intPrePrice = Integer.parseInt(prePrice);
        price = price.replaceAll("[^0-9]","");

        int intprice = Integer.parseInt(price);
        intPrePrice -= intprice;

        return intPrePrice;
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
