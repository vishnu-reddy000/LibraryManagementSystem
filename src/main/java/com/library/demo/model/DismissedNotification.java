package com.library.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "dismissed_notifications")
public class DismissedNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "notification_key", nullable = false)
    private String notificationKey;

    public DismissedNotification() {}

    public DismissedNotification(User user, String notificationKey) {
        this.user = user;
        this.notificationKey = notificationKey;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getNotificationKey() {
        return notificationKey;
    }

    public void setNotificationKey(String notificationKey) {
        this.notificationKey = notificationKey;
    }
}
