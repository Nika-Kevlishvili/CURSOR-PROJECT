package phoenix.core.customer.repository.customer.communicationData;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import phoenix.core.customer.model.entity.customer.communication.CustomerCommContactPurposes;
import phoenix.core.customer.model.enums.customer.Status;
import phoenix.core.customer.model.response.customer.communicationData.ContactPurposeBasicInfo;
import phoenix.core.customer.model.response.customer.communicationData.ContactPurposeResponse;

import java.util.List;

public interface CustomerCommContactPurposesRepository extends JpaRepository<CustomerCommContactPurposes, Long> {

    @Query(
        """
            select new phoenix.core.customer.model.response.customer.communicationData.ContactPurposeBasicInfo(
                cccp.id,
                cp.id,
                cp.name
            )
            from CustomerCommContactPurposes as cccp
            left join ContactPurpose as cp on cccp.contactPurposeId = cp.id
                where cccp.customerCommunicationsId = :customerCommId
                and cccp.status in :statuses
                order by cccp.createDate asc
        """
    )
    List<ContactPurposeBasicInfo> getBasicInfoByCustomerCommIdAndStatuses(
            @Param("customerCommId") Long customerCommId,
            @Param("statuses") List<Status> statuses
    );

    @Query(
        """
            select new phoenix.core.customer.model.response.customer.communicationData.ContactPurposeResponse(
                cccp.customerCommunicationsId,
                cp.name
            )
            from CustomerCommContactPurposes as cccp
            left join ContactPurpose as cp on cccp.contactPurposeId = cp.id
                where cccp.customerCommunicationsId in :customerCommIds
                and cccp.status in :statuses
                order by cccp.createDate asc
        """
    )
    List<ContactPurposeResponse> getContactPurposesByCommunicationDataIdsAndStatuses(
            @Param("customerCommIds") List<Long> customerCommIds,
            @Param("statuses") List<Status> statuses
    );

    @Query(
        """
            select cccp from CustomerCommContactPurposes cccp
                where cccp.customerCommunicationsId = :customerCommunicationsId
                and cccp.status in :statuses
        """
    )
    List<CustomerCommContactPurposes> findByCustomerCommId(
            @Param("customerCommunicationsId") Long customerCommunicationsId,
            @Param("statuses") List<Status> statuses
    );
}
