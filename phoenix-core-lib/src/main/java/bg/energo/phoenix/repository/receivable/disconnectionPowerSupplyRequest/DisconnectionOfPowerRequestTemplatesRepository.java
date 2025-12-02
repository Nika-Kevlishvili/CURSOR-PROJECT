package bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest;

import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionOfPowerSupplyRequestTemplate;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DisconnectionOfPowerRequestTemplatesRepository extends JpaRepository<DisconnectionOfPowerSupplyRequestTemplate,Long> {
    @Query("""
            select pct from DisconnectionOfPowerSupplyRequestTemplate pct
            where pct.status='ACTIVE'
            and pct.disconnectionId = :disconnectionId
            """)
    List<DisconnectionOfPowerSupplyRequestTemplate> findByProductDetailId(Long disconnectionId);

    @Query("""
            select new bg.energo.phoenix.model.response.template.ContractTemplateShortResponse(ctd.templateId,ctd.id,ctd.name,ctd.templateFileId,ctf.name,text(ctd.outputFileFormat),text(ctd.fileSigning))
            from ContractTemplateDetail ctd
            join ContractTemplateFiles ctf on ctf.id=ctd.templateFileId
            join ContractTemplate ct on ct.id = ctd.templateId
            join DisconnectionOfPowerSupplyRequestTemplate pct on pct.templateId=ctd.templateId
            where pct.status='ACTIVE'
            and (ct.status = 'ACTIVE' or ct.status = 'INACTIVE')
            and pct.disconnectionId=:disconnectionId
            and ctd.startDate = (select max(ctd2.startDate) from ContractTemplateDetail ctd2 where ctd2.templateId = ctd.templateId and ctd2.startDate <=:currentDate)
            """)
    List<ContractTemplateShortResponse> findForContract(Long disconnectionId, LocalDate currentDate);

    List<DisconnectionOfPowerSupplyRequestTemplate> findByDisconnectionId(Long disconnectionId);
}
