package com.attendant.telegramBotAssistant;

import com.attendant.model.ReminderEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.attendant.utils.UtilsSpreadsheet.*;
import static com.attendant.utils.UtilsDB.*;

public class TelegramBotAssistant extends TelegramLongPollingBot {

    private boolean checkCreateReminder = false; // флаг, означающий, что пользователь хочет установить напоминание

    private static Logger logger = LoggerFactory.getLogger(TelegramBotAssistant.class);

    @Override
    public synchronized void onUpdateReceived(Update update) {
        logger.info("onUpdateReceived");
        String message = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();
//        if (checkCreateReminder) {
//            message = "2 этап создания напоминания";
//        }
        switch (message) {
            case "/start":
                logger.info("-!-!-!-!-!-!-!-!-!- onUpdateReceived -> /start");
                message = "Привет! Напиши номер комнаты, дату дежурства которой хочешь узнать";
                sendMsg(message, chatId);
                break;
            case "Создать напоминание":
                logger.info("-!-!-!-!-!-!-!-!-!- onUpdateReceived -> Создать напоминание");
                message = "Введите комнату на которую хотите создать напоминание";
                checkCreateReminder = true;
                sendMsg(message, chatId);
                break;
//            case "2 этап создания напоминания":
//            logger.info("-!-!-!-!-!-!-!-!-!- onUpdateReceived -> 2 этап создания напоминания");
//                createReminderAndSendMessage(message, chatId);
//                break;
            default:
                logger.info("-!-!-!-!-!-!-!-!-!- onUpdateReceived -> default switch");
                sendMsgSearchDateDutyInGoogleSpreadsheet(message, chatId);
                break;
        }

    }

//    private synchronized void createReminderAndSendMessage(String room, String chatId) {
//        SendMessage sendMessage = new SendMessage();
//        sendMessage.enableMarkdown(true);
//        sendMessage.setChatId(chatId);
//
//        try {
//            if (checkExistenceThisRoomAndSetDateDutyToSendMessage(room, sendMessage) && checkCreateReminder) {
//                checkCreateReminder = false; // флаг. когда он false, это значит, что установки напоминания небыло. сейчас меняем в false потому, что установка напоминание происходит сейчас
//
//                String dateDutyFromSendMessage = sendMessage.getText().trim(); // по моей логике, тут должна быть дата дежурства, которая добавилась из метода checkExistenceThisRoomAndSetDateDutyToSendMessage.
//                // находим целевую дату дежурства с помощью слова "дежурит"
//                String subString = "дежурит";
//                String dateDuty = "";
//
//                System.out.println("chat_id = " + chatId + " room = " + room + " date_duty = " + dateDutyFromSendMessage);
//                if (!dateDutyFromSendMessage.equals("")) {//если в гугл таблице существует дата следующего дежурства, то получить его из sendMessage и распарсить, та как там не чистая дата а уже готовое сообщение.
//                    dateDuty = dateDutyFromSendMessage.substring(dateDutyFromSendMessage.indexOf(subString) + subString.length()).trim();
//                }
//
//
//                // Пользователь просто хочет узнать дату дежурства
//                if (dateDuty.equals("")) {
//
//                    try {// в таблице нету след даты дежурства, но напоминание все равно должно быть установлено
//                        saveReminderInDB(chatId, room, dateDuty);
//                        setButtons(sendMessage, chatId);
//                        execute(sendMessage.setText("Напоминание создано. пока следующей даты дежурства нет. " +
//                                "Бот будет предупреждать о наступлении дежурства в течении 3-х дней перед " +
//                                "дежурством включая день дежурства." +
//                                " Следите за графиком здесь: https://docs.google.com/spreadsheets/d/1emj4PwGeoEhagVu9YlydMjwgLyxtbf8N5wa7Ai7Z7PQ/edit#gid=1096564453"));
//
//                    } catch (Exception e) {
//                        setButtons(sendMessage, chatId);
//                        execute(sendMessage.setText("Что-то пошло не так( мы работаем над устранением неисправности"));
//                        System.out.println("catch create reminder in sendMsgSearchDateDutyInGoogleSpreadsheet");
//                        e.printStackTrace();
//                    }
//
//                } else {
//
//                    try { // все ок, комната найдена и напоминание установлено
//                        saveReminderInDB(chatId, room, dateDuty);
//                        setButtons(sendMessage, chatId);
//                        execute(sendMessage.setText("Напоминание создано. Бот напомнит о дежурстве в течении " +
//                                "3 дней до наступления дежурства включая день дежурства."));
//                    } catch (TelegramApiException e) {
//                        setButtons(sendMessage, chatId);
//                        execute(sendMessage.setText("Что-то пошло не так"));
//                        System.out.println("sendMsgSearchDateDutyInGoogleSpreadsheet catch create reminder");
//                        e.printStackTrace();
//                    }
//
//                }
//
//            } else if (checkCreateReminder) {
//                execute(sendMessage.setText("Такой комнаты нет в графике")); // если такой комнаты не существует
//            }
//
//        } catch (TelegramApiException e) {
//            System.out.println("createReminderAndSendMessage catch");
//            e.printStackTrace();
//        }
//    }

