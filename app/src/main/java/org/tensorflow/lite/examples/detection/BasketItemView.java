package org.tensorflow.lite.examples.detection;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BasketItemView extends LinearLayout {
    TextView textView;
    TextView textView2;
    ImageView imageView;

    public BasketItemView(Context context) {
        super(context);
        init(context);
    }

    public BasketItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.basket_item, this, true);

        textView = (TextView) findViewById(R.id.nameText);
        textView2 = (TextView) findViewById(R.id.priceText);
    }

    public void setName(String name) {
        textView.setText(name);
    }

    public void setPrice(String price){
        textView2.setText(price);
    }
}

