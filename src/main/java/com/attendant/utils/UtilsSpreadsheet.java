package com.attendant.utils;

import com.attendant.googleSpreadsheet.SheetsQuickstart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class UtilsSpreadsheet {

    private static Logger logger = LoggerFactory.getLogger(UtilsSpreadsheet.class);

    public static boolean todayBeforeOrEqualsThisDate(String dateDutyStr) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        Date today = format.parse(format.format(new Date()));
        Date dateDuty = format.parse(dateDutyStr);

//        logger.info("******** UtilsSpreadsheet -> todayBeforeOrEqualsThisDate == {}, dateDutyStr == {}, today == {}", today.getTime() - dateDuty.getTime() > 0, dateDuty, today);
        return today.before(dateDuty) || today.equals(dateDuty);
    }

    //возвращает true в случае, если искомая комната найдена
    public static boolean checkExistenceThisRoomAndSetDateDutyToSendMessage(String room, SendMessage sendMessage) {
        sendMessage.setText("");
        boolean existenceThisRoom = false; // флаг существования комнаты в гугл таблице.
        // Если ее там в гугл таблицах нет, то ее не нужно записывать в БД

        List<List<Object>> values = null;
        try {

            values = SheetsQuickstart.infoAttendantGoogleSpreadsheet("Database!A3:S"); // получаем заданную страницу в виде вдумерного массива
            logger.info("******** UtilsSpreadsheet -> checkExistenceThisRoomAndSetDateDutyToSendMessage -> room(message) == {}, sendMessage == {}", room, sendMessage.toString());
            System.out.println(values);
            checkDate:
            if (values != null) {
                for (List row : values) {
                    for (int i = 0; i < row.size(); i++) {
                        if (room.equals(row.get(i).toString())) { // если переданая в сообщении комната равна комнате в БД из Гугл таблиц
                            existenceThisRoom = true;
                            String dateDuty = row.get(i + 1).toString().trim();// то достанем дату дежурства, а это соседняя колонка
                            try {
                                if (!dateDuty.equals("") && todayBeforeOrEqualsThisDate(dateDuty)) { // прошла ли дата дежурства
                                    sendMessage.setText("Комната " + room + " дежурит " + dateDuty);// ВНИМАНИЕ!!! изменение єтого сообщения, которое в setText,
                                    //повлечет за собой изменение в данных, которые заносятся в базу. я парсю это сообщение, что бы найти в нем дату дежурства
                                    // (dateDuty). главное, что-бы осталось слово дежурит а за этим словом дальше шла дата,
                                    // так как привязываюсь для поиска я именно к слову "дежурит"
                                    break checkDate;
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.warn("******** UtilsSpreadsheet ->  checkExistenceThisRoomAndSetDateDutyToSendMessage -> Exception values");
            e.printStackTrace();
        }

        logger.info("******** UtilsSpreadsheet -> checkExistenceThisRoomAndSetDateDutyToSendMessage -> \n sendMessage.getText() == {}, existenceThisRoom == {}", sendMessage.getText(), existenceThisRoom);
        return existenceThisRoom;
    }

    // метод, который сетит даты дежурств тем людям, у которых их небыло, или они уже подежурили. достаем из базы людей без дежурств и ищем в гугл доках, появились ли даты. добавляю в коллекцию и возвращаю тех, кому добавились даты
//    public static List<ReminderEntity> setDatesOfDutyToThoseRoomsThatDidNotHave(List<ReminderEntity> entityList) {
//        List<List<Object>> values = null;
//        ArrayList<ReminderEntity> entityHaveAddedDatesDuty = new ArrayList<>();
//        try {
//            values = SheetsQuickstart.infoAttendantGoogleSpreadsheet("Database!A3:S"); // получаем заданную страницу в виде двумерного массива
//            if (values != null) {
//                for (List row : values) { // получаем все данные из гугл таблиц
//                    for (int i = 1; i < row.size(); i += 2) { // выбираем их по строкам. берем с 1 ячейки, там идет комната. рядом дата.
//
//                        String room = row.get(i).toString().trim();
//                        String dateDuty = row.get(i + 1).toString().trim();// вот и дата, которая рядом с комнатой
//
//                        if (!dateDuty.equals("") && todayBeforeOrEqualsThisDate(dateDuty)) { // ищем напоминание в том случае, если дата не прошла дату дежурсива
//                            Iterator<ReminderEntity> iterator = entityList.iterator();
//                            while (iterator.hasNext()) {
//                                ReminderEntity r = iterator.next();
//                                if (room.equals(r.getNumberRoom())) {// если комната равна комнате в напоминании, то добавить в напомининие дату,
//                                    // добавить напоминание в выходную коллекцию. удалить напоминание, что бі больше не возвращаться к нему
//                                    r.setDateDuty(dateDuty);
//                                    entityHaveAddedDatesDuty.add(r);
//                                    iterator.remove();
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            logger.warn("******** UtilsSpreadsheet -> setDatesOfDutyToThoseRoomsThatDidNotHave -> Exception values");
//            e.printStackTrace();
//        }
//        logger.info("******** UtilsSpreadsheet -> setDatesOfDutyToThoseRoomsThatDidNotHave -> entityHaveAddedDatesDuty == {}", entityHaveAddedDatesDuty.toString());
//        return entityHaveAddedDatesDuty;
//    }
}
