package bg.energo.phoenix.repository.receivable.cancellationOfDisconnectionOfThePowerSupply;

import bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationDcnTemplate;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CancellationOfDcnTemplateRepository extends JpaRepository<CancellationDcnTemplate,Long> {
    @Query("""
            select pct from CancellationDcnTemplate pct
            where pct.status='ACTIVE'
            and pct.powerSupplyDcnId = :powerSupplyDcnId
            """)
    List<CancellationDcnTemplate> findByProductDetailId(Long powerSupplyDcnId);

    @Query("""
            select new bg.energo.phoenix.model.response.template.ContractTemplateShortResponse(ctd.templateId,ctd.id,ctd.name,ctd.templateFileId,ctf.name,text(ctd.outputFileFormat),text(ctd.fileSigning))
            from ContractTemplateDetail ctd
            join ContractTemplateFiles ctf on ctf.id=ctd.templateFileId
            join ContractTemplate ct on ct.id = ctd.templateId
            join CancellationDcnTemplate pct on pct.templateId=ctd.templateId
            where pct.status='ACTIVE'
            and (ct.status = 'ACTIVE' or ct.status = 'INACTIVE')
            and pct.powerSupplyDcnId=:powerSupplyDcnId
            and ctd.startDate = (select max(ctd2.startDate) from ContractTemplateDetail ctd2 where ctd2.templateId = ctd.templateId and ctd2.startDate <=:currentDate)
            """)
    List<ContractTemplateShortResponse> findForContract(Long powerSupplyDcnId, LocalDate currentDate);
}
