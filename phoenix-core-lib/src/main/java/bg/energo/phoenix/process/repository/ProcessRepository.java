package bg.energo.phoenix.process.repository;

import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.process.model.enums.ProcessStatus;
import bg.energo.phoenix.process.model.enums.ProcessType;
import bg.energo.phoenix.service.notifications.interfaces.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ProcessRepository extends Notification, JpaRepository<Process, Long> {

    @Query(
            """
                    Select p from Process p
                    where (
                        :prompt is null
                            or (:searchBy = 'all' and lower(p.name) like :prompt)
                            or (:searchBy = 'name' and lower(p.name) like :prompt)
                    )
                    and (CAST(:processStatus as string) is null or p.status = :processStatus)
                    and (CAST(:createdDateFrom as date) is null or p.createDate >= :createdDateFrom)
                    and (CAST(:createdDateTo as date) is null or p.createDate <= :createdDateTo)
                    and (CAST(:startDateFrom as date) is null or p.processStartDate >= :startDateFrom)
                    and (CAST(:startDateTo as date) is null or p.processStartDate <= :startDateTo)
                    and (CAST(:completeDateFrom as date) is null or p.processCompleteDate >= :completeDateFrom)
                    and (CAST(:completeDateTo as date) is null or p.processCompleteDate <= :completeDateTo)
                    and (:systemUserId is null or p.systemUserId = :systemUserId)
                    """
    )
    Page<Process> findAll(
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("processStatus") ProcessStatus processStatus,
            @Param("createdDateFrom") LocalDateTime createdDateFrom,
            @Param("createdDateTo") LocalDateTime createdDateTo,
            @Param("startDateFrom") LocalDateTime startDateFrom,
            @Param("startDateTo") LocalDateTime startDateTo,
            @Param("completeDateFrom") LocalDateTime completeDateFrom,
            @Param("completeDateTo") LocalDateTime completeDateTo,
            @Param("systemUserId") String systemUserId,
            Pageable pageable
    );

    Set<Process> findAllByReminderIdAndTypeAndStatusIn(Long reminderId, ProcessType type, Collection<ProcessStatus> status);

    boolean existsByIdAndStatusNotIn(Long id, List<ProcessStatus> status);

    @Query(value = """
            (
                SELECT pn.performer_id
                FROM process_management.process p
                         LEFT JOIN process_management.process_notification pn ON pn.process_id = p.id
                WHERE pn.performer_id IS NOT NULL
                  AND p.id = :entityId
                  AND (coalesce(:notificationState, '') = '' or text(pn.notification_type) = text(:notificationState))
            )
            UNION
            (
                SELECT am.id
                FROM process_management.process p
                         JOIN process_management.process_notification pn ON pn.process_id = p.id
                         JOIN customer.portal_tags pt ON pn.performer_tag_id = pt.id
                         JOIN customer.account_manager_tags camt ON pt.id = camt.portal_tag_id
                         JOIN customer.account_managers am ON camt.account_manager_id = am.id
                WHERE p.id = :entityId
                AND (coalesce(:notificationState, '') = '' or text(pn.notification_type) = text(:notificationState))
            )
            """, nativeQuery = true)
    List<Long> getNotificationTargets(Long entityId, String notificationState);
}
