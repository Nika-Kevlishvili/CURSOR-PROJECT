package bg.energo.phoenix.service.signing.qes.repositories;


import bg.energo.phoenix.service.signing.qes.entities.QesSignSessionObjects;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QesSignSessionObjectsRepository extends JpaRepository<QesSignSessionObjects, Long> {

    void deleteByQesSignSessionIdAndQesDocumentDetailsId(Long sessionId, Long documentId);

    void deleteAllByQesSignSessionIdIn(List<Long> ids);

    void deleteAllByQesSignSessionId(Long qesSignSessionId);

    @Query("""
                delete from QesDocumentDetails qdd
                where qdd.id in (select qos.qesDocumentDetails.id  from QesSignSessionObjects qos
                where qos.qesSignSessionId in (:stuckSessionIds))
            """)
    @Modifying
    void deleteInProgressQesDetails(List<Long> stuckSessionIds);

    boolean existsByQesSignSessionId(Long qesSessionId);

    int countAllByQesSignSessionId(Long id);
}
