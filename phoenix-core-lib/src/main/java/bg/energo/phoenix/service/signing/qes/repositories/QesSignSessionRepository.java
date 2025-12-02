package bg.energo.phoenix.service.signing.qes.repositories;


import bg.energo.phoenix.model.entity.template.QesDocument;
import bg.energo.phoenix.service.signing.qes.entities.QesSignSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QesSignSessionRepository extends JpaRepository<QesSignSession, Long> {

    @Query("""
            select qss from QesSignSession qss
            where not exists (select qsso from QesSignSessionObjects qsso where qsso.qesSignSessionId=qss.id)
            """)
    List<QesSignSession> findFinishedSessions();

    @Query("""
            select qss from QesSignSession qss
            where :dateTime > qss.modifyDate
            """)
    List<QesSignSession> findStuckSessions(LocalDateTime dateTime);

    Optional<QesSignSession> findBySessionId(String qesSignSessionId);

    @Query("""
            select qd from QesSignSessionObjects qos
            join QesDocument qd on qd.id=qos.qesDocumentDetails.qesDocumentId
            where qos.qesSignSessionId in (:sessionId)
            """)
    List<QesDocument> findSessionDocuments(List<Long> sessionId);
}
