package bg.energo.phoenix.repository.crm.emailCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationRelatedCustomer;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface EmailCommunicationRelatedCustomerRepository extends JpaRepository<EmailCommunicationRelatedCustomer, Long> {
    @Query("""
            select new bg.energo.phoenix.model.response.shared.ShortResponse(
                customer.id,
                case when customer.customerType = 'LEGAL_ENTITY' then concat(customer.identifier, ' (', customerDetails.name, ')')
                    else replace(concat(customer.identifier , ' (',coalesce(customerDetails.name, '') , ' ', coalesce(customerDetails.middleName, '') , ' ', coalesce(customerDetails.lastName, ''), ')'), '\\s+', ' ') end
            )
            from EmailCommunicationRelatedCustomer emailCommunicationRelatedCustomer
            join Customer customer on customer.id = emailCommunicationRelatedCustomer.customerId
            join CustomerDetails customerDetails on customerDetails.id = customer.lastCustomerDetailId
            where emailCommunicationRelatedCustomer.emailCommunicationId = :emailCommunicationId
            and emailCommunicationRelatedCustomer.status ='ACTIVE'
            """
    )
    List<ShortResponse> findByEmailCommunicationId(Long emailCommunicationId);

    Set<EmailCommunicationRelatedCustomer> findAllByEmailCommunicationIdAndStatus(Long emailCommunicationId, EntityStatus entityStatus);

}
