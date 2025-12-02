package bg.energo.phoenix.repository.crm.emailCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationTemplates;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmailCommunicationTemplateRepository extends JpaRepository<EmailCommunicationTemplates,Long> {

    @Query("""
            select pct from EmailCommunicationTemplates pct
            where pct.status='ACTIVE'
            and pct.emailCommId = :objectionToChangeWithdrawalId
            """)
    List<EmailCommunicationTemplates> findByProductDetailId(Long objectionToChangeWithdrawalId);

    @Query("""
            select new bg.energo.phoenix.model.response.template.ContractTemplateShortResponse(ctd.templateId,ctd.id,ctd.name,ctd.templateFileId,ctf.name,text(ctd.outputFileFormat),text(ctd.fileSigning))
            from ContractTemplateDetail ctd
            join ContractTemplateFiles ctf on ctf.id=ctd.templateFileId
            join ContractTemplate ct on ct.id = ctd.templateId
            join EmailCommunicationTemplates pct on pct.templateId=ctd.templateId
            where pct.status='ACTIVE'
            and (ct.status = 'ACTIVE' or ct.status = 'INACTIVE')
            and pct.emailCommId=:emailCommId
            and ctd.startDate = (select max(ctd2.startDate) from ContractTemplateDetail ctd2 where ctd2.templateId = ctd.templateId and ctd2.startDate <=:currentDate)
            """)
    List<ContractTemplateShortResponse> findForContract(Long emailCommId, LocalDate currentDate);

    List<EmailCommunicationTemplates> findAllByEmailCommIdAndStatus(Long emailCommId, EntityStatus status);

}
