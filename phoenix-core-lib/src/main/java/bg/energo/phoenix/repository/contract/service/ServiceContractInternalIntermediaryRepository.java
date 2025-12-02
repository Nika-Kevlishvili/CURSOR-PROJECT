package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractInternalIntermediary;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractSubObjectShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceContractInternalIntermediaryRepository extends JpaRepository<ServiceContractInternalIntermediary, Long> {

    List<ServiceContractInternalIntermediary> findByContractDetailIdAndStatusIn(Long id, List<EntityStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractSubObjectShortResponse(
                        cii.internalIntermediaryId,
                        concat(ac.displayName, ' (', ac.userName, ')')
                    )
                    from ServiceContractInternalIntermediary cii
                    join AccountManager ac on cii.internalIntermediaryId = ac.id
                        where cii.contractDetailId = :contractDetailId
                        and cii.status in :statuses
                    """
    )
    List<ServiceContractSubObjectShortResponse> getShortResponseByContractDetailIdAndStatusIn(
            @Param("contractDetailId") Long contractDetailId,
            @Param("statuses") List<EntityStatus> statuses
    );
    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractSubObjectShortResponse(
                        cii.internalIntermediaryId,
                        concat(ac.displayName, ' (', ac.userName, ')')
                    )
                    from ServiceContractInternalIntermediary cii
                    join AccountManager ac on cii.internalIntermediaryId = ac.id
                        where cii.contractDetailId = :contractDetailId
                        and cii.status in :statuses
                    """
    )
    List<ServiceContractSubObjectShortResponse> getShortResponseByContractDetailIdAndStatusInWithInternalIntermediaryId(
            @Param("contractDetailId") Long contractDetailId,
            @Param("statuses") List<EntityStatus> statuses
    );

}