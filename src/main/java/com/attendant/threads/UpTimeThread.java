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
        GregorianCalendar doNotUpTimeAfter = new GregorianCalendar();
        doNotUpTimeAfter.set(Calendar.HOUR_OF_DAY, 23);
        doNotUpTimeAfter.set(Calendar.MINUTE, 35);
        doNotUpTimeAfter.set(Calendar.SECOND, 0);

        GregorianCalendar startUpTimeBefore = new GregorianCalendar();
        startUpTimeBefore.add(Calendar.DAY_OF_MONTH, 1);
        startUpTimeBefore.set(Calendar.HOUR_OF_DAY, 8);
        startUpTimeBefore.set(Calendar.MINUTE, 0);
        startUpTimeBefore.set(Calendar.SECOND, 0);

        while (true) {
            GregorianCalendar now = new GregorianCalendar();

            if (now.after(doNotUpTimeAfter)) {
                try {
                    sleep(startUpTimeBefore.getTimeInMillis() - now.getTimeInMillis());
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
