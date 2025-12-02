package bg.energo.phoenix.repository.billing.companyDetails;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyInvoiceCompiler;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyInvoiceCompilerRepository extends JpaRepository<CompanyInvoiceCompiler, Long> {

    List<CompanyInvoiceCompiler> findAllByCompanyDetailIdAndStatus(Long companyDetailId, EntityStatus status);

}
