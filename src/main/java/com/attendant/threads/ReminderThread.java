package com.attendant.threads;

import com.attendant.model.ReminderEntity;
import com.attendant.telegramBotAssistant.TelegramBotAssistant;
import com.attendant.utils.UtilsDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReminderThread extends Thread {

    private Logger logger = LoggerFactory.getLogger(ReminderThread.class);

    public ReminderThread(String name) {
        super(name);
    }

    @Override
    public void run() {
        while (true) {
            final int DAY_IN_MILLISECONDS = 86400000;
            long needSleepStream = canRunThreadOrNeedToWait();
            logger.info("!!!!! ReminderThread -> run -> needSleepStream == {}", needSleepStream);
            if (needSleepStream == 0) {
                try {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
                    Date date = simpleDateFormat.parse(simpleDateFormat.format(new Date()));

                    String curDate = simpleDateFormat.format(date);

                    int i = 0; // нулевой индекс означает, что это текущая дата. 1 означает что это завтра, индекс 2, послезавтра.
                    // Это достигается тем, что вконце цикла я прибавляю 1 сутки в милесекундах

                    HashMap<Integer, ArrayList<ReminderEntity>> mapReminders = new HashMap<>();
                    do {
                        ArrayList<ReminderEntity> reminders = new ArrayList<>(UtilsDB.getReminderGivenDate(curDate)); // получаем дежурных конкретного дня. это значит, что получаем по очереди дежурных сегодня, завтра и послезавтра
                        mapReminders.put(i, reminders);

                        i++;
                        curDate = simpleDateFormat.format(new Date(date.getTime() + DAY_IN_MILLISECONDS)); // прибавляю, что бы получить завтрашний день. делаю это 2 раза, что бы получить завтрашний и после-завтрашний дни.
                    } while (i < 3);

                    logger.info("!!!!! ReminderThread ->  run -> mapReminders == {}", mapReminders.toString());
                    TelegramBotAssistant telegramBotAssistant = new TelegramBotAssistant();
                    telegramBotAssistant.sendReminder(mapReminders); // отправить напоминание

                    long sleepThread = sleepThreadUntilNextDay();
                    logger.info("!!!!! ReminderThread ->  run -> sleepThread == {}", sleepThread);
                    sleep(sleepThread);

                } catch (ParseException e) {
                    logger.warn("!!!!! ReminderThread ->  run -> ParseException");
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    logger.warn("!!!!! ReminderThread ->  run -> InterruptedException");
                    e.printStackTrace();
                }

            } else {
                try {
                    logger.info("!!!!!  ReminderThread -> run -> else needSleepStream == {}", needSleepStream);
                    sleep(needSleepStream);
                } catch (InterruptedException e) {
                    logger.warn("!!!!!  ReminderThread -> run -> InterruptedException");
                    e.printStackTrace();
                }
            }
        }
    }

    private long canRunThreadOrNeedToWait() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        GregorianCalendar timeReminder = new GregorianCalendar();
        timeReminder.set(Calendar.YEAR, timeReminder.get(Calendar.YEAR));
        timeReminder.set(Calendar.MONTH, timeReminder.get(Calendar.MONTH));
        timeReminder.set(Calendar.DAY_OF_MONTH, timeReminder.get(Calendar.DAY_OF_MONTH));
        timeReminder.set(Calendar.HOUR, 9);
        timeReminder.set(Calendar.MINUTE, 0);
        timeReminder.set(Calendar.SECOND, 0);

        logger.info("canRunThreadOrNeedToWait() -> gregorianCalendar == {} \n timeReminder == {} \n gregorianCalendar.after(timeReminder) == {} \n timeReminder.getTimeInMillis() - gregorianCalendar.getTimeInMillis() == {}", gregorianCalendar.getTime(), timeReminder.getTime(), gregorianCalendar.after(timeReminder), timeReminder.getTimeInMillis() - gregorianCalendar.getTimeInMillis());
        if (gregorianCalendar.after(timeReminder)) {
            return 0;
        } else {
            return timeReminder.getTimeInMillis() - gregorianCalendar.getTimeInMillis();
        }
    }

    private long sleepThreadUntilNextDay() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        GregorianCalendar timeReminder = new GregorianCalendar();
        timeReminder.set(Calendar.YEAR, timeReminder.get(Calendar.YEAR));
        timeReminder.set(Calendar.MONTH, timeReminder.get(Calendar.MONTH));
        timeReminder.add(Calendar.DAY_OF_MONTH, 1);//добавляем к сегодняшней дате 1 день
        timeReminder.set(Calendar.HOUR, 9);
        timeReminder.set(Calendar.MINUTE, 0);
        timeReminder.set(Calendar.SECOND, 0);

        logger.info("sleepThreadUntilNextDay() -> timeReminder.getTimeInMillis() - gregorianCalendar.getTimeInMillis() == {}", timeReminder.getTimeInMillis() - gregorianCalendar.getTimeInMillis());
        return timeReminder.getTimeInMillis() - gregorianCalendar.getTimeInMillis();
    }
}
