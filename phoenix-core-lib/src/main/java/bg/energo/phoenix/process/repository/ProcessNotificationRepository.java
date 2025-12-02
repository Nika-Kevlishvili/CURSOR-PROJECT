package bg.energo.phoenix.process.repository;

import bg.energo.phoenix.process.model.entity.ProcessNotification;
import bg.energo.phoenix.process.model.response.ProcessNotificationResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessNotificationRepository extends JpaRepository<ProcessNotification, Long> {

    List<ProcessNotification> findAllByProcessId(Long processId);

    @Query("""
            select new bg.energo.phoenix.process.model.response.ProcessNotificationResponse(am,pt,pn.notificationType)
            from ProcessNotification pn 
            left join PortalTag pt on pt.id=pn.performerTagId
            left join AccountManager am on am.id=pn.performerId
            where pn.processId=:processId
            
            """)
    List<ProcessNotificationResponse> findResponseByProcessId(Long processId);
}
