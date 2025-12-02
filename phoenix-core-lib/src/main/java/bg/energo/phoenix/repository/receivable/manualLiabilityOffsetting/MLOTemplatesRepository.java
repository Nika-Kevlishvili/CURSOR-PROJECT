package bg.energo.phoenix.repository.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.MLOTemplates;
import bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.ReceivableTemplateResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MLOTemplatesRepository extends JpaRepository<MLOTemplates, Long> {

    @Query("""
            select new bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.ReceivableTemplateResponse(ctd.templateId,ctd.id,ctd.name,ctd.templateFileId,ctf.name,pct.type,text(ctd.outputFileFormat),text(ctd.fileSigning))
            from ContractTemplateDetail ctd
            join ContractTemplateFiles ctf on ctf.id=ctd.templateFileId
            join ContractTemplate ct on ct.id = ctd.templateId
            join MLOTemplates pct on pct.templateId=ctd.templateId
            where pct.status='ACTIVE'
            and (ct.status = 'ACTIVE' or ct.status = 'INACTIVE')
            and pct.mloId=:mloId
            and ctd.startDate = (select max(ctd2.startDate) from ContractTemplateDetail ctd2 where ctd2.templateId = ctd.templateId and ctd2.startDate <=:currentDate)
            """)
    List<ReceivableTemplateResponse> findForContract(Long mloId, LocalDate currentDate);

    @Query("""
        select mt,t,ctd from MLOTemplates mt
        join ManualLiabilityOffsetting mlo on mlo.id=mt.mloId
        join ContractTemplate t on t.id=mt.templateId
        join ContractTemplateDetail ctd on ctd.id=t.lastTemplateDetailId
        where mlo.id=:mloId
""")
    List<Object[]> findByMloId(Long mloId);
}
