package bg.energo.phoenix.repository.crm.smsCommunication;

import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunicationCustomers;
import bg.energo.phoenix.model.response.crm.smsCommunication.CustomerDetailShortResponse;
import bg.energo.phoenix.model.response.crm.smsCommunication.MassSMSCustomerAndContractResponse;
import bg.energo.phoenix.model.response.crm.smsCommunication.SmsCustomersQueryResponse;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface SmsCommunicationCustomersRepository extends JpaRepository<SmsCommunicationCustomers,Long> {

    Optional<SmsCommunicationCustomers> findBySmsCommunicationId(Long smsCommunicationId);

    @Query("""
            select new bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse(
                communication.id,
                communication.contactTypeName,
                communication.createDate
            )
            from SmsCommunicationCustomers smsCommunicationCustomer
            join CustomerDetails customerDetails on customerDetails.id = smsCommunicationCustomer.customerDetailId
            join CustomerCommunications communication on communication.id = smsCommunicationCustomer.customerCommunicationId
            where smsCommunicationCustomer.id=:smsCommunicationCustomerId
            """
    )
    Optional<CustomerCommunicationDataResponse> getCustomerCommunicationDataBySmsCommunicationCustomerId(Long smsCommunicationCustomerId);


    @Query("""
            select new bg.energo.phoenix.model.response.crm.smsCommunication.CustomerDetailShortResponse(
                customer.id,
                case when customer.customerType = 'LEGAL_ENTITY' then concat(customer.identifier, ' (', customerDetails.name, ')')
                    else replace(concat(customer.identifier , ' (',coalesce(customerDetails.name, '') , ' ', coalesce(customerDetails.middleName, '') , ' ', coalesce(customerDetails.lastName, ''), ')'), '\\s+', ' ') end,
                    customerDetails.id,
                                customerDetails.versionId
            )
            from SmsCommunicationCustomers smsCommunicationCustomer
            join CustomerDetails customerDetails on customerDetails.id = smsCommunicationCustomer.customerDetailId
            join Customer customer on customer.id = customerDetails.customerId
            where smsCommunicationCustomer.id = :smsCommunicationCustomerId
            """
    )
    Optional<CustomerDetailShortResponse> getBySmsCommunicationCustomerId(Long smsCommunicationCustomerId);

    @Query("""
            select distinct new bg.energo.phoenix.model.response.shared.ShortResponse(
                customer.id,
                case when customer.customerType = 'LEGAL_ENTITY' then concat(customer.identifier, ' (', customerDetails.name, ')')
                    else replace(concat(customer.identifier , ' (',coalesce(customerDetails.name, '') , ' ', coalesce(customerDetails.middleName, '') , ' ', coalesce(customerDetails.lastName, ''), ')'), '\\s+', ' ') end
            )
            from SmsCommunicationCustomers smsCommunicationCustomer
            join CustomerDetails customerDetails on customerDetails.id = smsCommunicationCustomer.customerDetailId
            join Customer customer on customer.id = customerDetails.customerId
            where smsCommunicationCustomer.id=:smsCommunicationCustomersId
            """
    )
    Optional<ShortResponse> getBySmsCommunicationCustomersId(Long smsCommunicationCustomersId);

    @Query("""
        select distinct new bg.energo.phoenix.model.response
        .crm.smsCommunication.MassSMSCustomerAndContractResponse(c.id,c.identifier,scc.serviceContractDetailid,scc.productContractDetailId)
        from SmsCommunicationCustomers scc
        join CustomerDetails cd on scc.customerDetailId=cd.id
        join Customer c on c.id=cd.customerId
        join SmsCommunication sc on scc.smsCommunicationId=sc.id
        where scc.smsCommunicationId=:massCommunicationId
         and (
                (sc.communicationStatus <> 'SENT' or scc.customerCommunicationId is not null)
            )
""")
    List<MassSMSCustomerAndContractResponse> getMassCustomers(Long massCommunicationId);

    @Query("""
        select new bg.energo.phoenix.model.response.crm.smsCommunication.SmsCustomersQueryResponse(scc.customerDetailId,scc.customerCommunicationId)
        from SmsCommunicationCustomers scc
        where scc.smsCommunicationId=:smsCommunicationId
""")
    Set<SmsCustomersQueryResponse> findAllSmsCommunicationCustomersBySmsCommunicationId(Long smsCommunicationId);

    @Query("""
        select scc.id from SmsCommunicationCustomers scc
        where scc.smsCommunicationId=:smsCommunicationId
""")
    List<Long> findSmsCommunicationCustomersBySmsCommunicationId(Long smsCommunicationId);

    @Query("""
        select sc, scc from SmsCommunicationCustomers scc
        join SmsCommunication sc on scc.smsCommunicationId=sc.id
        where scc.id=:id
        and sc.communicationChannel='MASS_SMS'
""")
    List<Object[]> findSmsCommunicationBySmsCommunicationCustomerId(Long id);

    @Transactional
    void deleteAllBySmsCommunicationId(Long smsCommunicationId);

    @Query("""
        delete from SmsCommunicationCustomers scc
        where scc.id in :ids
""")
    @Modifying
    void deleteAllByIdIn(List<Long> ids);

    @Query(nativeQuery = true, value = """
                    select * from crm.sms_communication_customers scc
                    where scc.sms_comm_status='IN_PROGRESS'
                      and scc.create_date <= :now
                      and scc.create_date >= CURRENT_TIMESTAMP - INTERVAL '72 hours'
            """)
    List<SmsCommunicationCustomers> findSmsWithInProgressStatus(LocalDateTime now);

    @Query(nativeQuery = true, value = """
                    select * from crm.sms_communication_customers scc
                    where scc.sms_comm_status='TODO'
                      and scc.create_date <= :now
                      and scc.create_date >= CURRENT_TIMESTAMP - INTERVAL '48 hours'
            """)
    List<SmsCommunicationCustomers> findSmsWithToDoStatus(LocalDateTime now);

    @Query("""
        select sc,scc from SmsCommunication sc
        join SmsCommunicationCustomers scc on scc.smsCommunicationId=sc.id
        where sc.smsBody is null and scc.smsBody is null
        and sc.communicationChannel='MASS_SMS'
        and sc.communicationStatus='SENT'
        and sc.status='ACTIVE'
""")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "javax.persistence.lock.timeout", value = "5000")
    })
    List<Object[]> findSmsForBodyGeneration();

    @Modifying
    @Query(nativeQuery = true, value = """
                    update crm.sms_communication_customers
                    set sms_comm_status = 'SEND_FAILED'
                    where (sms_comm_status = 'IN_PROGRESS' and create_date < CURRENT_TIMESTAMP - INTERVAL '72 hours')
                       or (sms_comm_status = 'TODO' and create_date < CURRENT_TIMESTAMP - INTERVAL '48 hours')
            """)
    void updateSmsCommStatusForTimedOutCommunications();

}
