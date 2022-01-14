package org.tensorflow.lite.examples.detection;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

public class BasketAdapter extends BaseAdapter {

    Database_SQL shop_database;


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

        shop_database = Database_SQL.getInstance(context.getApplicationContext());


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
                Toast.makeText(context.getApplicationContext(), item.getName()+"이/가 삭제되었습니다", Toast.LENGTH_SHORT).show();
                items.remove(position);
//                flush_speak(item.getName()+"이/가 삭제되었습니다");
                shop_database.delData2(item.getName());
//                String preTotalPrice = textView.getText().toString();
//                String nowPrice = minusPrice(item.getPrice(), preTotalPrice);
//                textView.setText(nowPrice);
//                nowPrice = nowPrice.replaceAll("[^0-9]","");

//                    Log.e("PRICE",item.getPrice());

//                flush_speak("현재 총 가격은 "+nowPrice+"원입니다.");
//                Toast.makeText(context.getApplicationContext(), "현재 총 가격은 "+nowPrice+"원입니다.", Toast.LENGTH_SHORT).show();

                basketAdapter.notifyDataSetChanged();
            }
        });
        return view;
    }

}
