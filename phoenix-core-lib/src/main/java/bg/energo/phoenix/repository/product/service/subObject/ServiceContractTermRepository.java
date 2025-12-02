package bg.energo.phoenix.repository.product.service.subObject;

import bg.energo.phoenix.model.entity.product.service.ServiceContractTerm;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.response.service.ContractTermNameResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceContractTermRepository extends JpaRepository<ServiceContractTerm, Long> {

    @Query(
            value = """
                    select distinct new bg.energo.phoenix.model.response.service.ContractTermNameResponse(
                        sct.name
                    )
                    from ServiceContractTerm sct
                        where sct.status in :statuses
                        and sct.serviceDetails.service.status = 'ACTIVE'
                        and (:prompt is null or lower(sct.name) like :prompt)
                    """
    )
    Page<ContractTermNameResponse> findDistinctNameByStatusIn(
            @Param("statuses") List<ServiceSubobjectStatus> status,
            @Param("prompt") String prompt,
            Pageable pageable
    );


    List<ServiceContractTerm> findByServiceDetailsIdAndStatusIn(Long serviceDetailsId, List<ServiceSubobjectStatus> statuses);
    List<ServiceContractTerm> findByServiceDetailsIdAndStatusInOrderByCreateDate(Long serviceDetailsId, List<ServiceSubobjectStatus> statuses);

}
