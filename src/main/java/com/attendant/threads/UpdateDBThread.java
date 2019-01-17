package com.attendant.threads;

import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Update;


public class UpdateDBThread extends Thread {

    private String name;
    private Update update;

    public UpdateDBThread(Update update, String name){
        super(name);
        this.name = name;
        this.update = update;
    }


    @Override
    public void run() {
        Contact contact = update.getMessage().getContact();
        String room;
    }
}
