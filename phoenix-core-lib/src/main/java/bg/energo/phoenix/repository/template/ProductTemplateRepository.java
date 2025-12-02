package bg.energo.phoenix.repository.template;

import bg.energo.phoenix.model.entity.template.ProductTemplate;
import bg.energo.phoenix.model.response.contract.DocumentGenerationPopupMiddleResponse;
import bg.energo.phoenix.model.response.product.ProductServiceTemplateShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Repository
public interface ProductTemplateRepository extends JpaRepository<ProductTemplate, Long> {

    @Query("""
            select pct from ProductTemplate pct
            where pct.status='ACTIVE'
            and pct.productDetailId = :productDetailId
            """)
    List<ProductTemplate> findByProductDetailId(Long productDetailId);

    @Query("""
            select new bg.energo.phoenix.model.response.product.ProductServiceTemplateShortResponse(ctd.templateId,ctd.id,ctd.name,ctd.templateFileId,ctf.name,pct.type,text(ctd.outputFileFormat),text(ctd.fileSigning))
            from ContractTemplateDetail ctd
            join ContractTemplateFiles ctf on ctf.id=ctd.templateFileId
            join ContractTemplate ct on ct.id = ctd.templateId
            join ProductTemplate pct on pct.templateId=ctd.templateId
            where pct.status='ACTIVE'
            and (ct.status = 'ACTIVE' or ct.status = 'INACTIVE')
            and pct.productDetailId=:productDetailId
            and ctd.startDate = (select max(ctd2.startDate) from ContractTemplateDetail ctd2 where ctd2.templateId = ctd.templateId and ctd2.startDate <=:currentDate)
            """)
    List<ProductServiceTemplateShortResponse> findForContract(Long productDetailId, LocalDate currentDate);

    @Query("""
            select new bg.energo.phoenix.model.response.product.ProductServiceTemplateShortResponse(ctd.templateId,ctd.id,ctd.name,ctd.templateFileId,ctf.name,pct.type,text(ctd.outputFileFormat),text(ctd.fileSigning))
            from ContractTemplateDetail ctd
            join ContractTemplateFiles ctf on ctf.id=ctd.templateFileId
            join ContractTemplate ct on ct.id = ctd.templateId
            join ProductTemplate pct on pct.templateId=ctd.templateId
            where pct.status='ACTIVE'
            and ct.status = 'ACTIVE'
            and pct.productDetailId=:productDetailId
            and ctd.startDate = (select max(ctd2.startDate) from ContractTemplateDetail ctd2 where ctd2.templateId = ctd.templateId and ctd2.startDate <=:currentDate)
            """)
    List<ProductServiceTemplateShortResponse> findForCopy(Long productDetailId, LocalDate currentDate);


    @Query(nativeQuery = true, value = """
            with pod_consumption_purpose as (select pd.consumption_purpose,
                                                    cd.id as contract_detail_id
                                             from product_contract.contract_pods cp
                                                      join product_contract.contract_details cd on cp.contract_detail_id = cd.id
                                                      join pod.pod_details pd on cp.pod_detail_id = pd.id
                                             where cp.status = 'ACTIVE'
                                             group by pd.consumption_purpose, cd.id)
            select t.id
            from product_contract.contracts pc
                     join product_contract.contract_details pcd on pc.id = pcd.contract_id
                     join product.product_details pd on pcd.product_detail_id = pd.id
                     join product.product_templates pt on pd.id = pt.product_detail_id and pt.status = 'ACTIVE'
                     join template.templates t on pt.template_id = t.id
                     join customer.customer_details cd on pcd.customer_detail_id = cd.id
                     join customer.customers c on cd.customer_id = c.id
                     join template.template_details td on t.id = td.template_id and td.start_date = (select max(itd.start_date)
                                                                                                     from template.template_details itd
                                                                                                     where itd.template_id = t.id
                                                                                                       and itd.start_date <= current_date)
                     join pod_consumption_purpose pcp on pcp.contract_detail_id = pcd.id
            where pc.id = :contractId
              and pcd.version_id = :contractVersion
              and ((td.customer_type is null or cardinality(td.customer_type) = 0) or
                   text(c.customer_type) = any (array(select text(value) from unnest(td.customer_type) as value)))
              and pt.product_template_type in ('CONTRACT_TEMPLATE', 'BI_CONTRACT_TEMPLATE')
              and ((td.purpose_of_consumption is null or cardinality(td.purpose_of_consumption) = 0) or
                   text(pcp.consumption_purpose) = any (array(select text(value) from unnest(td.purpose_of_consumption) as value)))
              and t.id in (:templateIds)
            union
            select t.id
            from product_contract.contracts pc
                     join product_contract.contract_details pcd on pc.id = pcd.contract_id
                     join product.product_details pd on pcd.product_detail_id = pd.id
                     join template.templates t
                          on :purposes is not null and text(t.template_purpose) in (:purposes) and t.status = 'ACTIVE'
                     join customer.customer_details cd on pcd.customer_detail_id = cd.id
                     join customer.customers c on cd.customer_id = c.id
                     join template.template_details td on t.id = td.template_id and td.start_date = (select max(itd.start_date)
                                                                                                     from template.template_details itd
                                                                                                     where itd.template_id = t.id
                                                                                                       and itd.start_date <= current_date)
                     join pod_consumption_purpose pcp on pcp.contract_detail_id = pcd.id
            where pc.id = :contractId
              and pcd.version_id = :contractVersion
              and pcd.type = 'ADDITIONAL_AGREEMENT'
              and ((td.customer_type is null or cardinality(td.customer_type) = 0) or
                   text(c.customer_type) = any (array(select text(value) from unnest(td.customer_type) as value)))
              and ((td.purpose_of_consumption is null or cardinality(td.purpose_of_consumption) = 0) or
                   text(pcp.consumption_purpose) = any (array(select text(value) from unnest(td.purpose_of_consumption) as value)))
              and t.id in (:templateIds)
            """)
    Set<Long> findForContractDocument(Long contractId,
                                      Integer contractVersion,
                                      List<Long> templateIds,
                                      List<String> purposes);

