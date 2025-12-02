package bg.energo.phoenix.repository.template;

import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import bg.energo.phoenix.model.response.template.TemplateVersionResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ContractTemplateDetailsRepository extends JpaRepository<ContractTemplateDetail, Long> {
    @Query("""
                select count(td.id) > 0
                from ContractTemplateDetail td
                join ContractTemplate t on td.templateId = t.id
                where td.name = :name 
                and t.status = :status
            """)
    boolean existsByNameAndTemplateStatus(@Param("name") String name,
                                          @Param("status") ContractTemplateStatus status);

    @Query("""
                select new bg.energo.phoenix.model.response.template.TemplateVersionResponse(d.id, d.version, d.startDate)
                from ContractTemplateDetail d
                where d.templateId = :id
                order by d.startDate
            """)
    List<TemplateVersionResponse> getTemplateVersions(@Param("id") Long id);

    @Query("""
                select td
                from ContractTemplateDetail td
                where td.templateId = :id
                and ((coalesce(:versionId, '0') = '0' and td.startDate = (select max(innerTd.startDate)
                                                                         from ContractTemplateDetail innerTd
                                                                         where innerTd.templateId = :id
                                                                         and cast(innerTd.startDate as date) <= current_date))
                    or td.version = :versionId)
            """)
    Optional<ContractTemplateDetail> findByTemplateIdAndVersion(@Param("id") Long id, @Param("versionId") Integer versionId);

    @Query("""
            select count(td.id) > 0
            from ContractTemplateDetail td
            join ContractTemplate t on td.templateId = t.id
            where td.templateId <> :id
            and t.status = :status
            and td.name = :name
            """)
    boolean existsByNameAndStatusInOtherTemplate(@Param("name") String name,
                                                 @Param("status") ContractTemplateStatus status,
                                                 @Param("id") Long id);

    @Query("""
                select count(td.id) > 0
                from ContractTemplateDetail td
                where td.templateId = :id
                and td.version = 1
                and :startDate <= cast(td.startDate as date)
            """)
    boolean startDateBeforeFirstVersionStartDate(@Param("startDate") LocalDate startDate,
                                                 @Param("id") Long id);

    @Query("""
                select count(td.id) > 0
                from ContractTemplateDetail td
                where td.templateId = :id
                and :startDate = cast(td.startDate as date)
            """)
    boolean startDateEqualToAnyVersionStartDate(LocalDate startDate, Long id);

    @Query("""
                select max(td.version)
                from ContractTemplateDetail td
                where td.templateId = :id
            """)
    Integer getMaxTemplateVersion(@Param("id") Long id);

    @Query(value = """
            select ctd.*
            from template.template_details ctd
            where ctd.template_id = :templateId
            and ctd.start_date <= :date
            and (ctd.end_date is null or ctd.end_date >= :date)
            order by ctd.start_date desc
            limit 1
            """, nativeQuery = true)
    Optional<ContractTemplateDetail> findRespectiveTemplateDetailsByTemplateIdAndDate(Long templateId, LocalDate date);

    @Query("""
            select ctd
            from ContractTemplateDetail ctd
            join ContractTemplate ct on ctd.templateId = ct.id
            where ct.id in (:templateIds)
            and ctd.startDate = (select max(ictd.startDate)
                                 from ContractTemplateDetail ictd
                                 where ictd.templateId = ct.id
                                 and ictd.startDate <= current_date)
            """)
    Set<ContractTemplateDetail> findRespectiveTemplateDetailsByTemplateIds(Set<Long> templateIds);

    @Query("""
            select td
            from ServiceOrder so
            join ServiceDetails sd on so.serviceDetailId = sd.id
            left join ServiceTemplate st on st.serviceDetailId = sd.id and st.status = 'ACTIVE' and st.type = 'EMAIL_TEMPLATE'
            join ContractTemplateDetail td on (td.templateId = coalesce(so.emailTemplateId, st.templateId) and td.startDate = (select max(ictd.startDate)
                                                                                                                              from ContractTemplateDetail ictd
                                                                                                                              where ictd.templateId = coalesce(so.emailTemplateId, st.templateId)
                                                                                                                              and ictd.startDate <= current_date))
            where so.id = :serviceOrderId                                                                                 
            """)
    Optional<ContractTemplateDetail> fetchServiceOrderEmailTemplate(Long serviceOrderId);

    @Query("""
            select td
            from GoodsOrder go
            left join ContractTemplate ct on (go.emailTemplateId = ct.id  )
            left join ContractTemplate ct2 on (ct2.defaultForGoodsOrderEmail = true and ct.status = 'ACTIVE')
            join ContractTemplateDetail td on (td.templateId = ct.id and td.startDate = (select max(ictd.startDate)
                                                                                         from ContractTemplateDetail ictd
                                                                                         where ictd.templateId = coalesce(ct.id,ct2.id) 
                                                                                         and ictd.startDate <= current_date))
            where go.id = :goodsOrderId
            and coalesce(ct.id,ct2.id) is not null 
            """)
    Optional<ContractTemplateDetail> fetchGoodsOrderEmailTemplate(Long goodsOrderId);

    @Query("""
            select td
            from Action a
            left join ContractTemplate ct on a.templateId = ct.id
            left join Penalty p on p.id = a.penaltyId
            left join ContractTemplate ct2 on p.templateId = ct2.id
            join ContractTemplateDetail td on (td.templateId = coalesce(ct.id,ct2.id) and td.startDate = (select max(ictd.startDate)
                                                                                         from ContractTemplateDetail ictd
                                                                                         where ictd.templateId = coalesce(ct.id,ct2.id)
                                                                                         and ictd.startDate <= current_date))
            where a.id = :actionId
            and coalesce(ct2.id,ct.id) is not null
            """)
    Optional<ContractTemplateDetail> fetchActionPenaltyDocumentTemplate(Long actionId);

    @Query("""
            select td
            from Action a
            left join ContractTemplate ct on a.emailTemplateId = ct.id
            left join Penalty p on p.id = a.penaltyId
            left join ContractTemplate ct2 on p.emailTemplateId = ct2.id
            join ContractTemplateDetail td on (td.templateId = coalesce(ct.id,ct2.id) and td.startDate = (select max(ictd.startDate)
                                                                                         from ContractTemplateDetail ictd
                                                                                         where ictd.templateId = coalesce(ct.id,ct2.id)
                                                                                         and ictd.startDate <= current_date))
            where a.id = :actionId
            and coalesce(ct2.id,ct.id) is not null
            """)
    Optional<ContractTemplateDetail> fetchActionPenaltyEmailTemplate(Long actionId);
    @Query("""
    select ctd
    from Termination t
    join ContractTemplate ct on t.templateId = ct.id
    join ContractTemplateDetail ctd on (ctd.templateId = ct.id and ctd.startDate = (select max(ictd.startDate)
                                                                                     from ContractTemplateDetail ictd
                                                                                     where ictd.templateId = ct.id
                                                                                     and ictd.startDate <= current_date ))
    where t.id = :terminationId                                                                                                    
    """)
    Optional<ContractTemplateDetail> fetchTerminationEmailTemplate(Long terminationId);
}
