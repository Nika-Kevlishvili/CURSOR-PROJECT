package bg.energo.phoenix.repository.crm.emailCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmailCommunicationAttachmentRepository extends JpaRepository<EmailCommunicationAttachment, Long> {

    Optional<EmailCommunicationAttachment> findByIdAndStatus(Long id, EntityStatus status);

    @Query(value = """
            SELECT ecf.fileUrl
            FROM EmailCommunicationAttachment ecf
            WHERE ecf.emailCommunicationId = :emailCommunicationId
            AND ecf.status = 'ACTIVE'
            """
    )
    List<String> findAllByEmailCommunicationIdAndStatus(Long emailCommunicationId);

    @Query(value = """
            SELECT ecf
            FROM EmailCommunicationAttachment ecf
            WHERE ecf.emailCommunicationId = :emailCommunicationId
            AND ecf.status = 'ACTIVE'
            """
    )
    List<EmailCommunicationAttachment> findAllActiveAttachmentsByEmailCommunicationId(Long emailCommunicationId);

    @Query(value = """
            SELECT ecf.id
            FROM EmailCommunicationAttachment ecf
            WHERE ecf.emailCommunicationId = :emailCommunicationId
            AND ecf.status = 'ACTIVE'
            """
    )
    List<Long> findAllActiveAttachmentIdsByEmailCommunicationId(Long emailCommunicationId);

}
