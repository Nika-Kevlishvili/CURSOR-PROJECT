package bg.energo.phoenix.repository.billing.companyDetails;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyManagerRepository extends JpaRepository<CompanyManager, Long> {

    List<CompanyManager> findAllByCompanyDetailIdAndStatus(Long companyDetailId, EntityStatus status);

}
