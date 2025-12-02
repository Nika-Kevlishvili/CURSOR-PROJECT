package bg.energo.phoenix.repository.billing.companyDetails;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyBankRepository extends JpaRepository<CompanyBank, Long> {

    List<CompanyBank> findAllByCompanyDetailIdAndStatus(Long companyDetailId, EntityStatus status);
}
