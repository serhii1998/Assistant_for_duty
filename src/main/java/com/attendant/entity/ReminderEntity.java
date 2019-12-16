package com.attendant.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "reminder_for_duty")
public class ReminderEntity {
    @Id
    private Long id;
    @Column(name = "chat_id")
    private String chatId;
    @Column(name = "number_room")
    private String numberRoom;
    @Column(name = "date_duty")
    private String dateDuty;
    @Column(name = "send_confirmation_today")
    private boolean sendConfirmationToday;
    @Column(name = "send_confirmation_tomorrow")
    private boolean sendConfirmationTomorrow;
    @Column(name = "send_confirmation_after_tomorrow")
    private boolean sendConfirmationAfterTomorrow;
    @Column(name = "send_confirmation_today_duty_in_1600")
    private boolean sendConfirmationTodayIn1600;
}
