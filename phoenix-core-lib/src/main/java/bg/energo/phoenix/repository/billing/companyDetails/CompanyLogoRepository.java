package bg.energo.phoenix.repository.billing.companyDetails;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyLogos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyLogoRepository extends JpaRepository<CompanyLogos, Long> {
    @Query("""
                    Select cl from CompanyLogos cl where cl.companyDetailId is null and cl.status='ACTIVE'
            """)
    List<CompanyLogos> findActiveByCompanyDetailIdNull();

    Optional<CompanyLogos> findFirstByCompanyDetailIdAndStatus(Long companyDetailId, EntityStatus status);

    boolean existsCompanyLogosByIdAndCompanyDetailIdAndStatus(Long logoId, Long companyDetailId, EntityStatus status);
}
