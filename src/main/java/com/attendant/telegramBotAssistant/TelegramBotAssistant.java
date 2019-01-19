package com.attendant.telegramBotAssistant;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static com.attendant.utils.UtilsSpreadsheet.*;


public class TelegramBotAssistant extends TelegramLongPollingBot {

    private boolean createReminder = false;

    @Override
    public void onUpdateReceived(Update update) {
        String message = update.getMessage().getText();
        try {
            sendMsg(update.getMessage().getChatId().toString(), message, update);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    private synchronized void sendMsg(String chatId, String s, Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        //setButtons(sendMessage);
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        if(s.equals("/start")){
            sendMessage.setText("Привет! Напиши номер комнаты, дату дежурства которой хочешь узнать");
            execute(sendMessage);
            return;
        }

        if(checkExistenceThisRoomAndSetDateAttendantToSendMessage(s, sendMessage) && !createReminder){
            workDB(update);
            if (sendMessage.getText().trim().equals("")){
                execute(sendMessage.setText("В графике пока нет даты следующего дежурства." +
                        " Следите за графиком здесь: https://docs.google.com/spreadsheets/d/1emj4PwGeoEhagVu9YlydMjwgLyxtbf8N5wa7Ai7Z7PQ/edit#gid=1096564453"));
            }else {
                execute(sendMessage);
            }
        }else {
            execute(sendMessage.setText("Такой комнаты нет в графике"));
        }

        if(checkExistenceThisRoomAndSetDateAttendantToSendMessage(s, sendMessage) && createReminder){

        }
    }

    private void workDB(Update update){
        /*
        * давай поразсуждаем. вот я уже определил, что эта комната существует в гугл таблице. все ок.
        * но пользователь хочет установить напоинание. Для этого:
         * 1. он должен ввести свою комнату.
         * 2. я эту комнату запоминаю, запоминаю chatid, с которого пришел запрос на напоминание.
         * 3. нахожу дату дежурства, когда надо ему напомнить.
         * 4. запоминаю эту дату и записываю ее в базу к тому чату, от которого поступил запрос
         * 5. потом у меня будет поток, который будет запускаться 1 раз в сутки, сканировать базу данных
         * 6. он будет искать даты в ML? сравнивать с текущей.
         * 7. если текущая дата на 2 меньше, чем в бд, то нужно отослать напоминание тому чату,
         * с которого поступил запрос на напоминание
         * 8. собственно вроде бы все. */



    }


    public synchronized void setButtons(SendMessage sendMessage) {
        // Создаем клавиуатуру
//        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
//        sendMessage.setReplyMarkup(replyKeyboardMarkup);
//        replyKeyboardMarkup.setSelective(true);
//        replyKeyboardMarkup.setResizeKeyboard(true);
//        replyKeyboardMarkup.setOneTimeKeyboard(false);
//
//        // Создаем список строк клавиатуры
//        List<KeyboardRow> keyboard = new ArrayList<>();
//
//        // Первая строчка клавиатуры
//        KeyboardRow keyboard1 = new KeyboardRow();
//        // Добавляем кнопки в первую строчку клавиатуры
//        keyboard1.add(new KeyboardButton("USD rate UAH"));
//
//        // Вторая строчка клавиатуры
//        KeyboardRow keyboard2 = new KeyboardRow();
//        // Добавляем кнопки во вторую строчку клавиатуры
//        keyboard2.add(new KeyboardButton("EUR rate UAH"));
//
//        //Третья
//        KeyboardRow keyboard3 = new KeyboardRow();
//        // Добавляем кнопки в третью строчку клавиатуры
//        keyboard3.add(new KeyboardButton("RUB rate UAH"));
//
//        // Добавляем все строчки клавиатуры в список
//        keyboard.add(keyboard1);
//        keyboard.add(keyboard2);
//        keyboard.add(keyboard3);
//        // и устанваливаем этот список нашей клавиатуре
//        replyKeyboardMarkup.setKeyboard(keyboard);
    }

    @Override
    public String getBotUsername() {
        return "AssistantAttendant";
    }

    @Override
    public String getBotToken() {
        return "701533339:AAGZpsNPVAblKnbq_kxkYOcTMX3F4gJKlSg";
    }

}
