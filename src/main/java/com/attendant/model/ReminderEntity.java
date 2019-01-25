package com.attendant.model;

import lombok.Data;

@Data
public class ReminderEntity {

    private String chatId;
    private String numberRoom;
    private String dateDuty;
    private boolean sendConfirmationToday;
    private boolean sendConfirmationTomorrow;
    private boolean sendConfirmationAfterTomorrow;
}
