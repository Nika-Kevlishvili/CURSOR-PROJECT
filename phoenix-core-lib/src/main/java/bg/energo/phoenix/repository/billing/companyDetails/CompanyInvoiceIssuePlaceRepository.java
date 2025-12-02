package bg.energo.phoenix.repository.billing.companyDetails;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyInvoiceIssuePlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyInvoiceIssuePlaceRepository extends JpaRepository<CompanyInvoiceIssuePlace, Long> {
    List<CompanyInvoiceIssuePlace> findAllByCompanyDetailIdAndStatus(Long companyDetailId, EntityStatus status);

}