    private synchronized void sendMsg(String message, String chatId) {
        SendMessage sendMessage = new SendMessage();
        setButtons(sendMessage, chatId);
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        try {
            logger.info("-!-!-!-!-!-!-!-!-!- sendMsg");
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private synchronized void sendMsgSearchDateDutyInGoogleSpreadsheet(String room, String chatId) {
        logger.info("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet");
        SendMessage sendMessage = new SendMessage();
        setButtons(sendMessage, chatId);
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);

        boolean checkExistenceThisRoom = checkExistenceThisRoomAndSetDateDutyToSendMessage(room, sendMessage);
        try {
            if (checkExistenceThisRoom && !checkCreateReminder) {
                if (sendMessage.getText().trim().equals("")) {
                    logger.info("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet -> sendMessage.getText().trim().equals(\"\") = true + {}", sendMessage.toString());
                    execute(sendMessage.setText("В графике пока нет даты следующего дежурства." +
                            " Следите за графиком здесь: https://docs.google.com/spreadsheets/d/1emj4PwGeoEhagVu9YlydMjwgLyxtbf8N5wa7Ai7Z7PQ/edit#gid=1096564453"));
                } else {
                    logger.info("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet -> sendMessage.getText().trim().equals(\"\") = false + {}", sendMessage.toString());
                    execute(sendMessage);
                }
            } else if (!checkCreateReminder) {// здесь нужно это условие, что бы в случае установки напоминание бот не попадал в этот else
                logger.info("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet -> else if");
                execute(sendMessage.setText("Такой комнаты нет в графике"));
            }

            if (checkExistenceThisRoom && checkCreateReminder) {
                logger.info("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet -> checkCreateReminder = true");
                checkCreateReminder = false; // флаг. когда он false, это значит, что установки напоминания небыло. сейчас меняем в false потому, что установка напоминание происходит сейчас

                String dateDutyFromSendMessage = sendMessage.getText().trim(); // по моей логике, тут должна быть дата дежурства, которая добавилась из метода checkExistenceThisRoomAndSetDateDutyToSendMessage.
                // находим целевую дату дежурства с помощью слова "дежурит"
                String subString = "дежурит";
                String dateDuty = "";

                if (!dateDutyFromSendMessage.equals("")) {//если в гугл таблице существует дата следующего дежурства, то получить его из sendMessage и распарсить, та как там не чистая дата а уже готовое сообщение.
                    dateDuty = dateDutyFromSendMessage.substring(dateDutyFromSendMessage.indexOf(subString) + subString.length()).trim();
                }
                logger.info("chat_id == {}, room == {}, dateDutyFromSendMessage == {}, dateDuty == {}", chatId, room, dateDutyFromSendMessage, dateDuty);

                // Пользователь просто хочет узнать дату дежурства
                if (dateDuty.equals("")) {
                    try {// в таблице нету след даты дежурства, но напоминание все равно должно быть установлено
                        logger.info("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet -> dateDuty.equals(\"\") = {}", dateDuty.equals(""));
                        saveReminderInDB(chatId, room, dateDuty);
                        setButtons(sendMessage, chatId);
                        execute(sendMessage.setText("Напоминание создано. пока следующей даты дежурства нет. " +
                                "Бот будет предупреждать о наступлении дежурства в течении 3-х дней перед " +
                                "дежурством включая день дежурства." +
                                " Следите за графиком здесь: https://docs.google.com/spreadsheets/d/1emj4PwGeoEhagVu9YlydMjwgLyxtbf8N5wa7Ai7Z7PQ/edit#gid=1096564453"));

                    } catch (Exception e) {
                        logger.warn("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet -> dateDuty.equals(\"\") CATCH");
                        setButtons(sendMessage, chatId);
                        execute(sendMessage.setText("Что-то пошло не так( мы работаем над устранением неисправности"));
                        e.printStackTrace();
                    }

                } else {

                    try { // все ок, комната найдена и напоминание установлено
                        logger.info("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet -> dateDuty.equals(\"\") = {}, {}", dateDuty.equals(""), dateDuty);
                        saveReminderInDB(chatId, room, dateDuty);
                        setButtons(sendMessage, chatId);
                        execute(sendMessage.setText("Напоминание создано. Бот напомнит о дежурстве в течении " +
                                "3 дней до наступления дежурства включая день дежурства."));
                    } catch (TelegramApiException e) {
                        logger.warn("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet -> dateDuty.equals(\"\") == false CATCH");
                        setButtons(sendMessage, chatId);
                        execute(sendMessage.setText("Что-то пошло не так"));
                        e.printStackTrace();
                    }

                }

            } else if (checkCreateReminder) {
                logger.info("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet -> checkExistenceThisRoomAndSetDateDutyToSendMessage(room, sendMessage) = false, checkCreateReminder = true");
                execute(sendMessage.setText("Такой комнаты нет в графике")); // если такой комнаты не существует
            }

        } catch (Exception e) {
            logger.warn("-!-!-!-!-!-!-!-!-!- sendMsgSearchDateDutyInGoogleSpreadsheet CATCH");
            e.printStackTrace();
        }
    }

    public static synchronized void setButtons(SendMessage sendMessage, String chatId) {
        logger.info("-!-!-!-!-!-!-!-!-!- setButtons");
        String room = getRoomRemainderByChatId(chatId).trim();
        //Создаем клавиуатуру
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        KeyboardButton keyboardButton = new KeyboardButton("Создать напоминание");
        KeyboardButton keyboardButton2 = new KeyboardButton();

        if (room.equals("")) {
            keyboardButton2.setText("Напоминание пока не установлено");
        } else {
            keyboardButton2.setText("Напоминание установлено на " + room + " комнату");
        }
        logger.info("-!-!-!-!-!-!-!-!-!- setButtons -> keyboardButton2.getText() == {}", keyboardButton2.getText());
        KeyboardRow keyboardRow = new KeyboardRow();

        keyboardRow.add(keyboardButton);
        keyboardRow.add(keyboardButton2);

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(keyboardRow);

        replyKeyboardMarkup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
    }

    /*
     * 1. key - это значение дня. 0 - это текущий день. 1 - завтрашний. 2 - послезавтрашний
     * 2. ArrayList<ReminderEntity> - это списки напоминаний для соответстующих дней(для тех, кто дежурит сегодня, завтра поле-завтра)
     */
    public synchronized void sendReminder(HashMap<Integer, ArrayList<ReminderEntity>> mapReminders) {
        logger.info("-!-!-!-!-!-!-!-!-!- sendReminder -> {}", mapReminders.toString());
        for (Map.Entry<Integer, ArrayList<ReminderEntity>> entry : mapReminders.entrySet()) {
            int dayDuty = entry.getKey(); // 0 - сегодня, 1 - завтра, 2 - послезавтра
            String message = "";
            for (ReminderEntity r : entry.getValue()) {
                switch (dayDuty) {
                    case 0:
                        if (r.isSendConfirmationCurDay()) {
                            message = "Привет! Комната " + r.getNumberRoom() + " сегодня дежурная. Удачного дня :)";
                            sendMsg(message, r.getChatId());
                        }
                        break;
                    case 1:
                        if (r.isSendConfirmationOneDay()) {
                            message = "Привет! Комната " + r.getNumberRoom() + " завтра дежурная. Удачного дня :)";
                            sendMsg(message, r.getChatId());
                        }
                        break;
                    case 2:
                        if (r.isSendConfirmationTwoDay()) {
                            message = "Привет! Комната " + r.getNumberRoom() + " дежурнит через 2 дня (" + r.getDateDuty() + "). Удачного дня :)";
                            sendMsg(message, r.getChatId());
                        }
                        break;
                    default:
                        logger.warn("-!-!-!-!-!-!-!-!-!- sendReminder -> default -> WARNING");
                        break;
                }
            }
        }

        setStatusSendingReminder(mapReminders);
    }

    @Override
    public String getBotUsername() {
        return "Assistant_for_duty";
    }

    @Override
    public String getBotToken() {
        return "701533339:AAGZpsNPVAblKnbq_kxkYOcTMX3F4gJKlSg";
    }

}
