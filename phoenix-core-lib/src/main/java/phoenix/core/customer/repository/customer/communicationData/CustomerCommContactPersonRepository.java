package phoenix.core.customer.repository.customer.communicationData;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import phoenix.core.customer.model.entity.customer.communication.CustomerCommContactPerson;
import phoenix.core.customer.model.enums.customer.Status;
import phoenix.core.customer.model.response.customer.communicationData.ContactPersonBasicInfo;
import phoenix.core.customer.model.response.customer.communicationData.ContactPersonResponse;

import java.util.List;
import java.util.Optional;

public interface CustomerCommContactPersonRepository extends JpaRepository<CustomerCommContactPerson, Long> {

    @Query(
            """
                select new phoenix.core.customer.model.response.customer.communicationData.ContactPersonBasicInfo(cccp)
                from CustomerCommContactPerson as cccp
                    where cccp.customerCommunicationsId = :customerCommId
                    and cccp.status in :statuses
                    order by cccp.createDate asc
            """
    )
    List<ContactPersonBasicInfo> getBasicInfoByCustomerCommIdAndStatuses(
            @Param("customerCommId") Long customerCommId,
            @Param("statuses") List<Status> statuses
    );

    @Query(
            """
                select new phoenix.core.customer.model.response.customer.communicationData.ContactPersonResponse(
                    cccp.id,
                    t.id,
                    t.name,
                    cccp.name,
                    cccp.middleName,
                    cccp.surname,
                    cccp.jobPosition,
                    cccp.positionHeldFrom,
                    cccp.positionHeldTo,
                    cccp.birthDate,
                    cccp.additionalInfo,
                    cccp.status
                )
                from CustomerCommContactPerson as cccp
                left join Title as t on t.id = cccp.titleId
                    where cccp.id = :id
                    and cccp.status in :statuses
            """
    )
    Optional<ContactPersonResponse> getDetailedContactPersonByCustomerCommIdAndStatuses(
            @Param("id") Long id,
            @Param("statuses") List<Status> statuses
    );

    @Query(
        """
            select cccp from CustomerCommContactPerson as cccp
                where cccp.customerCommunicationsId = :customerCommunicationsId
                and cccp.status in :statuses
        """
    )
    List<CustomerCommContactPerson> findByCustomerCommIdAndStatuses(
            @Param("customerCommunicationsId") Long customerCommunicationsId,
            @Param("statuses") List<Status> statuses
    );

    @Query(
        """
            select cccp from CustomerCommContactPerson as cccp
                where cccp.id = :id
                and cccp.status in :statuses
        """
    )
    Optional<CustomerCommContactPerson> findByIdAndStatuses(
            @Param("id") Long id,
            @Param("statuses") List<Status> statuses
    );
}
