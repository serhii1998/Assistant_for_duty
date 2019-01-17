package com.attendant.threads;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;

import static java.lang.Thread.sleep;

public class UpTimeThread implements Runnable {

    @Override
    public void run() {
//        while(true){
//            System.out.println("in UpTimeThread while");
//
//            StringBuilder stringBuilder = new StringBuilder();
//
//            try(InputStream inputStream = new URL("https://assistant-attendant.herokuapp.com/").openStream();
//                    BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)){
//
//                int symbol;
//                while ((symbol = bufferedInputStream.read()) != -1){
//                    stringBuilder.append((char)symbol);
//                }
//
//                System.out.println(stringBuilder.toString());
//
//                System.out.println("in UpTimeThread while sleep 300000");
//
//                sleep(300000);//пробуждение через каждые 5 мин
//
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }
    }
}
