package bg.energo.phoenix.repository.customer.communicationData;

import bg.energo.phoenix.model.entity.customer.communication.CustomerCommContactPurposes;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.response.customer.communicationData.ContactPurposeBasicInfo;
import bg.energo.phoenix.model.response.customer.communicationData.ContactPurposeResponse;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.ContactPurposeDetailedResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerCommContactPurposesRepository extends JpaRepository<CustomerCommContactPurposes, Long> {

    @Query(
        """
            select new bg.energo.phoenix.model.response.customer.communicationData.ContactPurposeBasicInfo(
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
            select new bg.energo.phoenix.model.response.customer.communicationData.ContactPurposeResponse(
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

    @Query(
        """
            select new bg.energo.phoenix.model.response.customer.communicationData.detailed.ContactPurposeDetailedResponse(
                cccp.id,
                cp.id,
                cp.name,
                cccp.customerCommunicationsId,
                cccp.status
            ) 
            from CustomerCommContactPurposes as cccp
            left join ContactPurpose cp on cp.id = cccp.contactPurposeId
                where cccp.customerCommunicationsId in :customerCommIds
                and cccp.status in :statuses
        """
    )
    List<ContactPurposeDetailedResponse> findByCustomerCommIds(
            @Param("customerCommIds") List<Long> customerCommIds,
            @Param("statuses") List<Status> statuses
    );
}
