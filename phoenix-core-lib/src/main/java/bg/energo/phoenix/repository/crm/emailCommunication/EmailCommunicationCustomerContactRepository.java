package bg.energo.phoenix.repository.crm.emailCommunication;

import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationCustomerContact;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface EmailCommunicationCustomerContactRepository extends JpaRepository<EmailCommunicationCustomerContact, Long> {

    @Query(
            value = """
                    select eccc.emailAddress
                    from EmailCommunicationCustomerContact eccc
                    join EmailCommunicationCustomer ecc on ecc.id = eccc.emailCommunicationCustomerId
                    where ecc.emailCommunicationId = :emailCommunicationId
                     """
    )
    Set<String> findAllEmailAddressesByEmailCommunicationId(Long emailCommunicationId);

    @Query(
            value = """
                    select eccc.emailAddress
                    from EmailCommunicationCustomerContact eccc
                    where eccc.emailCommunicationCustomerId = :emailCommunicationCustomerId
                     """
    )
    Set<String> findAllEmailAddressesByEmailCommunicationCustomerId(Long emailCommunicationCustomerId);

    @Query(
            value = """
                    select eccc
                    from EmailCommunicationCustomerContact eccc
                    join EmailCommunicationCustomer ecc on ecc.id = eccc.emailCommunicationCustomerId
                    where ecc.emailCommunicationId = :emailCommunicationId
                     """
    )
    Set<EmailCommunicationCustomerContact> getAllByEmailCommunicationId(Long emailCommunicationId);

    List<EmailCommunicationCustomerContact> findAllByEmailCommunicationCustomerId(Long emailCommunicationCustomerId);

    void deleteAllByEmailCommunicationCustomerId(Long emailCommunicationCustomerId);

    @Query(
            nativeQuery = true,
            value = """
                    SELECT eccc.*
                    FROM crm.email_communication_customer_contacts eccc
                    WHERE eccc.create_date >= CURRENT_TIMESTAMP - INTERVAL '72 hours'
                    AND ((:statuses) is null or text(eccc.status)  in :statuses)
                    AND eccc.task_id notnull
                    """
    )
    List<EmailCommunicationCustomerContact> findAllByStatusInAndTaskIdNotNull(@Param("statuses") List<String> statuses);

}
