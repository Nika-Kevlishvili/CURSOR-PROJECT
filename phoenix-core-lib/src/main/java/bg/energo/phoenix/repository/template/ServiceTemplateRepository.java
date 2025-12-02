package bg.energo.phoenix.repository.template;

import bg.energo.phoenix.model.entity.template.ServiceTemplate;
import bg.energo.phoenix.model.response.contract.DocumentGenerationPopupMiddleResponse;
import bg.energo.phoenix.model.response.product.ProductServiceTemplateShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Repository
public interface ServiceTemplateRepository extends JpaRepository<ServiceTemplate, Long> {

    @Query("""
            select sct from ServiceTemplate sct
            where sct.status='ACTIVE'
            and sct.serviceDetailId = :serviceDetailId
            """)
    List<ServiceTemplate> findByServiceDetailId(Long serviceDetailId);

    @Query("""
            select new bg.energo.phoenix.model.response.product.ProductServiceTemplateShortResponse(ctd.templateId,ctd.id,ctd.name,ctd.templateFileId,ctf.name,sct.type,text(ctd.outputFileFormat),text(ctd.fileSigning))
            from ContractTemplateDetail ctd
            join ContractTemplateFiles ctf on ctf.id=ctd.templateFileId
            join ContractTemplate ct on ct.id = ctd.templateId
            join ServiceTemplate sct on sct.templateId=ctd.templateId
            where sct.status='ACTIVE'
            and (ct.status = 'ACTIVE' or ct.status = 'INACTIVE')
            and sct.serviceDetailId=:serviceDetailId
            and ctd.startDate = (select max(ctd2.startDate) from ContractTemplateDetail ctd2 where ctd2.templateId = ctd.templateId and ctd2.startDate <=:currentDate)
            """)
    List<ProductServiceTemplateShortResponse> findForContract(Long serviceDetailId, LocalDate currentDate);

    @Query("""
            select new bg.energo.phoenix.model.response.product.ProductServiceTemplateShortResponse(ctd.templateId,ctd.id,ctd.name,ctd.templateFileId,ctf.name,sct.type,text(ctd.outputFileFormat),text(ctd.fileSigning))
            from ContractTemplateDetail ctd
            join ContractTemplateFiles ctf on ctf.id=ctd.templateFileId
            join ContractTemplate ct on ct.id = ctd.templateId
            join ServiceTemplate sct on sct.templateId=ctd.templateId
            where sct.status='ACTIVE'
            and ct.status = 'ACTIVE'
            and sct.serviceDetailId=:serviceDetailId
            and ctd.startDate = (select max(ctd2.startDate) from ContractTemplateDetail ctd2 where ctd2.templateId = ctd.templateId and ctd2.startDate <=:currentDate)
            """)
    List<ProductServiceTemplateShortResponse> findForCopy(Long serviceDetailId, LocalDate currentDate);

