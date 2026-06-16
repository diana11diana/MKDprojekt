package com.dsms.notification;

import com.dsms.auth.AuthException;
import com.dsms.notification.NotificationDtos.NotificationResponse;
import com.dsms.user.User;
import com.dsms.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void notify(User user, NotificationType type, String title, String body) {
        repository.save(new Notification(user, type, title, body));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listMine(String email) {
        User user = getUser(email);
        return repository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(NotificationResponse::from).toList();
    }

    @Transactional
    public NotificationResponse markRead(Long id, String email) {
        User user = getUser(email);
        Notification notification = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.markRead();
        return NotificationResponse.from(notification);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
