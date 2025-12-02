package bg.energo.phoenix.repository.billing.accountingPeriods.files;

import bg.energo.phoenix.model.entity.billing.accountingPeriod.files.AccountPeriodSapReport;
import bg.energo.phoenix.model.response.billing.accountingPeriods.AccountPeriodFileResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountingPeriodSapReportRepository extends JpaRepository<AccountPeriodSapReport, Long> {
    @Modifying
    void deleteAllByAccountPeriodId(Long id);
    @Query("""
            select new bg.energo.phoenix.model.response.billing.accountingPeriods.AccountPeriodFileResponse(
                r.id,
                r.fileUrl,
                r.name,
                'SAP_EXCEL'
            )
            from AccountPeriodSapReport r
            where r.accountPeriodId = :id
            and r.status = 'ACTIVE'
            union
            select new bg.energo.phoenix.model.response.billing.accountingPeriods.AccountPeriodFileResponse(
                r.id,
                r.fileUrl,
                r.name,
                'VAT_DAIRY'
            )
            from AccountPeriodVatDairyReport r
            where r.accountPeriodId = :id
            and r.status = 'ACTIVE'
            """)
    List<AccountPeriodFileResponse> findByAccountPeriodId(Long id);
}
