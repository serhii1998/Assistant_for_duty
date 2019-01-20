package com.attendant.utils;

import com.attendant.model.ReminderEntity;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UtilsDB {

    private static final String USERNAME = "npuzgpmqumbnlt";
    private static final String PASSWORD = "07cf879928c6163797018e61397b2ecdbdfe2b6731ad51ac64d613b039c74d13";
    private static final String URL = "jdbc:postgresql://ec2-79-125-4-96.eu-west-1.compute.amazonaws.com:5432/de2b5g3itjb4ku";

    public static Connection dataConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        Connection connection = DriverManager.getConnection(
                URL, USERNAME, PASSWORD);
        return connection;
    }

    public static void saveReminderInDB(String chatId, String room, String dateDuty) {
        /*
         * давай поразсуждаем. вот я уже определил, что эта комната существует в гугл таблице. все ок.
         * но пользователь хочет установить напоинание. Для этого:
         * 1. он должен ввести свою комнату. +
         * 2. я эту комнату запоминаю, запоминаю chatid, с которого пришел запрос на напоминание. +
         * 3. нахожу дату дежурства, когда надо ему напомнить. +
         * 4. запоминаю эту дату и записываю ее в базу к тому чату, от которого поступил запрос +
         * 5. потом у меня будет поток, который будет запускаться 1 раз в сутки, сканировать базу данных
         * 6. он будет искать даты в ДБ, сравнивать с текущей.
         * 7. если текущая дата на 2 меньше, чем в бд, то нужно отослать напоминание тому чату,
         * с которого поступил запрос на напоминание
         * 8. собственно вроде бы все. */

        try (Connection connection = dataConnection()) {
            PreparedStatement prepareStatement = connection.prepareStatement("select chat_id from reminder_for_duty where chat_id = ?");
            prepareStatement.setString(1, chatId);
            ResultSet resultSetChatId = prepareStatement.executeQuery();

            if (resultSetChatId.next()) {

                prepareStatement = connection.prepareStatement("update reminder_for_duty set number_room = ?, date_duty = ? ,send_confirmation_cur_day = false, send_confirmation_one_day = false, send_confirmation_two_day = false where chat_id = ?");
                prepareStatement.setString(1, room);
                prepareStatement.setString(2, dateDuty);
                prepareStatement.setString(3, resultSetChatId.getString("chat_id"));
                prepareStatement.executeUpdate();

            } else {

                PreparedStatement preparedStatement = connection.prepareStatement("insert into reminder_for_duty (chat_id, number_room, date_duty, send_confirmation_two_day, send_confirmation_one_day, send_confirmation_cur_day) values (?,?,?,false,false,false)");
                preparedStatement.setString(1, chatId);
                preparedStatement.setString(2, room);
                preparedStatement.setString(3, dateDuty.trim());
                System.out.println(dateDuty.length() + "///////////////////////////////////////////////" + dateDuty.toString());

                preparedStatement.execute();
            }
            resultSetChatId.close();


        } catch (Exception e) {
            System.out.println("catch saveReminderInDB incorrect sql query");
            e.printStackTrace();
        }
    }

    // метод, который получает из базы созданное напоминание по переданной дате
    public static List<ReminderEntity> getReminderGivenDate(String dateDuty) {
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

        } catch (Exception e) {
            System.out.println("exception getReminderGivenDate catch");
            e.printStackTrace();
        }

        return reminders;

    }

}
