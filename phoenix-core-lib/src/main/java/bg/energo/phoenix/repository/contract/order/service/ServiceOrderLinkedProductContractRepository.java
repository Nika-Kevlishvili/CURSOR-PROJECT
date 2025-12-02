package bg.energo.phoenix.repository.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderLinkedProductContract;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderLinkedContractShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceOrderLinkedProductContractRepository extends JpaRepository<ServiceOrderLinkedProductContract, Long> {


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.order.service.ServiceOrderLinkedContractShortResponse(
                        solpc.contractId,
                        pc.contractNumber,
                        solpc.createDate,
                        'PRODUCT_CONTRACT'
                    )
                    from ServiceOrderLinkedProductContract solpc
                    join ProductContract pc on solpc.contractId = pc.id
                        where solpc.orderId = :id
                        and solpc.status in :statuses
                    """
    )
    List<ServiceOrderLinkedContractShortResponse> findByServiceOrderIdAndStatusIn(
            @Param("id") Long id,
            @Param("statuses") List<EntityStatus> statuses
    );


    List<ServiceOrderLinkedProductContract> findByOrderIdAndStatusIn(Long orderId, List<EntityStatus> statuses);

}
