package bg.energo.phoenix.repository.crm.emailCommunication;

import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationCustomer;
import bg.energo.phoenix.model.response.crm.emailCommunication.EmailConnectedCustomerShortResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.Optional;

public interface EmailCommunicationCustomerRepository extends JpaRepository<EmailCommunicationCustomer, Long> {
    @Query("""
            select new bg.energo.phoenix.model.response.crm.emailCommunication.EmailConnectedCustomerShortResponse(
                customer.id,
                customerDetails.id,
                customerDetails.versionId,
                case when customer.customerType = 'LEGAL_ENTITY' then concat(customer.identifier, ' (', customerDetails.name, ')')
                    else replace(concat(customer.identifier , ' (',coalesce(customerDetails.name, '') , ' ', coalesce(customerDetails.middleName, '') , ' ', coalesce(customerDetails.lastName, ''), ')'), '\\s+', ' ') end
            )
            from EmailCommunicationCustomer emailCommunicationCustomer
            join CustomerDetails customerDetails on customerDetails.id = emailCommunicationCustomer.customerDetailId
            join Customer customer on customer.id = customerDetails.customerId
            where emailCommunicationCustomer.emailCommunicationId = :emailCommunicationId
            """
    )
    Optional<EmailConnectedCustomerShortResponse> getByEmailCommunicationId(Long emailCommunicationId);

    @Query("""
            select new bg.energo.phoenix.model.response.crm.emailCommunication.EmailConnectedCustomerShortResponse(
                customer.id,
                customerDetails.id,
                customerDetails.versionId,
                case when customer.customerType = 'LEGAL_ENTITY' then concat(customer.identifier, ' (', customerDetails.name, ')')
                    else replace(concat(customer.identifier , ' (',coalesce(customerDetails.name, '') , ' ', coalesce(customerDetails.middleName, '') , ' ', coalesce(customerDetails.lastName, ''), ')'), '\\s+', ' ') end
            )
            from EmailCommunicationCustomer emailCommunicationCustomer
            join CustomerDetails customerDetails on customerDetails.id = emailCommunicationCustomer.customerDetailId
            join Customer customer on customer.id = customerDetails.customerId
            where emailCommunicationCustomer.id = :emailCommunicationCustomerId
            """
    )
    Optional<EmailConnectedCustomerShortResponse> getByEmailCommunicationCustomerId(Long emailCommunicationCustomerId);

    @Query("""
            select new bg.energo.phoenix.model.response.shared.ShortResponse(
                communication.id,
                communication.contactTypeName
            )
            from EmailCommunicationCustomer emailCommunicationCustomer
            join CustomerDetails customerDetails on customerDetails.id = emailCommunicationCustomer.customerDetailId
            join CustomerCommunications communication on communication.id = emailCommunicationCustomer.customerCommunicationId
            where emailCommunicationCustomer.emailCommunicationId = :emailCommunicationId
            """
    )
    Optional<ShortResponse> getCustomerCommunicationDataByEmailCommunicationId(Long emailCommunicationId);

    @Query("""
            select new bg.energo.phoenix.model.response.shared.ShortResponse(
                communication.id,
                communication.contactTypeName
            )
            from EmailCommunicationCustomer emailCommunicationCustomer
            join CustomerDetails customerDetails on customerDetails.id = emailCommunicationCustomer.customerDetailId
            join CustomerCommunications communication on communication.id = emailCommunicationCustomer.customerCommunicationId
            where emailCommunicationCustomer.id = :emailCommunicationCustomerId
            """
    )
    Optional<ShortResponse> getCustomerCommunicationDataByEmailCommunicationCustomerId(Long emailCommunicationCustomerId);

    Optional<EmailCommunicationCustomer> findByEmailCommunicationId(Long emailCommunicationId);

    void deleteAllByEmailCommunicationId(Long emailCommunicationId);

    boolean existsByEmailCommunicationId(Long emailCommunicationId);

    @Query("""
            select distinct new bg.energo.phoenix.model.response.shared.ShortResponse(
                    c.id,
                    c.identifier
            )
            from EmailCommunicationCustomer ecc
            join CustomerDetails cd on ecc.customerDetailId = cd.id
            join Customer c on c.id = cd.customerId
            where ecc.emailCommunicationId = :massCommunicationId
             """
    )
    List<ShortResponse> getMassEmailCustomers(Long massCommunicationId);

    @Query("""
            select ec,ecc from EmailCommunication ec
            join EmailCommunicationCustomer ecc on ecc.emailCommunicationId = ec.id
            where ec.emailBody is null and ecc.emailBody is null
            and ec.communicationChannel='MASS_EMAIL'
            and ec.emailCommunicationStatus ='SENT'
            and ec.entityStatus ='ACTIVE'
            """
    )
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "5000")})
    List<Object[]> findCustomersForBodyGeneration();

    @Query("""
            select 
              pc.id,
              pc.contractNumber 
            from ProductContractDetails pcd
            join ProductContract pc on pcd.contractId = pc.id
            where pcd.id = :detailId
            """
    )
    List<Object[]> fetchProductContractNumberByDetailId(Long detailId);

    @Query("""
            select 
              sc.id,
              sc.contractNumber 
            from ServiceContractDetails scd
            join ServiceContracts sc on scd.contractId = sc.id
            where scd.id = :detailId
            """
    )
    List<Object[]> fetchServiceContractNumberByDetailId(Long detailId);

    List<EmailCommunicationCustomer> findAllByEmailCommunicationId(Long emailCommunicationId);

}
