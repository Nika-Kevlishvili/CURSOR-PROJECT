package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.contract.service.ServiceTermRenewalCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceTermRenewalCountRepository extends JpaRepository<ServiceTermRenewalCount,Long> {
    @Query("""
        select rc from ServiceTermRenewalCount rc
        where rc.contractDetailId = :contractDetailId
        and rc.serviceContractTermId = :termId
            """)
    Optional<ServiceTermRenewalCount> findByTermIdAndContractDetailId(Long termId, Long contractDetailId);
}
