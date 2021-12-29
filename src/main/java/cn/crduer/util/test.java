package cn.crduer.util;

/**
 * @Author: cruder
 * @Date: 2021/12/29/15:02
 */
public class test {
    public static void main(String[] args) throws InterruptedException {
        ExpiryMap<String, String> stringStringExpiryMap = new ExpiryMap<>(10000);
        for (int i = 0; i < 10; i++) {
            stringStringExpiryMap.put(String.valueOf(i), String.valueOf(i));
        }
        System.out.println(stringStringExpiryMap);

        Thread.sleep(1000 * 50);
        System.out.println(stringStringExpiryMap);
    }
}
