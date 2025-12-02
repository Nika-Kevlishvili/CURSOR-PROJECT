package bg.energo.phoenix.repository.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderLinkedServiceContract;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderLinkedContractShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceOrderLinkedServiceContractRepository extends JpaRepository<ServiceOrderLinkedServiceContract, Long> {

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.order.service.ServiceOrderLinkedContractShortResponse(
                        solsc.contractId,
                        sc.contractNumber,
                        solsc.createDate,
                        'SERVICE_CONTRACT'
                    )
                    from ServiceOrderLinkedServiceContract solsc
                    join ServiceContracts sc on sc.id = solsc.contractId
                        where solsc.orderId = :id
                        and solsc.status in :statuses
                    """
    )
    List<ServiceOrderLinkedContractShortResponse> findByServiceOrderIdAndStatusIn(
            @Param("id") Long id,
            @Param("statuses") List<EntityStatus> statuses
    );


    List<ServiceOrderLinkedServiceContract> findByOrderIdAndStatusIn(Long orderId, List<EntityStatus> statuses);

}
