package bg.energo.phoenix.repository.product.service;

import bg.energo.phoenix.model.entity.product.service.ServiceAdditionalParams;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractAdditionalParamsResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceAdditionalParamsRepository extends JpaRepository<ServiceAdditionalParams, Long> {

    List<ServiceAdditionalParams> findServiceAdditionalParamsByServiceDetailId(Long serviceDetailId);

    @Query("""
            select new bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractAdditionalParamsResponse(
                serAdPar.id,
                serAdPar.orderingId,
                serAdPar.label,
                serAdPar.value
            )
            from ServiceAdditionalParams as serAdPar
            where serAdPar.serviceDetailId = :serviceDetailId
            and serAdPar.label is not null
            """)
    List<ServiceContractAdditionalParamsResponse> findServiceFilledAdditionalParamsByServiceDetailId(@Param("serviceDetailId") Long serviceDetailId);

}
