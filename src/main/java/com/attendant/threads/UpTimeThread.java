package com.attendant.threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static java.lang.Thread.sleep;

public class UpTimeThread implements Runnable {

    private Logger logger = LoggerFactory.getLogger(UpTimeThread.class);

    @Override
    public void run() {

        while (true) {
            GregorianCalendar doNotUpTimeAfter = new GregorianCalendar();
            doNotUpTimeAfter.set(Calendar.HOUR_OF_DAY, 23);
            doNotUpTimeAfter.set(Calendar.MINUTE, 35);
            doNotUpTimeAfter.set(Calendar.SECOND, 0);

            GregorianCalendar startUpTimeAfter = new GregorianCalendar();
            startUpTimeAfter.add(Calendar.DAY_OF_MONTH, 1);
            startUpTimeAfter.set(Calendar.HOUR_OF_DAY, 8);
            startUpTimeAfter.set(Calendar.MINUTE, 0);
            startUpTimeAfter.set(Calendar.SECOND, 0);

            GregorianCalendar now = new GregorianCalendar();

            if (now.after(doNotUpTimeAfter)) {
                try {
                    long needSleep = startUpTimeAfter.getTimeInMillis() - now.getTimeInMillis();
                    logger.info("!!!! UpTimeThread -> run -> sleep() == {}", needSleep);
                    sleep(needSleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            logger.info("!!!! UpTimeThread -> run -> stream open");
            try (InputStream inputStream = new URL("https://assistant-for-duty.herokuapp.com/").openStream()) {

                logger.info("!!!! UpTimeThread -> run -> stream sleep");
                System.out.println(inputStream.read());
                inputStream.close();

                sleep(300000);//пробуждение через каждые 5 мин
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
