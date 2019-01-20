package com.attendant.telegramBotAssistant;

import com.attendant.model.ReminderEntity;
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

import static com.attendant.utils.UtilsSpreadsheet.*;
import static com.attendant.utils.UtilsDB.*;

public class TelegramBotAssistant extends TelegramLongPollingBot {

    private boolean createReminder = false; // флаг, означающий, что пользователь хочет установить напоминание

    @Override
    public synchronized void onUpdateReceived(Update update) {
        String message = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();
        switch (message) {
            case "/start":
                message = "Привет! Напиши номер комнаты, дату дежурства которой хочешь узнать";
                sendMsg(message, chatId);
                break;
            case "Создать напоминание":
                message = "Введите комнату на которую хотите создать напоминание";
                createReminder = true;
                sendMsg(message, chatId);
                break;
            default:
                System.out.println("1");
                sendMsgSearchDateDutyInGoogleSpreadsheet(chatId, message);
                break;
        }

    }

    private synchronized void sendMsg(String message, String chatId) {
        SendMessage sendMessage = new SendMessage();
        setButtons(sendMessage);
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }



    private synchronized void sendMsgSearchDateDutyInGoogleSpreadsheet(String chatId, String room) {
        SendMessage sendMessage = new SendMessage();
        setButtons(sendMessage);
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);

        try {
            if (checkExistenceThisRoomAndSetDateDutyToSendMessage(room, sendMessage) && !createReminder) {
                if (sendMessage.getText().trim().equals("")) {
                    execute(sendMessage.setText("В графике пока нет даты следующего дежурства." +
                            " Следите за графиком здесь: https://docs.google.com/spreadsheets/d/1emj4PwGeoEhagVu9YlydMjwgLyxtbf8N5wa7Ai7Z7PQ/edit#gid=1096564453"));
                } else {
                    execute(sendMessage);
                }
            } else if (!createReminder) {// здесь нужно это условие, что бы в случае установки напоминание бот не попадал в этот else
                execute(sendMessage.setText("Такой комнаты нет в графике"));
            }

            if (checkExistenceThisRoomAndSetDateDutyToSendMessage(room, sendMessage) && createReminder) {


                String dateDutyFromSendMessage = sendMessage.getText().trim(); // по моей логике, тут должна быть дата дежурства, которая добавилась из метода checkExistenceThisRoomAndSetDateDutyToSendMessage.
                // находим целевую дату дежурства с помощью слова "дежурит"
                System.out.println("chat_id = " + chatId + " room = " + room + " date_duty = " + dateDutyFromSendMessage);
                String subString = "дежурит";
                String dateDuty = "";

                if (!dateDutyFromSendMessage.equals("")) {
                    dateDuty = dateDutyFromSendMessage.substring(dateDutyFromSendMessage.indexOf(subString) + subString.length()).trim();
                }

                createReminder = false; // флаг. когда он false, это значит, что установки напоминания небыло.
                // Пользователь просто хочет узнать дату дежурства
                if (dateDuty.equals("")) {
                    try {// в таблице нету след даты дежурства, но напоминание все равно должно быть установлено
                        saveReminderInDB(chatId, room, dateDuty);
                        execute(sendMessage.setText("Напоминание создано. пока следующей даты дежурства нет. " +
                                "Бот будет предупреждать о наступлении дежурства в течении 3-х дней перед " +
                                "дежурством включая день дежурства." +
                                " Следите за графиком здесь: https://docs.google.com/spreadsheets/d/1emj4PwGeoEhagVu9YlydMjwgLyxtbf8N5wa7Ai7Z7PQ/edit#gid=1096564453"));

                    } catch (Exception e) {
                        execute(sendMessage.setText("Что-то пошло не так( мы работаем над устранением неисправности"));
                        System.out.println("catch create reminder in sendMsgSearchDateDutyInGoogleSpreadsheet");
                        e.printStackTrace();
                    }

                } else {
                    try { // все ок, комната найдена и напоминание установлено
                        saveReminderInDB(chatId, room, dateDuty);
                        execute(sendMessage.setText("Напоминание создано. Бот напомнит о дежурстве в течении " +
                                "3 дней до наступления дежурства включая день дежурства."));
                    } catch (TelegramApiException e) {
                        execute(sendMessage.setText("Что-то пошло не так"));
                        System.out.println("sendMsgSearchDateDutyInGoogleSpreadsheet catch create reminder");
                        e.printStackTrace();
                    }
                }
            } else if (createReminder) {
                execute(sendMessage.setText("Такой комнаты нет в графике")); // если такой комнаты не существует
            }

        } catch (Exception e) {
            System.out.println("sendMsgSearchDateDutyInGoogleSpreadsheet catch");
            e.printStackTrace();
        }
    }

    public static synchronized void setButtons(SendMessage sendMessage) {
        //Создаем клавиуатуру
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        KeyboardButton keyboardButton = new KeyboardButton("Создать напоминание");
        KeyboardButton keyboardButton2 = new KeyboardButton("222222");

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
    * 2. ArrayList<ReminderEntity> - это списки напоминаний для соответстующих дней(для тех, кто дежурит сегодня, завтра поле-завтра)*/
    public static synchronized void sendReminder(HashMap<Integer, ArrayList<ReminderEntity>> mapReminders){

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
