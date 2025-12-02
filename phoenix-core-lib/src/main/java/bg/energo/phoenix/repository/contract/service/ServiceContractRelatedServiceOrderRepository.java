package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractRelatedServiceOrder;
import bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceContractRelatedServiceOrderRepository extends JpaRepository<ServiceContractRelatedServiceOrder, Long> {

    @Query(
            value = """
                    select scrs from ServiceContractRelatedServiceOrder scrs
                        where (
                            (:type = 'SERVICE_CONTRACT' and scrs.serviceContractId = :objectId)
                            or (:type = 'SERVICE_ORDER' and scrs.serviceOrderId = :objectId)
                        )
                        and scrs.status in :statuses
                    """
    )
    List<ServiceContractRelatedServiceOrder> findByServiceContractIdOrServiceOrderIdAndStatusIn(
            @Param("type") String type,
            @Param("objectId") Long objectId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select scrs from ServiceContractRelatedServiceOrder scrs
                        where scrs.serviceContractId = :serviceContractId
                        and scrs.serviceOrderId = :serviceOrderId
                        and scrs.status in :statuses
                    """
    )
    Optional<ServiceContractRelatedServiceOrder> findByServiceContractIdAndServiceOrderIdAndStatusIn(
            @Param("serviceContractId") Long serviceContractId,
            @Param("serviceOrderId") Long serviceOrderId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse(
                        scrs,
                        so.orderNumber,
                        'SERVICE_ORDER',
                        so.id
                    )
                    from ServiceContractRelatedServiceOrder scrs
                    join ServiceOrder so on so.id = scrs.serviceOrderId
                        where scrs.serviceContractId = :serviceContractId
                        and scrs.status in :statuses
                    """
    )
    List<RelatedEntityResponse> findByServiceContractIdAndStatusIn(
            @Param("serviceContractId") Long serviceContractId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse(
                        scrs,
                        sc.contractNumber,
                        'SERVICE_CONTRACT',
                        sc.id
                    )
                    from ServiceContractRelatedServiceOrder scrs
                    join ServiceContracts sc on sc.id = scrs.serviceContractId
                        where scrs.serviceOrderId = :serviceOrderId
                        and scrs.status in :statuses
                    """
    )
    List<RelatedEntityResponse> findByServiceOrderIdAndStatusIn(
            @Param("serviceOrderId") Long serviceOrderId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
