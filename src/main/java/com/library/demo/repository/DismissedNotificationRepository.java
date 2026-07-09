package com.library.demo.repository;

import com.library.demo.model.DismissedNotification;
import com.library.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DismissedNotificationRepository extends JpaRepository<DismissedNotification, Long> {
    List<DismissedNotification> findByUser(User user);
    boolean existsByUserAndNotificationKey(User user, String notificationKey);
}
