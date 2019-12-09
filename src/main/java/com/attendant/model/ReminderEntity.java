package com.attendant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReminderEntity {

    private String chatId;
    private String numberRoom;
    private String dateDuty;
    private boolean sendConfirmationToday;
    private boolean sendConfirmationTomorrow;
    private boolean sendConfirmationAfterTomorrow;
    private boolean sendConfirmationTodayIn1600;
}
