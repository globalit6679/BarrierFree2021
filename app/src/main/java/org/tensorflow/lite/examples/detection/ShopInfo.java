package org.tensorflow.lite.examples.detection;

public class ShopInfo {

    String name;
    String price;
    public ShopInfo(String name, String price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    //@Override
    public String toString(int i) {
        return i+1 + "." +
                "상품명 : " + name + '\n' +
                "가격 : " + price + '\n';
    }
}