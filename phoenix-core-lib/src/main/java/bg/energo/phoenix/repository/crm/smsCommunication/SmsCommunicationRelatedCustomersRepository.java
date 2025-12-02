package bg.energo.phoenix.repository.crm.smsCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunicationRelatedCustomers;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface SmsCommunicationRelatedCustomersRepository extends JpaRepository<SmsCommunicationRelatedCustomers,Long> {

    Set<SmsCommunicationRelatedCustomers> findAllBySmsCommunicationIdAndStatus(Long smsCommunicationId, EntityStatus entityStatus);
    @Query("""
            select new bg.energo.phoenix.model.response.shared.ShortResponse(
                customer.id,
                case when customer.customerType = 'LEGAL_ENTITY' then concat(customer.identifier, ' (', customerDetails.name, ')')
                    else replace(concat(customer.identifier , ' (',coalesce(customerDetails.name, '') , ' ', coalesce(customerDetails.middleName, '') , ' ', coalesce(customerDetails.lastName, ''), ')'), '\\s+', ' ') end
            )
            from SmsCommunicationRelatedCustomers smsCommunicationRelatedCustomer
            join Customer customer on customer.id = smsCommunicationRelatedCustomer.customerId
            join CustomerDetails customerDetails on customerDetails.id = customer.lastCustomerDetailId
            where smsCommunicationRelatedCustomer.smsCommunicationId = :smsCommunicationId
            and smsCommunicationRelatedCustomer.status='ACTIVE'
            """
    )
    List<ShortResponse> findBySmsCommunicationId(Long smsCommunicationId);
}
