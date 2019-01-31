package com.attendant.threads;

import com.attendant.googleSpreadsheet.SheetsQuickstart;
import com.attendant.model.ReminderEntity;
import com.attendant.telegramBotAssistant.TelegramBotAssistant;
import com.attendant.utils.UtilsDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;


public class ReminderThread extends Thread {

    private Logger logger = LoggerFactory.getLogger(ReminderThread.class);
    private boolean todayDatabaseHasAlreadyBeenUpdated = false;
    private int timeZoneDifference = 2;

    public ReminderThread(String name) {
        super(name);
    }

    @Override
    public void run() {
        while (true) {

            if (!todayDatabaseHasAlreadyBeenUpdated) {
                updateOfDutyDatesInDB(); // обновляем базу данных
                todayDatabaseHasAlreadyBeenUpdated = true;
            }

            long needSleepStreamBeforeSendReminder = canRunSendReminderOrNeedToWait();
            logger.info("!!!!! ReminderThread -> run -> needSleepStreamBeforeSendReminder == {}", needSleepStreamBeforeSendReminder);
            if (needSleepStreamBeforeSendReminder == 0) {
                try {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
                    GregorianCalendar gregorianCalendar = new GregorianCalendar();
                    String todayTomorrowAfterTomorrow = simpleDateFormat.format(gregorianCalendar.getTime()); // изменяю эту дату для получения завтра и послезавтра
                    // следующие 2 переменных для отправки напоминания в будень день в 16:00 часа про то, что дежурство в 17:00
                    int dayOfWeek = gregorianCalendar.get(Calendar.DAY_OF_WEEK);
                    String today = todayTomorrowAfterTomorrow;

                    // нулевой индекс означает, что это текущая дата. 1 означает что это завтра, индекс 2, послезавтра.
                    // Это достигается тем, что вконце цикла я прибавляю 1 день к gregorianCalendar

                    HashMap<Integer, ArrayList<ReminderEntity>> mapReminders = new HashMap<>();
                    for (int i = 0; i < 3; i++) {
                        ArrayList<ReminderEntity> reminders = new ArrayList<>(UtilsDB.getReminderGivenDate(todayTomorrowAfterTomorrow)); // получаем дежурных конкретного дня. это значит, что получаем по очереди дежурных сегодня, завтра и послезавтра
                        mapReminders.put(i, reminders);

                        gregorianCalendar.add(Calendar.DAY_OF_MONTH, 1);
                        todayTomorrowAfterTomorrow = simpleDateFormat.format(gregorianCalendar.getTime()); // прибавляю, что бы получить завтрашний день. делаю это 2 раза, что бы получить завтрашний и после-завтрашний дни, при этом соблюдая формат даты, как в гугл таблице
                    }

                    logger.info("!!!!! ReminderThread ->  run -> mapReminders == {}, {}, {}, {}", mapReminders.toString(), !mapReminders.get(0).isEmpty(), !mapReminders.get(1).isEmpty(), !mapReminders.get(2).isEmpty());
                    if (!mapReminders.get(0).isEmpty() || !mapReminders.get(1).isEmpty() || !mapReminders.get(2).isEmpty()) {
                        TelegramBotAssistant telegramBotAssistant = new TelegramBotAssistant();
                        telegramBotAssistant.sendReminder(mapReminders); // отправить напоминание
                    }


                    if (dayOfWeek > 1 && dayOfWeek < 7 && nowBefore1700()) {
                        long sleepThreadBeforeTodayDuty = sleepThreadBeforeTodayDutyInWeekdays();
                        logger.info("!!!!! ReminderThread ->  run -> sleepThreadBeforeTodayDutyInWeekdays() == {}", sleepThreadBeforeTodayDuty);
                        sleep(sleepThreadBeforeTodayDuty);
                        logger.info("!!!!! ReminderThread ->  run -> sleepThreadBeforeTodayDutyInWeekdays() -> UP");
                        TelegramBotAssistant telegramBotAssistant = new TelegramBotAssistant();
                        telegramBotAssistant.sendReminderIn1600(today);
                    }

                    long sleepThreadUntilNextDay = sleepThreadUntilNextDay();
                    logger.info("!!!!! ReminderThread ->  run -> sleepThreadUntilNextDay() == {}", sleepThreadUntilNextDay);

                    todayDatabaseHasAlreadyBeenUpdated = false; // говорим про то, что база данных не обновлена, что-бы на следующий день она опять обновилась

                    sleep(sleepThreadUntilNextDay);

                } catch (InterruptedException e) {
                    logger.warn("!!!!! ReminderThread ->  run -> InterruptedException");
                    e.printStackTrace();
                }

            } else {
                try {
                    logger.info("!!!!!  ReminderThread -> run -> else needSleepStreamBeforeSendReminder == {}", needSleepStreamBeforeSendReminder);
                    sleep(needSleepStreamBeforeSendReminder);
                } catch (InterruptedException e) {
                    logger.warn("!!!!!  ReminderThread -> run -> InterruptedException");
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean nowBefore1700(){
        GregorianCalendar now = new GregorianCalendar();

        GregorianCalendar todayStartDuty1700 = new GregorianCalendar();
        todayStartDuty1700.set(Calendar.HOUR_OF_DAY, 17-timeZoneDifference);
        todayStartDuty1700.set(Calendar.MINUTE, 0);
        todayStartDuty1700.set(Calendar.SECOND, 0);

        boolean nowBefore1700 = now.before(todayStartDuty1700);
        logger.info("!!!!!  ReminderThread -> run -> nowBefore1700() == {}", nowBefore1700);
        return nowBefore1700;
    }
    private long sleepThreadBeforeTodayDutyInWeekdays() {// візівать при условии, если точноо будень день
        GregorianCalendar now = new GregorianCalendar();

        GregorianCalendar todayStartDuty1600 = new GregorianCalendar();
        todayStartDuty1600.set(Calendar.HOUR_OF_DAY, 16-timeZoneDifference);
        todayStartDuty1600.set(Calendar.MINUTE, 0);
        todayStartDuty1600.set(Calendar.SECOND, 0);

        logger.info("!!!!! ReminderThread ->  sleepThreadBeforeTodayDutyInWeekdays() -> todayStartDuty1600.getTimeInMillis() - now.getTimeInMillis() == {}", todayStartDuty1600.getTimeInMillis() - now.getTimeInMillis());

        if (now.before(todayStartDuty1600)) {
            return todayStartDuty1600.getTimeInMillis() - now.getTimeInMillis();
        }else {
            return 0;
        }
    }

    private synchronized void updateOfDutyDatesInDB() {
        try {
            logger.info("!!!!! ReminderThread -> updateOfDutyDatesInDB()");
            List<List<Object>> valuesInGoogleSpreadsheet = SheetsQuickstart.infoAttendantGoogleSpreadsheet("Database!A3:S");
            UtilsDB.updateDutyDates(valuesInGoogleSpreadsheet);

        } catch (Exception e) {
            logger.warn("!!!!! ReminderThread -> updateOfDutyDatesInDB() -> catch");
            e.printStackTrace();
        }
    }

    private long canRunSendReminderOrNeedToWait() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        GregorianCalendar timeReminder = new GregorianCalendar();
        timeReminder.set(Calendar.HOUR_OF_DAY, 9-timeZoneDifference);
        timeReminder.set(Calendar.MINUTE, 0);
        timeReminder.set(Calendar.SECOND, 0);

        logger.info("!!!!! ReminderThread -> canRunSendReminderOrNeedToWait() -> gregorianCalendar == {} \n timeReminder == {} \n gregorianCalendar.after(timeReminder) == {} \n timeReminder.getTimeInMillis() - gregorianCalendar.getTimeInMillis() == {}", gregorianCalendar.getTime(), timeReminder.getTime(), gregorianCalendar.after(timeReminder), timeReminder.getTimeInMillis() - gregorianCalendar.getTimeInMillis());
        if (gregorianCalendar.after(timeReminder)) {
            return 0;
        } else {
            return timeReminder.getTimeInMillis() - gregorianCalendar.getTimeInMillis();
        }
    }

    private long sleepThreadUntilNextDay() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        GregorianCalendar timeReminder = new GregorianCalendar();
        timeReminder.add(Calendar.DAY_OF_MONTH, 1);//добавляем к сегодняшней дате 1 день
        timeReminder.set(Calendar.HOUR_OF_DAY, 8-timeZoneDifference);
        timeReminder.set(Calendar.MINUTE, 0);
        timeReminder.set(Calendar.SECOND, 0);

        logger.info("!!!!! ReminderThread -> sleepThreadUntilNextDay() -> timeReminder.getTimeInMillis() - gregorianCalendar.getTimeInMillis() == {}", timeReminder.getTimeInMillis() - gregorianCalendar.getTimeInMillis());
        return timeReminder.getTimeInMillis() - gregorianCalendar.getTimeInMillis();
    }
}
