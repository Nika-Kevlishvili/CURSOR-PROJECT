package bg.energo.phoenix.repository.crm.emailCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationCustomerAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailCommunicationCustomerAttachmentRepository extends JpaRepository<EmailCommunicationCustomerAttachment, Long> {

    @Query(value = """
            SELECT ecca
            FROM EmailCommunicationCustomerAttachment ecca
            WHERE ecca.emailCommunicationCustomerId = :emailCommunicationCustomerId
            AND ecca.status = 'ACTIVE'
            """
    )
    List<EmailCommunicationCustomerAttachment> findAllActiveAttachmentsByEmailCommunicationCustomerId(Long emailCommunicationCustomerId);

    Optional<EmailCommunicationCustomerAttachment> findByIdAndStatus(Long id, EntityStatus status);

}
