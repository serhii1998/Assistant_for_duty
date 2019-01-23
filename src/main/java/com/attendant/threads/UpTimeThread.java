package com.attendant.threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;

import static java.lang.Thread.sleep;

public class UpTimeThread implements Runnable {

    Logger logger = LoggerFactory.getLogger(UpTimeThread.class);

    @Override
    public void run() {
        while (true) {
            System.out.println("in UpTimeThread while");

            StringBuilder stringBuilder = new StringBuilder();

            try (InputStream inputStream = new URL("https://assistant-attendant.herokuapp.com/").openStream();
                 BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {

                logger.info("!!!! UpTimeThread -> run -> stream open");
//                int symbol;
//                while ((symbol = bufferedInputStream.read()) != -1){
//                    stringBuilder.append((char)symbol);
//                }
//

            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                logger.info("!!!! UpTimeThread -> run -> stream sleep");
                sleep(300000);//пробуждение через каждые 5 мин
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
