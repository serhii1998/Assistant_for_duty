package com.attendant.service;

import com.attendant.entity.ReminderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public interface ReminderEntityService extends JpaRepository<ReminderEntity, Long> {

}
