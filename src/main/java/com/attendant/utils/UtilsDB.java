package com.attendant.utils;

import com.attendant.model.ReminderEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class UtilsDB {

    private static Logger logger = LoggerFactory.getLogger(UtilsDB.class);

    private static final String USERNAME = "npuzgpmqumbnlt";
    private static final String PASSWORD = "07cf879928c6163797018e61397b2ecdbdfe2b6731ad51ac64d613b039c74d13";
    private static final String URL = "jdbc:postgresql://ec2-79-125-4-96.eu-west-1.compute.amazonaws.com:5432/de2b5g3itjb4ku";

    public static Connection dataConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        Connection connection = DriverManager.getConnection(
                URL, USERNAME, PASSWORD);
        return connection;
    }

    // человек делает напоминание в день днжурства. нужно все выставить в true
    // человек делает напоминание за день до дежурства. завтра и послезавтра делаю в true, а сегодня в false
    // человек делает напоминание за 2 дня до дежурства. послезавтра делаю в true, а сегодня и завтра в false
    // человек делает напоминание больше чем за 2 дня до дежурства. делаю все переменные в false
    //dateDuty; дата дежурства
    //если дата дежурства равна сегодняшней, то установить все даты в true
    //если дата дежурства равна завтрашней, установить сегодня в false, осталоное в true
    //если дата дежурства равна послезавтрашней, установить сегодня и завтра в false, после-завтра в true
    //иначе все в false

    public synchronized static void saveReminderInDB(String chatId, String room, String strDateDuty) {
        logger.info("////// UtilsDB -> saveReminderInDB -> chatId == {}, room == {}, strDateDuty == {}, strDateDuty.equals(\"\") == {}", chatId, room, strDateDuty, strDateDuty.equals(""));
        try (Connection connection = dataConnection()) {

            boolean sendConfirmationCurDay = false;
            boolean sendConfirmationOneDay = false;
            boolean sendConfirmationTwoDay = false;

            if (!strDateDuty.equals("")) {
                logger.info("////// UtilsDB -> saveReminderInDB -> !strDateDuty.equals(\"\") == {}", !strDateDuty.equals(""));
                SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
                Date dateDuty = format.parse(strDateDuty);

                GregorianCalendar GCCurDay = new GregorianCalendar();
                Date today = format.parse(format.format(GCCurDay.getTime()));

                GregorianCalendar GCOneDay = new GregorianCalendar();
                GCOneDay.add(Calendar.DAY_OF_MONTH, 1);// завтра
                Date tomorrow = format.parse(format.format(GCOneDay.getTime()));

                GregorianCalendar GCTwoDay = new GregorianCalendar();
                GCTwoDay.add(Calendar.DAY_OF_MONTH, 2); // послезавтра
                Date afterTomorrow = format.parse(format.format(GCTwoDay.getTime()));

                logger.warn("///// dateDuty.equals(today) == {}, dateDuty == {}, today == {}", dateDuty.equals(today), dateDuty, today);
                logger.warn("///// dateDuty.equals(tomorrow) == {}, dateDuty == {}, tomorrow == {}", dateDuty.equals(tomorrow), dateDuty, tomorrow);
                logger.warn("///// dateDuty.equals(afterTomorrow) == {}, dateDuty == {}, afterTomorrow == {}", dateDuty.equals(afterTomorrow), dateDuty, afterTomorrow);
                if (dateDuty.equals(today)) {
                    sendConfirmationCurDay = true;
                    sendConfirmationOneDay = true;
                    sendConfirmationTwoDay = true;
                } else if (dateDuty.equals(tomorrow)) {
                    sendConfirmationOneDay = true;
                    sendConfirmationTwoDay = true;
                } else if (dateDuty.equals(afterTomorrow)) {
                    sendConfirmationTwoDay = true;
                }
            }
            logger.info("////// UtilsDB -> saveReminderInDB -> curDay == {}, oneDay == {}, twoDay == {} ", sendConfirmationCurDay, sendConfirmationOneDay, sendConfirmationTwoDay);
            PreparedStatement preparedStatement = connection.prepareStatement("select chat_id from reminder_for_duty where chat_id = ?");
            preparedStatement.setString(1, chatId);
            ResultSet resultSetChatId = preparedStatement.executeQuery();

            if (resultSetChatId.next()) {

                preparedStatement = connection.prepareStatement("update reminder_for_duty set number_room = ?, date_duty = ? ,send_confirmation_cur_day = ?, send_confirmation_one_day = ?, send_confirmation_two_day = ? where chat_id = ?");
                preparedStatement.setString(1, room);
                preparedStatement.setString(2, strDateDuty);
                preparedStatement.setBoolean(3, sendConfirmationCurDay);
                preparedStatement.setBoolean(4, sendConfirmationOneDay);
                preparedStatement.setBoolean(5, sendConfirmationTwoDay);
                preparedStatement.setString(6, resultSetChatId.getString("chat_id"));
                preparedStatement.executeUpdate();

            } else {

                preparedStatement = connection.prepareStatement("insert into reminder_for_duty (chat_id, number_room, date_duty, send_confirmation_cur_day, send_confirmation_one_day, send_confirmation_two_day) values (?,?,?,?,?,?)");
                preparedStatement.setString(1, chatId);
                preparedStatement.setString(2, room);
                preparedStatement.setString(3, strDateDuty);
                preparedStatement.setBoolean(4, sendConfirmationCurDay);
                preparedStatement.setBoolean(5, sendConfirmationOneDay);
                preparedStatement.setBoolean(6, sendConfirmationTwoDay);
                preparedStatement.execute();
            }
            resultSetChatId.close();


        } catch (Exception e) {
            logger.warn("////// UtilsDB -> saveReminderInDB -> CATCH");
            e.printStackTrace();
        }

    }

    // метод, который получает из базы созданное напоминание по переданной дате
    public synchronized static List<ReminderEntity> getReminderGivenDate(String dateDuty) {
        ArrayList<ReminderEntity> reminders = new ArrayList<>();

        try (Connection connection = dataConnection()) {

            PreparedStatement preparedStatement = connection.prepareStatement("select * from reminder_for_duty where date_duty = ?");
            preparedStatement.setString(1, dateDuty);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {// создаем список всех напоминаний и передаем во вне

                ReminderEntity reminderEntity = new ReminderEntity();
                reminderEntity.setChatId(resultSet.getString("chat_id"));
                reminderEntity.setNumberRoom(resultSet.getString("number_room"));
                reminderEntity.setDateDuty(resultSet.getString("date_duty"));
                reminderEntity.setSendConfirmationCurDay(resultSet.getBoolean("send_confirmation_cur_day"));
                reminderEntity.setSendConfirmationCurDay(resultSet.getBoolean("send_confirmation_one_day"));
                reminderEntity.setSendConfirmationCurDay(resultSet.getBoolean("send_confirmation_two_day"));

                reminders.add(reminderEntity);
            }
            logger.info("////// UtilsDB ->  getReminderGivenDate ->  reminders == {}", reminders.toString());
        } catch (Exception e) {
            logger.warn("////// UtilsDB -> getReminderGivenDate -> CATCH");
            e.printStackTrace();
        }

        return reminders;

    }

    public synchronized static void setStatusSendingReminder(HashMap<Integer, ArrayList<ReminderEntity>> mapReminders) {
        logger.info("///// UtilsDB -> setStatusSendingReminder -> mapReminders == {}", mapReminders.toString());

        try (Connection connection = dataConnection()) {
            PreparedStatement preparedStatement = null;
            for (Map.Entry<Integer, ArrayList<ReminderEntity>> entry : mapReminders.entrySet()) {
                int dayDuty = entry.getKey(); // 0 - сегодня, 1 - завтра, 2 - послезавтра
                for (ReminderEntity r : entry.getValue()) {
                    switch (dayDuty) {
                        case 0:
                            preparedStatement = connection.prepareStatement("update reminder_for_duty set send_confirmation_cur_day = true where chat_id = ?");
                            preparedStatement.setString(1, r.getChatId());
                            preparedStatement.executeUpdate();
                            break;
                        case 1:
                            preparedStatement = connection.prepareStatement("update reminder_for_duty set send_confirmation_one_day = true where chat_id = ?");
                            preparedStatement.setString(1, r.getChatId());
                            preparedStatement.executeUpdate();
                            break;
                        case 2:
                            preparedStatement = connection.prepareStatement("update reminder_for_duty set send_confirmation_two_day = true where chat_id = ?");
                            preparedStatement.setString(1, r.getChatId());
                            preparedStatement.executeUpdate();
                            break;
                    }
                }
            }

        } catch (Exception e) {
            logger.warn("///// UtilsDB -> setStatusSendingReminder -> Exception ");
            e.printStackTrace();
        }
    }

    public synchronized static String getRoomRemainderByChatId(String chatId) {
        String room = "";
        try (Connection connection = dataConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("select number_room from reminder_for_duty where chat_id = ?");
            preparedStatement.setString(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                room = resultSet.getString("number_room");
            }

            logger.info("///// UtilsDB -> getRoomRemainderByChatId -> room == {}", room);
            return room;
        } catch (Exception e) {
            logger.warn("///// UtilsDB -> getRoomRemainderByChatId -> Exception ");
            e.printStackTrace();
            return "";
        }
    }

}
