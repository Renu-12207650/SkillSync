package in.skillsync.notification.repository;

import in.skillsync.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

    List<Notification> findByRecipientUserIdAndReadFalseOrderByCreatedAtDesc(Long recipientUserId);

    long countByRecipientUserIdAndReadFalse(Long recipientUserId);
}
