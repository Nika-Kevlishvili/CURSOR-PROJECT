package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractAssistingEmployee;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractSubObjectShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceContractAssistingEmployeeRepository extends JpaRepository<ServiceContractAssistingEmployee, Long> {

    List<ServiceContractAssistingEmployee> findByContractDetailIdAndStatusIn(Long id, List<EntityStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractSubObjectShortResponse(
                        cae.assistingEmployeeId,
                        concat(ac.displayName, ' (', ac.userName, ')')
                    )
                    from ServiceContractAssistingEmployee cae
                    join AccountManager ac on cae.assistingEmployeeId = ac.id
                        where cae.contractDetailId = :contractDetailId
                        and cae.status in :statuses
                    """
    )
    List<ServiceContractSubObjectShortResponse> getShortResponseByContractDetailIdAndStatusIn(
            @Param("contractDetailId") Long contractDetailId,
            @Param("statuses") List<EntityStatus> statuses
    );
    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractSubObjectShortResponse(
                        cae.assistingEmployeeId,
                        concat(ac.displayName, ' (', ac.userName, ')')
                    )
                    from ServiceContractAssistingEmployee cae
                    join AccountManager ac on cae.assistingEmployeeId = ac.id
                        where cae.contractDetailId = :contractDetailId
                        and cae.status in :statuses
                    """
    )
    List<ServiceContractSubObjectShortResponse> getShortResponseByContractDetailIdAndStatusInWithAndAssistingEmployeeId(
            @Param("contractDetailId") Long contractDetailId,
            @Param("statuses") List<EntityStatus> statuses
    );

}