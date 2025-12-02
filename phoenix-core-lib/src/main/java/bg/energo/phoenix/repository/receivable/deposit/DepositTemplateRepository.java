package bg.energo.phoenix.repository.receivable.deposit;

import bg.energo.phoenix.model.entity.receivable.deposit.DepositTemplate;
import bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.ReceivableTemplateResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DepositTemplateRepository extends JpaRepository<DepositTemplate, Long> {
    @Query("""
            select new bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.ReceivableTemplateResponse(ctd.templateId,ctd.id,ctd.name,ctd.templateFileId,ctf.name,pct.type,text(ctd.outputFileFormat),text(ctd.fileSigning))
            from ContractTemplateDetail ctd
            join ContractTemplateFiles ctf on ctf.id=ctd.templateFileId
            join ContractTemplate ct on ct.id = ctd.templateId
            join DepositTemplate pct on pct.templateId=ctd.templateId
            where pct.status='ACTIVE'
            and (ct.status = 'ACTIVE' or ct.status = 'INACTIVE')
            and pct.depositId=:depositId
            and ctd.startDate = (select max(ctd2.startDate) from ContractTemplateDetail ctd2 where ctd2.templateId = ctd.templateId and ctd2.startDate <=:currentDate)
            """)
    List<ReceivableTemplateResponse> findForContract(Long depositId, LocalDate currentDate);

    @Query("""
            select pct from DepositTemplate pct
            where pct.status='ACTIVE'
            and pct.depositId = :depositId
            """)
    List<DepositTemplate> findByDepositId(Long depositId);
}
