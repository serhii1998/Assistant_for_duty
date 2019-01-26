package com.attendant.utils;

import com.attendant.model.ReminderEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

import static com.attendant.utils.CommonUtils.todayBeforeOrEqualsThisDate;

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

    //поринимает в себя список из 3 булевих переменных, проверяет прошла ли дата дежурства и сетит соответствующие флаги
    private synchronized static List<Boolean> checkSendConfirmation(List<Boolean> confirmations, String strDateDuty) {
        try {
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

//                logger.warn("///// dateDuty.equals(today) == {}, dateDuty == {}, today == {}", dateDuty.equals(today), dateDuty, today);
//                logger.warn("///// dateDuty.equals(tomorrow) == {}, dateDuty == {}, tomorrow == {}", dateDuty.equals(tomorrow), dateDuty, tomorrow);
//                logger.warn("///// dateDuty.equals(afterTomorrow) == {}, dateDuty == {}, afterTomorrow == {}", dateDuty.equals(afterTomorrow), dateDuty, afterTomorrow);
            if (dateDuty.equals(today)) {
                confirmations.add(0, true);
                confirmations.add(1, true);
                confirmations.add(2, true);
            } else if (dateDuty.equals(tomorrow)) {
                confirmations.add(0, false);
                confirmations.add(1, true);
                confirmations.add(2, true);
            } else if (dateDuty.equals(afterTomorrow)) {
                confirmations.add(0, false);
                confirmations.add(1, false);
                confirmations.add(2, true);
            } else {
                confirmations.add(0, false);
                confirmations.add(1, false);
                confirmations.add(2, false);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        logger.info("////// UtilsDB -> saveReminderInDB -> checkSendConfirmation -> confirmations == {}", confirmations);
        return confirmations;
    }


    //сохранить напоминание
    public synchronized static void saveReminderInDB(String chatId, String room, String strDateDuty) {
        logger.info("////// UtilsDB -> saveReminderInDB -> chatId == {}, room == {}, strDateDuty == {}, strDateDuty.equals(\"\") == {}", chatId, room, strDateDuty, strDateDuty.equals(""));
        try (Connection connection = dataConnection()) {

//            boolean sendConfirmationToday = false;
//            boolean sendConfirmationTomorrow = false;
//            boolean sendConfirmationAfterTomorrow = false;

            ArrayList<Boolean> confirmations = new ArrayList<>();
            confirmations.add(0, false); // сегодня
            confirmations.add(1, false);//завтра
            confirmations.add(2, false);//послезавтра

            if (!strDateDuty.equals("")) {
                confirmations = new ArrayList<>(checkSendConfirmation(confirmations, strDateDuty));
            }

            logger.info("////// UtilsDB -> saveReminderInDB -> curDay == {}, oneDay == {}, twoDay == {} ", confirmations.get(0), confirmations.get(1), confirmations.get(2));
            PreparedStatement preparedStatement = connection.prepareStatement("select chat_id from reminder_for_duty where chat_id = ?");
            preparedStatement.setString(1, chatId);
            ResultSet resultSetChatId = preparedStatement.executeQuery();

            if (resultSetChatId.next()) {

                preparedStatement = connection.prepareStatement("update reminder_for_duty set number_room = ?, date_duty = ? ,send_confirmation_today = ?, send_confirmation_tomorrow = ?, send_confirmation_after_tomorrow = ? where chat_id = ?");
                preparedStatement.setString(1, room);
                preparedStatement.setString(2, strDateDuty);
                preparedStatement.setBoolean(3, confirmations.get(0));
                preparedStatement.setBoolean(4, confirmations.get(1));
                preparedStatement.setBoolean(5, confirmations.get(2));
                preparedStatement.setString(6, resultSetChatId.getString("chat_id"));
                preparedStatement.executeUpdate();

            } else {

                preparedStatement = connection.prepareStatement("insert into reminder_for_duty (chat_id, number_room, date_duty, send_confirmation_today, send_confirmation_tomorrow, send_confirmation_after_tomorrow) values (?,?,?,?,?,?)");
                preparedStatement.setString(1, chatId);
                preparedStatement.setString(2, room);
                preparedStatement.setString(3, strDateDuty);
                preparedStatement.setBoolean(4, confirmations.get(0));
                preparedStatement.setBoolean(5, confirmations.get(1));
                preparedStatement.setBoolean(6, confirmations.get(2));
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
                reminderEntity.setSendConfirmationToday(resultSet.getBoolean("send_confirmation_today"));
                reminderEntity.setSendConfirmationTomorrow(resultSet.getBoolean("send_confirmation_tomorrow"));
                reminderEntity.setSendConfirmationAfterTomorrow(resultSet.getBoolean("send_confirmation_after_tomorrow"));

                reminders.add(reminderEntity);
            }
            logger.info("////// UtilsDB ->  getReminderGivenDate ->  reminders == {}", reminders.toString());
        } catch (Exception e) {
            logger.warn("////// UtilsDB -> getReminderGivenDate -> CATCH");
            e.printStackTrace();
        }

        return reminders;

    }

    // устанавливаем в БД статус отправки уведомления
    public synchronized static void setStatusSendingReminder(HashMap<Integer, ArrayList<ReminderEntity>> mapReminders) {
        logger.info("///// UtilsDB -> setStatusSendingReminder -> mapReminders == {}", mapReminders.toString());

        try (Connection connection = dataConnection()) {
            PreparedStatement preparedStatement = null;
            for (Map.Entry<Integer, ArrayList<ReminderEntity>> entry : mapReminders.entrySet()) {
                int dayDuty = entry.getKey(); // 0 - сегодня, 1 - завтра, 2 - послезавтра
                for (ReminderEntity r : entry.getValue()) {
                    switch (dayDuty) {
                        case 0:
                            preparedStatement = connection.prepareStatement("update reminder_for_duty set send_confirmation_today = true,  send_confirmation_tomorrow = true,  send_confirmation_after_tomorrow = true where chat_id = ?");
                            preparedStatement.setString(1, r.getChatId());
                            preparedStatement.executeUpdate();
                            break;
                        case 1:
                            preparedStatement = connection.prepareStatement("update reminder_for_duty set send_confirmation_today = false, send_confirmation_tomorrow = true, send_confirmation_after_tomorrow = true where chat_id = ?");
                            preparedStatement.setString(1, r.getChatId());
                            preparedStatement.executeUpdate();
                            break;
                        case 2:
                            preparedStatement = connection.prepareStatement("update reminder_for_duty set send_confirmation_today = false, send_confirmation_tomorrow = false, send_confirmation_after_tomorrow = true where chat_id = ?");
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

    //получаем комнату для напоминания по чат Id
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

    //удалить напоминание
    public synchronized static void deleteReminderFromChatId(String chatId) {
        try (Connection connection = dataConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("delete from reminder_for_duty where chat_id = ?");
            preparedStatement.setString(1, chatId);
            preparedStatement.execute();
        } catch (Exception e) {
            logger.warn("/////UtilsDB -> deleteReminderFromChatId CATCH");
        }
    }

    // этот метод сначала чистит базу данных на лицо просроченых дат а потом возвращает напоминания без дат
//    public synchronized static List<ReminderEntity> getReminderEntityWhoDidNotHaveDateDuty() {
//        List<ReminderEntity> entityList = new ArrayList<>();
//        try (Connection connection = dataConnection()) {
//
//            checkOfOverdueDatesDuty(connection);// модифицируем базу данных, что бы понять, у кого уже просрочено дежурство
//
//            PreparedStatement preparedStatement = connection.prepareStatement("select chat_id, number_room from reminder_for_duty where date_duty = ''");
//            ResultSet resultSet = preparedStatement.executeQuery();
//            while (resultSet.next()) {
//                ReminderEntity reminderEntity = new ReminderEntity();
//                reminderEntity.setChatId(resultSet.getString("chat_id"));
//                reminderEntity.setNumberRoom(resultSet.getString("number_room"));
//                entityList.add(reminderEntity);
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            e.printStackTrace();
//        }
//        logger.info("/////UtilsDB -> getReminderEntityWhoDidNotHaveDateDuty() -> entityList == {}", entityList.toString());
//        return entityList;
//    }

//    // проверяем просроченые дежурства
//    private static void checkOfOverdueDatesDuty(Connection connection) {
//        logger.info("/////UtilsDB -> checkOfOverdueDatesDuty");
//        try {
//            PreparedStatement preparedStatement = connection.prepareStatement("select chat_id, date_duty from reminder_for_duty");
//            ResultSet resultSet = preparedStatement.executeQuery();
//            while (resultSet.next()) {
//                String chatId = resultSet.getString("chat_id");
//                String dateDuty = resultSet.getString("date_duty");
//
//                if (!dateDuty.equals("") && !UtilsSpreadsheet.todayBeforeOrEqualsThisDate(dateDuty.trim())) {
//                    preparedStatement = connection.prepareStatement("update reminder_for_duty set date_duty = '' where chat_id = ?");
//                    preparedStatement.setString(1, chatId);
//                    preparedStatement.executeUpdate();
//                }
//            }
//
//        } catch (SQLException | ParseException e) {
//            logger.warn("/////UtilsDB -> checkOfOverdueDatesDuty -> CATCH");
//            e.printStackTrace();
//        }
//    }

    public static void updateDutyDates(List<List<Object>> valuesInGoogleSpreadsheet) {
        //key == room, values == dateDuty;
        HashMap<String, String> supportMap = new HashMap<>();// коллекция, в которую записываю комнаты и даты,
        // чьи даты в БД уже были обновлены. это нужно для того, что бы не перезатереть старые даты дежурств новыми, которые написаны, на месяц на 2 вперед

        logger.info("/////UtilsDB -> updateDutyDates");
        try (Connection connection = dataConnection()) {
            for (List row : valuesInGoogleSpreadsheet) {
                for (int i = 1; i < row.size(); i += 2) {


                    String dateDuty = row.get(i + 1).toString().trim();
                    String room = row.get(i).toString().trim();
                    if (!room.equals("") && !dateDuty.equals("") && todayBeforeOrEqualsThisDate(dateDuty) && supportMap.get(room) == null) {
                        //зайти сюда только в том случае, если ячейка в гугл таблице не пустая, если дата дежурства в гугл таблице находится после сегодняшнего дня (если дата дежурства еще не прошла)
                        // и если для этой комнаты еще небыло обновлений в дате

                        PreparedStatement preparedStatement = connection.prepareStatement("select date_duty from reminder_for_duty where number_room = ?");
                        preparedStatement.setString(1, room);
                        ResultSet resultSet = preparedStatement.executeQuery();
                        String dateDutyFromDB = resultSet.getString("date_duty");
                        //дальше проверяем, устарела ли дата дежурства в гугл таблице.
                        if (!dateDuty.equals(dateDutyFromDB)){
                            logger.info("/////UtilsDB -> updateDutyDates -> room = {}, dateDuty = {}, dateDutyFromDB", room, dateDuty, dateDutyFromDB);
                            //если устарела, то обновим дату дежурства
                            preparedStatement = connection.prepareStatement("update reminder_for_duty set date_duty = ? where number_room = ?");
                            preparedStatement.setString(1, dateDuty);
                            preparedStatement.setString(2, room);
                            preparedStatement.executeUpdate();

                        }
                        // ну и вконце обозначим, что для этой даты обновление уже произошло в раннем времени.
                        supportMap.put(room, dateDuty);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("/////UtilsDB -> updateDutyDates -> CATCH");
            e.printStackTrace();
        }
    }


    // обновляем даты дежурств тех людей, у которых появились новые даты в гугл таблицах
//    public static void updateDateDutyFromReminderEntitys(ArrayList<ReminderEntity> updatedReminders) {
//        logger.info("/////UtilsDB -> updateDateDutyFromReminderEntitys -> updatedReminders == {}", updatedReminders);
//        try (Connection connection = dataConnection()) {
//            for (ReminderEntity r: updatedReminders) {
//
//                ArrayList<Boolean> confirmation = new ArrayList<>(Arrays.asList(false,false,false));
//                confirmation = new ArrayList<>(checkSendConfirmation(confirmation, r.getDateDuty()));
//
//                PreparedStatement preparedStatement = connection.prepareStatement("update reminder_for_duty set number_room = ?, date_duty = ? ,send_confirmation_today = ?, send_confirmation_tomorrow = ?, send_confirmation_after_tomorrow = ? where chat_id = ?");
//                preparedStatement.setString(1, r.getNumberRoom());
//                preparedStatement.setString(2, r.getDateDuty());
//                preparedStatement.setBoolean(3, confirmation.get(0));
//                preparedStatement.setBoolean(4, confirmation.get(1));
//                preparedStatement.setBoolean(5, confirmation.get(2));
//                preparedStatement.setString(6, r.getChatId());
//
//                preparedStatement.executeUpdate();
//            }
//
//        } catch (Exception e) {
//            logger.warn("/////UtilsDB -> updateDateDutyFromReminderEntitys -> CATCH");
//            e.printStackTrace();
//        }
//    }
}
