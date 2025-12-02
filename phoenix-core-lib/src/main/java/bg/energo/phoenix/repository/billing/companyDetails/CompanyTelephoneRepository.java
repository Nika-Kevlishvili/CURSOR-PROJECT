package bg.energo.phoenix.repository.billing.companyDetails;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyTelephone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyTelephoneRepository extends JpaRepository<CompanyTelephone, Long> {

    List<CompanyTelephone> findAllByCompanyDetailIdAndStatus(Long companyDetailId, EntityStatus status);

}
