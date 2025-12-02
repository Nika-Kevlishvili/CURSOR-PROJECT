package phoenix.core.customer.repository.customer.communicationData;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import phoenix.core.customer.model.entity.customer.communication.CustomerCommunicationContacts;
import phoenix.core.customer.model.enums.customer.Status;
import phoenix.core.customer.model.response.customer.communicationData.ContactBasicInfo;

import java.util.List;

public interface CustomerCommunicationContactsRepository extends JpaRepository<CustomerCommunicationContacts, Long> {

    @Query(
        """
            select new phoenix.core.customer.model.response.customer.communicationData.ContactBasicInfo(
                ccc.id,
                CONCAT(ccc.contactValue, case
                        when (ccc.contactType = 'MOBILE_NUMBER' and ccc.sendSms is true) then ' (SMS)'
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
}
