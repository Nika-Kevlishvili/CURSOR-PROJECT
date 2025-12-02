package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractExternalIntermediary;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractSubObjectShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceContractExternalIntermediaryRepository extends JpaRepository<ServiceContractExternalIntermediary, Long> {

    List<ServiceContractExternalIntermediary> findByContractDetailIdAndStatusIn(Long id, List<EntityStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractSubObjectShortResponse(
                        cei.externalIntermediaryId,
                        concat(ei.name, ' (', ei.identifier, ')')
                    )
                    from ServiceContractExternalIntermediary cei
                    join ExternalIntermediary ei on cei.externalIntermediaryId = ei.id
                        where cei.contractDetailId = :contractDetailId
                        and cei.status in :statuses
                    """
    )
    List<ServiceContractSubObjectShortResponse> getShortResponseByContractDetailIdAndStatusIn(
            @Param("contractDetailId") Long contractDetailId,
            @Param("statuses") List<EntityStatus> statuses
    );

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractSubObjectShortResponse(
                        cei.externalIntermediaryId,
                        concat(ei.name, ' (', ei.identifier, ')')
                    )
                    from ServiceContractExternalIntermediary cei
                    join ExternalIntermediary ei on cei.externalIntermediaryId = ei.id
                        where cei.contractDetailId = :contractDetailId
                        and cei.status in :statuses
                    """
    )
    List<ServiceContractSubObjectShortResponse> getShortResponseByContractDetailIdAndStatusInWithExternalIntermediaryId(
            @Param("contractDetailId") Long contractDetailId,
            @Param("statuses") List<EntityStatus> statuses
    );

}