package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractRelatedServiceContract;
import bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceContractRelatedServiceContractRepository extends JpaRepository<ServiceContractRelatedServiceContract,Long> {

    @Query(
            value = """
                    select scrsc from ServiceContractRelatedServiceContract scrsc
                        where (
                            (scrsc.serviceContractId = :contractId and scrsc.relatedServiceContractId = :relatedContractId)
                            or (scrsc.serviceContractId = :relatedContractId and scrsc.relatedServiceContractId = :contractId)
                        )
                        and scrsc.status in :statuses
                    """
    )
    Optional<ServiceContractRelatedServiceContract> findByContractIdAndRelatedContractIdAndStatusIn(
            @Param("contractId") Long contractId,
            @Param("relatedContractId") Long relatedContractId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select scrsc from ServiceContractRelatedServiceContract scrsc
                        where (scrsc.serviceContractId = :contractId or scrsc.relatedServiceContractId = :contractId)
                        and scrsc.status in :statuses
                    """
    )
    List<ServiceContractRelatedServiceContract> findByContractIdAndStatusIn(
            @Param("contractId") Long contractId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse(
                        scrsc,
                        sc.contractNumber,
                        'SERVICE_CONTRACT',
                        case when scrsc.serviceContractId = :contractId
                            then scrsc.relatedServiceContractId
                            else scrsc.serviceContractId
                            end
                    )
                    from ServiceContractRelatedServiceContract scrsc
                    join ServiceContracts sc on
                        case when scrsc.serviceContractId = :contractId
                            then scrsc.relatedServiceContractId
                            else scrsc.serviceContractId
                            end = sc.id
                        where (scrsc.serviceContractId = :contractId or scrsc.relatedServiceContractId = :contractId)
                        and scrsc.status in :statuses
                    """
    )
    List<RelatedEntityResponse> findByServiceContractIdAndStatusIn(
            @Param("contractId") Long contractId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