    @Query(nativeQuery = true, value = """
            with pod_consumption_purpose as (select pd.consumption_purpose,
                                                    cd.id as contract_detail_id
                                             from product_contract.contract_pods cp
                                                      join product_contract.contract_details cd on cp.contract_detail_id = cd.id
                                                      join pod.pod_details pd on cp.pod_detail_id = pd.id
                                             where cp.status = 'ACTIVE'
                                             group by pd.consumption_purpose, cd.id)
            select t.id                        as templateId,
                   td.version                  as templateVersion,
                   td.name                     as templateName,
                   text(td.output_file_format) as outputFileFormat,
                   text(td.file_signing)       as fileSignings
            from product_contract.contracts pc
                     join product_contract.contract_details pcd on pc.id = pcd.contract_id
                     join product.product_details pd on pcd.product_detail_id = pd.id
                     join product.product_templates pt on pd.id = pt.product_detail_id and pt.status = 'ACTIVE'
                     join template.templates t on pt.template_id = t.id
                     join customer.customer_details cd on pcd.customer_detail_id = cd.id
                     join customer.customers c on cd.customer_id = c.id
                     join template.template_details td on t.id = td.template_id and td.start_date = (select max(itd.start_date)
                                                                                                     from template.template_details itd
                                                                                                     where itd.template_id = t.id
                                                                                                       and itd.start_date <= current_date)
                     join pod_consumption_purpose pcp on pcp.contract_detail_id = pcd.id
            where pc.id = :contractId
              and pcd.version_id = :contractVersion
              and ((td.customer_type is null or cardinality(td.customer_type) = 0) or
                   text(c.customer_type) = any (array(select text(value) from unnest(td.customer_type) as value)))
              and pt.product_template_type in ('CONTRACT_TEMPLATE', 'BI_CONTRACT_TEMPLATE')
              and ((td.purpose_of_consumption is null or cardinality(td.purpose_of_consumption) = 0) or
                   text(pcp.consumption_purpose) = any (array(select text(value) from unnest(td.purpose_of_consumption) as value)))
            union
            select t.id,
                   td.version,
                   td.name,
                   text(td.output_file_format),
                   text(td.file_signing)
            from product_contract.contracts pc
                     join product_contract.contract_details pcd on pc.id = pcd.contract_id
                     join product.product_details pd on pcd.product_detail_id = pd.id
                     join template.templates t
                          on :purposes is not null and text(t.template_purpose) in (:purposes) and t.status = 'ACTIVE'
                     join customer.customer_details cd on pcd.customer_detail_id = cd.id
                     join customer.customers c on cd.customer_id = c.id
                     join template.template_details td on t.id = td.template_id and td.start_date = (select max(itd.start_date)
                                                                                                     from template.template_details itd
                                                                                                     where itd.template_id = t.id
                                                                                                       and itd.start_date <= current_date)
                     join pod_consumption_purpose pcp on pcp.contract_detail_id = pcd.id
            where pc.id = :contractId
              and pcd.version_id = :contractVersion
              and pcd.type = 'ADDITIONAL_AGREEMENT'
              and ((td.customer_type is null or cardinality(td.customer_type) = 0) or
                   text(c.customer_type) = any (array(select text(value) from unnest(td.customer_type) as value)))
              and ((td.purpose_of_consumption is null or cardinality(td.purpose_of_consumption) = 0) or
                   text(pcp.consumption_purpose) = any (array(select text(value) from unnest(td.purpose_of_consumption) as value)))
                        """)
    List<DocumentGenerationPopupMiddleResponse> findTemplatesForContractDocumentGeneration(Long contractId, Integer contractVersion, List<String> purposes);
}