    @Query(nativeQuery = true, value = """
            select t.id
            from service_contract.contracts sc
                     join service_contract.contract_details scd on sc.id = scd.contract_id
                     join service.service_details sd on scd.service_detail_id = sd.id
                     join service.service_templates st on sd.id = st.service_detail_id and st.status = 'ACTIVE'
                     join template.templates t on st.template_id = t.id
                     join customer.customer_details cd on scd.customer_detail_id = cd.id
                     join customer.customers c on cd.customer_id = c.id
                     join template.template_details td on t.id = td.template_id and td.start_date = (select max(itd.start_date)
                                                                                                     from template.template_details itd
                                                                                                     where itd.template_id = t.id
                                                                                                       and itd.start_date <= current_date)
            where sc.id = :contractId
              and scd.version_id = :contractVersion
              and ((td.customer_type is null or cardinality(td.customer_type) = 0) or
                   text(c.customer_type) = any (array(select text(value) from unnest(td.customer_type) as value)))
              and st.service_template_type in ('CONTRACT_TEMPLATE', 'BI_CONTRACT_TEMPLATE')
              and t.id in (:templateIds)
            union
            select t.id
            from service_contract.contracts sc
                     join service_contract.contract_details scd on sc.id = scd.contract_id
                     join service.service_details sd on scd.service_detail_id = sd.id
                     join template.templates t
                          on :purposes is not null and text(t.template_purpose) in (:purposes) and t.status = 'ACTIVE'
                     join customer.customer_details cd on scd.customer_detail_id = cd.id
                     join customer.customers c on cd.customer_id = c.id
                     join template.template_details td on t.id = td.template_id and td.start_date = (select max(itd.start_date)
                                                                                                     from template.template_details itd
                                                                                                     where itd.template_id = t.id
                                                                                                       and itd.start_date <= current_date)
            where sc.id = :contractId
              and scd.version_id = :contractVersion
              and scd.type = 'ADDITIONAL_AGREEMENT'
              and ((td.customer_type is null or cardinality(td.customer_type) = 0) or
                   text(c.customer_type) = any (array(select text(value) from unnest(td.customer_type) as value)))
              and t.id in (:templateIds)
            """)
    Set<Long> findForContractDocument(Long contractId,
                                      Long contractVersion,
                                      List<Long> templateIds,
                                      List<String> purposes);

    @Query(nativeQuery = true, value = """
            select t.id                        as templateId,
                   td.version                  as templateVersion,
                   td.name                     as templateName,
                   text(td.output_file_format) as outputFileFormat,
                   text(td.file_signing)       as fileSignings
            from service_contract.contracts sc
                     join service_contract.contract_details scd on sc.id = scd.contract_id
                     join service.service_details sd on scd.service_detail_id = sd.id
                     join service.service_templates st on sd.id = st.service_detail_id and st.status = 'ACTIVE'
                     join template.templates t on st.template_id = t.id
                     join customer.customer_details cd on scd.customer_detail_id = cd.id
                     join customer.customers c on cd.customer_id = c.id
                     join template.template_details td on t.id = td.template_id and td.start_date = (select max(itd.start_date)
                                                                                                     from template.template_details itd
                                                                                                     where itd.template_id = t.id
                                                                                                       and itd.start_date <= current_date)
            where sc.id = :contractId
              and scd.version_id = :contractVersion
              and ((td.customer_type is null or cardinality(td.customer_type) = 0) or
                   text(c.customer_type) = any (array(select text(value) from unnest(td.customer_type) as value)))
              and st.service_template_type in ('CONTRACT_TEMPLATE', 'BI_CONTRACT_TEMPLATE')
            union
            select t.id,
                   td.version,
                   td.name,
                   text(td.output_file_format),
                   text(td.file_signing)
            from service_contract.contracts sc
                     join service_contract.contract_details scd on sc.id = scd.contract_id
                     join service.service_details sd on scd.service_detail_id = sd.id
                     join template.templates t
                          on :purposes is not null and text(t.template_purpose) in (:purposes) and t.status = 'ACTIVE'
                     join customer.customer_details cd on scd.customer_detail_id = cd.id
                     join customer.customers c on cd.customer_id = c.id
                     join template.template_details td on t.id = td.template_id and td.start_date = (select max(itd.start_date)
                                                                                                     from template.template_details itd
                                                                                                     where itd.template_id = t.id
                                                                                                       and itd.start_date <= current_date)
            where sc.id = :contractId
              and scd.version_id = :contractVersion
              and scd.type = 'ADDITIONAL_AGREEMENT'
              and ((td.customer_type is null or cardinality(td.customer_type) = 0) or
                   text(c.customer_type) = any (array(select text(value) from unnest(td.customer_type) as value)))   
                """)
    List<DocumentGenerationPopupMiddleResponse> findTemplatesForContractDocumentGeneration(Long contractId, Long contractVersion, List<String> purposes);
}
