package bg.energo.phoenix.repository.receivable.reconnectionOfPowerSupply;

import bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply.ReconnectionPowerSupplyTemplates;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReconnectionOfPSTemplatesRepository extends JpaRepository<ReconnectionPowerSupplyTemplates,Long> {
    @Query("""
            select pct from ReconnectionPowerSupplyTemplates pct
            where pct.status='ACTIVE'
            and pct.powerSupplyReconnectionId = :powerSupplyReconnectionId
            """)
    List<ReconnectionPowerSupplyTemplates> findByProductDetailId(Long powerSupplyReconnectionId);

    @Query("""
            select new bg.energo.phoenix.model.response.template.ContractTemplateShortResponse(ctd.templateId,ctd.id,ctd.name,ctd.templateFileId,ctf.name,text(ctd.outputFileFormat),text(ctd.fileSigning))
            from ContractTemplateDetail ctd
            join ContractTemplateFiles ctf on ctf.id=ctd.templateFileId
            join ContractTemplate ct on ct.id = ctd.templateId
            join ReconnectionPowerSupplyTemplates pct on pct.templateId=ctd.templateId
            where pct.status='ACTIVE'
            and (ct.status = 'ACTIVE' or ct.status = 'INACTIVE')
            and pct.powerSupplyReconnectionId=:powerSupplyReconnectionId
            and ctd.startDate = (select max(ctd2.startDate) from ContractTemplateDetail ctd2 where ctd2.templateId = ctd.templateId and ctd2.startDate <=:currentDate)
            """)
    List<ContractTemplateShortResponse> findForContract(Long powerSupplyReconnectionId, LocalDate currentDate);

    List<ReconnectionPowerSupplyTemplates> findByPowerSupplyReconnectionId(Long reconnectionId);
}
