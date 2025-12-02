package bg.energo.phoenix.repository.billing.companyDetails;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyEmailRepository extends JpaRepository<CompanyEmail, Long> {

    List<CompanyEmail> findAllByCompanyDetailIdAndStatus(Long companyDetailId, EntityStatus status);
}
