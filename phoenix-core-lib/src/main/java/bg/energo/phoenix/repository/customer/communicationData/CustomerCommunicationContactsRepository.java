package bg.energo.phoenix.repository.customer.communicationData;

import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.response.customer.communicationData.ContactBasicInfo;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.ContactDetailedResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerCommunicationContactsRepository extends JpaRepository<CustomerCommunicationContacts, Long> {

    @Query(
            """
                        select new bg.energo.phoenix.model.response.customer.communicationData.ContactBasicInfo(
                            ccc.id,
                            CONCAT(ccc.contactValue, case
                                    when (ccc.contactType = 'MOBILE_NUMBER' and ccc.sendSms = true) then ' (SMS)'
                                    when (ccc.contactType = 'OTHER_PLATFORM' and ccc.platformId is not null) then CONCAT(' (', p.name, ')') 
                                    else '' end),
                            ccc.contactType
                        )
                        from CustomerCommunicationContacts as ccc
                        left join Platform as p on ccc.platformId = p.id
                            where ccc.customerCommunicationsId = :customerCommId
                            and ccc.status in :statuses
                        order by ccc.createDate asc
                    """
    )
    List<ContactBasicInfo> getBasicInfoByCustomerCommIdAndStatuses(
            @Param("customerCommId") Long customerCommId,
            @Param("statuses") List<Status> statuses
    );

    @Query(
            """
                        select ccc from CustomerCommunicationContacts as ccc
                            where ccc.customerCommunicationsId = :customerCommunicationsId
                            and ccc.status in :statuses
                    """
    )
    List<CustomerCommunicationContacts> findByCustomerCommIdAndStatuses(
            @Param("customerCommunicationsId") Long customerCommunicationsId,
            @Param("statuses") List<Status> statuses
    );

    @Query(
            """
                        select ccc from CustomerCommunicationContacts as ccc
                            where ccc.customerCommunicationsId = :customerCommunicationsId
                            and ccc.status in :statuses
                            and ccc.contactType in (:contactTypes)
                    """
    )
    List<CustomerCommunicationContacts> findByCustomerCommIdContactTypesAndStatuses(
            @Param("customerCommunicationsId") Long customerCommunicationsId,
            @Param("contactTypes") List<CustomerCommContactTypes> contactTypes,
            @Param("statuses") List<Status> statuses
    );

    @Query("""
                    select ccc from CustomerCommunicationContacts ccc
                    where ccc.customerCommunicationsId = :customerCommunicationsId
                    and ccc.status='ACTIVE'
                    and ccc.contactType='MOBILE_NUMBER'
                    and ccc.sendSms=true
            """)
    Optional<CustomerCommunicationContacts> findMobileContactByCommunicationId(@Param("customerCommunicationsId") Long customerCommunicationsId);

    @Query(
            """
                        select new bg.energo.phoenix.model.response.customer.communicationData.detailed.ContactDetailedResponse(
                            ccc.id,
                            ccc.sendSms,
                            p.id,
                            p.name,
                            ccc.status,
                            ccc.contactType,
                            ccc.contactValue,
                            ccc.customerCommunicationsId
                        )
                        from CustomerCommunicationContacts ccc 
                        left join Platform p on p.id = ccc.platformId
                            where ccc.customerCommunicationsId in :customerCommIds
                            and ccc.status in :statuses
                            and ccc.contactType in (:contactTypes)
                    """
    )
    List<ContactDetailedResponse> findByCustomerCommIds(
            @Param("customerCommIds") List<Long> customerCommIds,
            @Param("statuses") List<Status> statuses,
            @Param("contactTypes") List<CustomerCommContactTypes> contactTypes
    );
}
