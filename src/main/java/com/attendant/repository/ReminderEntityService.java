package com.attendant.repository;

import com.attendant.entity.ReminderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReminderEntityService extends JpaRepository<ReminderEntity, Long> {

}
