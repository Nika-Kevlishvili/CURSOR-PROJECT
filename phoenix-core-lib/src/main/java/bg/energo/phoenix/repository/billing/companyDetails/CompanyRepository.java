package bg.energo.phoenix.repository.billing.companyDetails;

import bg.energo.phoenix.model.entity.billing.companyDetails.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
}
