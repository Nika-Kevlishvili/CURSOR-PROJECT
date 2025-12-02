package bg.energo.phoenix.repository.notification;

import bg.energo.phoenix.model.entity.notification.UserNotification;
import bg.energo.phoenix.model.response.notification.UserNotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    @Query("""
            select new bg.energo.phoenix.model.response.notification.UserNotificationResponse(un)
            from UserNotification un
            where un.accountManagerId = :accountManagerId
            and ((un.isReaden and un.readDate > :readenNotificationDeadline) or (un.isReaden = false))
            order by un.isReaden, un.createDate desc
            """)
    Page<UserNotificationResponse> findUserNotificationByAccountManagerId(Long accountManagerId,
                                                                          LocalDateTime readenNotificationDeadline,
                                                                          Pageable pageable);

    @Query("""
            select count(un.id)
            from UserNotification un
            where un.accountManagerId = :accountManagerId
            and un.isReaden <> true
            """)
    long countUnreadenUserNotificationByAccountManagerId(Long accountManagerId);

    @Modifying
    @Transactional
    @Query("""
            delete from UserNotification un
            where (un.isReaden and un.readDate < :readenDeadline)
            or (un.isReaden = false and un.createDate < :unreadenDeadline)
            """)
    void deleteOutDatedUserNotifications(LocalDateTime unreadenDeadline, LocalDateTime readenDeadline);
}
