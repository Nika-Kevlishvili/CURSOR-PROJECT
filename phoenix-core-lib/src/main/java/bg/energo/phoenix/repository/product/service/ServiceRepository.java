package bg.energo.phoenix.repository.product.service;

import bg.energo.phoenix.model.entity.product.service.EPService;
import bg.energo.phoenix.model.entity.product.service.ServiceContractServiceListingMiddleResponse;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.response.billing.billingRun.condition.ConditionParameterResponse;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse;
import bg.energo.phoenix.model.response.service.AvailableServiceRelatedEntitiyResponse;
import bg.energo.phoenix.model.response.service.ServiceListMiddleResponse;
import bg.energo.phoenix.model.response.service.ServiceListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<EPService, Long> {

    List<EPService> findByIdInAndStatusIn(List<Long> ids, List<ServiceStatus> statuses);

    Optional<EPService> findByIdAndStatusIn(Long id, List<ServiceStatus> statuses);

    @Query(
            value = """
                        select count (s.id) > 0 from EPService s
                        join ProductLinkToService pls on pls.linkedService.id = s.id
                            where s.id = :serviceId
                            and pls.productSubObjectStatus = 'ACTIVE'
                            and pls.productDetails.product.productStatus = 'ACTIVE'
                    """
    )
    boolean hasConnectionToProduct(@Param("serviceId") Long serviceId);


    @Query(
            value = """
                        select count (s.id) > 0 from EPService s
                        join ServiceLinkedService sls on sls.service.id = s.id
                            where s.id = :serviceId
                            and sls.status = 'ACTIVE'
                            and sls.serviceDetails.service.status = 'ACTIVE'
                    """
    )
    boolean hasConnectionToService(@Param("serviceId") Long serviceId);

    @Query(nativeQuery = true, value = """
                        WITH filtered_services AS (
                        SELECT s.id
                        FROM service.services s
                        JOIN service.service_details sd ON sd.service_id = s.id
                        WHERE
                            (coalesce(:excludeOldVersion, 'false') = 'false'
                             OR (:excludeOldVersion = 'true' AND sd.id = s.last_service_detail_id))
                            AND ((:individualServiceOption = 'ALL' and ((coalesce(s.customer_identifier, '0') <> '0' and text(s.status) in (:individualServiceStatuses))
                                        or (coalesce(s.customer_identifier, '0') = '0' and text(s.status) in (:standardServiceStatuses))))
                                        or (:individualServiceOption = 'YES' and (coalesce(s.customer_identifier, '0') <> '0' and text(s.status)   in (:individualServiceStatuses)))
                                        or (:individualServiceOption = 'NO' and (coalesce(s.customer_identifier, '0') = '0' and text(s.status) in (:standardServiceStatuses))))
                            AND ((:serviceDetailStatuses) is null OR (text(sd.status) IN (:serviceDetailStatuses)))
                            AND ((:serviceGroupIds) is null OR sd.service_group_id IN (:serviceGroupIds))
                            AND ((:serviceTypeIds) is null OR sd.service_type_id IN (:serviceTypeIds))
                            and (:serviceContractTermNames is null or exists(select 1
                                                                               from  service.service_contract_terms sct
                                                                               where sct.service_details_id = sd.id
                                                                                 and text(sct.name) in (:serviceContractTermNames)
                                                                                 and sct.status = 'ACTIVE'))
                                     and (
                                       ((:salesChannelsIds) is not null and coalesce(:globalSalesChannel, '0') <> '0'
                                            and exists(select 1
                                                       from service.service_sales_channels ssh
                                                       where ssh.service_detail_id = sd.id
                                                         and ssh.sales_channel_id in (:salesChannelsIds)
                                                         and ssh.status = 'ACTIVE') or
                                        sd.global_sales_channel = :globalSalesChannel)
                                           or ((:salesChannelsIds) is not null and coalesce(:globalSalesChannel, '0') = '0'
                                           and exists(select 1
                                                      from service.service_sales_channels ssh
                                                      where ssh.service_detail_id = sd.id
                                                        and ssh.sales_channel_id in (:salesChannelsIds)
                                                        and ssh.status = 'ACTIVE'))
                                           or ((:salesChannelsIds) is null and coalesce(:globalSalesChannel, '0') <> '0' and
                                               sd.global_sales_channel = :globalSalesChannel)
                                           or ((:salesChannelsIds) is null and coalesce(:globalSalesChannel, '0') = '0')
                                       )
                                     and (
                                       ((:segmentIds) is not null and coalesce(:globalSegment, '0') <> '0'
                                            and exists(select 1
                                                       from service.service_segments ss
                                                       where ss.service_detail_id = sd.id
                                                         and ss.segment_id in (:segmentIds)
                                                         and ss.status = 'ACTIVE') or sd.global_segment = :globalSegment)
                                           or ((:segmentIds) is not null and coalesce(:globalSegment, '0') = '0'
                                           and exists(select 1
                                                      from service.service_segments ss
                                                      where ss.service_detail_id = sd.id
                                                        and ss.segment_id in (:segmentIds)
                                                        and ss.status = 'ACTIVE'))
                                           or
                                       ((:segmentIds) is null and coalesce(:globalSegment, '0') <> '0' and sd.global_segment = :globalSegment)
                                           or ((:segmentIds) is null and coalesce(:globalSegment, '0') = '0')
                                       )
                                     and (coalesce(:consumptionPurposes, '0') = '0' or
                                          CAST(sd.consumption_purpose as text[]) && CAST(:consumptionPurposes as text[]))
                    ),
                    service_groups AS (
                        SELECT sd.id AS service_detail_id, sg.name AS service_group_name
                        FROM service.service_details sd
                        LEFT JOIN nomenclature.service_groups sg ON sd.service_group_id = sg.id
                    ),
                    penalty_groups AS (
                        SELECT spg.service_detail_id, STRING_AGG(pgd.name,',') AS penalty_group_names
                        FROM service.service_penalty_groups spg
                        JOIN terms.penalty_groups pg2 ON spg.penalty_group_id = pg2.id
                        JOIN terms.penalty_group_details pgd ON pgd.penalty_group_id = pg2.id
                        WHERE spg.status = 'ACTIVE' AND pg2.status = 'ACTIVE'
                        GROUP BY spg.service_detail_id
                    ),
                    penalties as (
                           select sp.service_detail_id,STRING_AGG( p2.name,',') AS penalty_names
                            from service.service_penalties sp
                            join terms.penalties p2 on sp.penalty_id = p2.id
                             where sp.status = 'ACTIVE'  and p2.status = 'ACTIVE'
                        GROUP BY sp.service_detail_id
                    ),
                    termination_groups as
                            (select stg.service_detail_id,STRING_AGG( tgd.name,',') AS termination_group_names
                              from service.service_termination_groups stg
                              join product.termination_groups tg on stg.termination_group_id = tg.id
                              join product.termination_group_details tgd on tgd.termination_group_id = tg.id
                               where tg.status = 'ACTIVE'
                              GROUP BY stg.service_detail_id
                    ),
                    terminations AS (
                        SELECT st.service_detail_id, STRING_AGG( t.name,',') AS termination_names
                        FROM service.service_terminations st
                        JOIN product.terminations t ON st.termination_id = t.id
                        WHERE st.status = 'ACTIVE' AND t.status = 'ACTIVE'
                        GROUP BY st.service_detail_id
                    ),
                    terms as(
                    select t2.id as term_id, t2.name as term_names
                                                 from terms.terms t2
                                                 where t2.status = 'ACTIVE'
                    ),
                    term_groups as(
                                   select t3.id as term_group_id, tgd.name as term_group_names
                                    from terms.term_groups t3
                                    join terms.term_group_details tgd on tgd.group_id = t3.id
                                   where t3.status = 'ACTIVE'
                    ),
                    price_component_groups AS (
                        SELECT spcg.service_detail_id, STRING_AGG( pcgd.name,',') AS price_component_group_names
                        FROM service.service_price_component_groups spcg
                        JOIN price_component.price_component_groups pcg ON spcg.price_component_group_id = pcg.id
                        JOIN price_component.price_component_group_details pcgd ON pcgd.price_component_group_id = pcg.id
                        WHERE spcg.status = 'ACTIVE' AND pcg.status = 'ACTIVE'
                        GROUP BY spcg.service_detail_id
                    ),
                    price_components as(
                        select spc.service_detail_id, STRING_AGG( pc.name,',') as price_component_names
                        from service.service_price_components spc
                        join price_component.price_components pc on spc.price_component_id = pc.id
                        where spc.status = 'ACTIVE' and pc.status = 'ACTIVE'
                        GROUP BY spc.service_detail_id
                    ),
                    iaps as(
                        select siap.service_detail_id, STRING_AGG( iap.name,',') as iap_names
                                                 from service.service_interim_advance_payment_groups siap
                                                          join interim_advance_payment.interim_advance_payments iap
                                                               on siap.interim_advance_payment_group_id = iap.id
                                                 where siap.status = 'ACTIVE'
                                                   and iap.status = 'ACTIVE'
                                                GROUP BY siap.service_detail_id
                    ),
                    iapgs as(select siapg.service_detail_id,STRING_AGG( iapgd.name,',') as iapg_names
                                                 from service.service_interim_advance_payment_groups siapg
                                                          join interim_advance_payment.interim_advance_payment_groups iapg
                                                               on siapg.interim_advance_payment_group_id = iapg.id
                                                          join interim_advance_payment.interim_advance_payment_group_details iapgd
                                                               on iapgd.interim_advance_payment_group_id = iapg.id
                                                 where siapg.status = 'ACTIVE'
                                                   and iapg.status = 'ACTIVE'
                                                   GROUP BY siapg.service_detail_id),
                    filtered_search AS (
                        SELECT fss.id
                        FROM filtered_services fss
                        JOIN service.service_details sd ON sd.service_id = fss.id
                        LEFT JOIN service_groups sg ON sg.service_detail_id = sd.id
                        LEFT JOIN penalty_groups spg ON spg.service_detail_id = sd.id
                        LEFT join penalties p on p.service_detail_id = sd.id
                        left join termination_groups tg on tg.service_detail_id = sd.id
                        LEFT JOIN terminations tm ON tm.service_detail_id = sd.id
                        LEFT JOIN terms trm on sd.term_id = trm.term_id
                        LEFT JOIN term_groups trmg on sd.term_group_id = trmg.term_group_id
                        LEFT JOIN price_component_groups pcg ON pcg.service_detail_id = sd.id
                        LEFT JOIN price_components pc ON pc.service_detail_id = sd.id
                        LEFT JOIN iaps ON iaps.service_detail_id = sd.id
                        left join iapgs on iapgs.service_detail_id = sd.id
                        WHERE (coalesce(:prompt, '0') = '0'
                            OR (:searchBy = 'ALL' AND (
                                LOWER(sd.name) LIKE :prompt
                                OR LOWER(sg.service_group_name) LIKE :prompt
                                OR LOWER(spg.penalty_group_names) LIKE :prompt
                                OR LOWER(p.penalty_names) like :prompt
                                OR LOWER(tm.termination_names) LIKE :prompt
                                OR LOWER(trm.term_names) LIKE :prompt
                                OR LOWER(trmg.term_group_names) LIKE :prompt
                                OR LOWER(pcg.price_component_group_names) LIKE :prompt
                                OR LOWER(pc.price_component_names) LIKE :prompt
                                OR LOWER(tg.termination_group_names) LIKE :prompt
                                OR LOWER(iaps.iap_names) LIKE :prompt
                                OR LOWER(iapgs.iapg_names) LIKE :prompt
                            )) or ((:searchBy = 'NAME' and lower(sd.name) like :prompt)
                                           or (:searchBy = 'SERVICE_GROUP_NAME' and LOWER(sg.service_group_name) LIKE :prompt)
                                           or (:searchBy = 'PENALTIES_GROUP_NAME' and LOWER(spg.penalty_group_names) LIKE :prompt)
                                           or (:searchBy = 'PENALTY_NAME' and LOWER(p.penalty_names) like :prompt)
                                           or (:searchBy = 'TERMINATIONS_GROUP_NAME' and LOWER(trmg.term_group_names) LIKE :prompt)
                                           or (:searchBy = 'TERMINATION_NAME' and LOWER(tm.termination_names) LIKE :prompt)
                                           or (:searchBy = 'TERMS_NAME' and LOWER(trm.term_names) LIKE :prompt)
                                           or (:searchBy = 'TERMS_GROUP_NAME' and LOWER(tg.termination_group_names) LIKE :prompt)
                                           or (:searchBy = 'PRICE_COMPONENTS_GROUP_NAME' and LOWER(pcg.price_component_group_names) LIKE :prompt)
                                           or (:searchBy = 'PRICE_COMPONENT_NAME' and LOWER(pc.price_component_names) LIKE :prompt)
                                           or (:searchBy = 'IAP_NAME' and LOWER(iaps.iap_names) LIKE :prompt)
                                           or (:searchBy = 'IAP_GROUP_NAME' and LOWER(iapgs.iapg_names) LIKE :prompt)
                                         )
                        )
                    )
                    SELECT
                        s.id as serviceId,
                        sd.name AS name,
                        sg.service_group_name as serviceGroupName,
                        s.status AS status,
                        sd.status AS serviceDetailStatus,
                        st.id AS serviceTypeId,
                        st.name AS serviceTypeName,
                        s.create_date as dateOfCreation,
                        sd.global_sales_channel as globalSalesChannel,
                        CASE WHEN coalesce(s.customer_identifier, '0') <> '0' THEN 'Yes' ELSE 'No' END AS individualService,
                        CASE WHEN :contractTermsDirection = 'ASC' THEN  sct.name WHEN :contractTermsDirection = 'DESC' THEN sct.NAME_DESC ELSE  sct.namE END as contractTermsName,
                        CASE WHEN :salesChannelsDirection = 'ASC' THEN  ssc.name WHEN :salesChannelsDirection = 'DESC' THEN ssc.NAME_DESC ELSE  ssc.namE END as salesChannelsName
                    FROM service.services s
                    JOIN service.service_details sd ON s.last_service_detail_id = sd.id
                    LEFT JOIN service_groups sg ON sg.service_detail_id = sd.id
                    JOIN nomenclature.service_types st ON sd.service_type_id = st.id
                    LEFT JOIN service.vw_service_contract_terms sct ON sct.service_details_id = sd.id
                    LEFT JOIN service.vw_service_sales_channels ssc ON ssc.service_detail_id = sd.id
                    WHERE s.id IN (SELECT id FROM filtered_search)
            """,
            countQuery = """
                        WITH filtered_services AS (
                        SELECT s.id
                        FROM service.services s
                        JOIN service.service_details sd ON sd.service_id = s.id
                        WHERE
                            (coalesce(:excludeOldVersion, 'false') = 'false'
                             OR (:excludeOldVersion = 'true' AND sd.id = s.last_service_detail_id))
                            AND ((:individualServiceOption = 'ALL' and ((coalesce(s.customer_identifier, '0') <> '0' and text(s.status) in (:individualServiceStatuses))
                                        or (coalesce(s.customer_identifier, '0') = '0' and text(s.status) in (:standardServiceStatuses))))
                                        or (:individualServiceOption = 'YES' and (coalesce(s.customer_identifier, '0') <> '0' and text(s.status)   in (:individualServiceStatuses)))
                                        or (:individualServiceOption = 'NO' and (coalesce(s.customer_identifier, '0') = '0' and text(s.status) in (:standardServiceStatuses))))
                            AND ((:serviceDetailStatuses) is null OR (text(sd.status) IN (:serviceDetailStatuses)))
                            AND ((:serviceGroupIds) is null OR sd.service_group_id IN (:serviceGroupIds))
                            AND ((:serviceTypeIds) is null OR sd.service_type_id IN (:serviceTypeIds))
                            and (:serviceContractTermNames is null or exists(select 1
                                                                               from  service.service_contract_terms sct
                                                                               where sct.service_details_id = sd.id
                                                                                 and text(sct.name) in (:serviceContractTermNames)
                                                                                 and sct.status = 'ACTIVE'))
                                     and (
                                       ((:salesChannelsIds) is not null and coalesce(:globalSalesChannel, '0') <> '0'
                                            and exists(select 1
                                                       from service.service_sales_channels ssh
                                                       where ssh.service_detail_id = sd.id
                                                         and ssh.sales_channel_id in (:salesChannelsIds)
                                                         and ssh.status = 'ACTIVE') or
                                        sd.global_sales_channel = :globalSalesChannel)
                                           or ((:salesChannelsIds) is not null and coalesce(:globalSalesChannel, '0') = '0'
                                           and exists(select 1
                                                      from service.service_sales_channels ssh
                                                      where ssh.service_detail_id = sd.id
                                                        and ssh.sales_channel_id in (:salesChannelsIds)
                                                        and ssh.status = 'ACTIVE'))
                                           or ((:salesChannelsIds) is null and coalesce(:globalSalesChannel, '0') <> '0' and
                                               sd.global_sales_channel = :globalSalesChannel)
                                           or ((:salesChannelsIds) is null and coalesce(:globalSalesChannel, '0') = '0')
                                       )
                                     and (
                                       ((:segmentIds) is not null and coalesce(:globalSegment, '0') <> '0'
                                            and exists(select 1
                                                       from service.service_segments ss
                                                       where ss.service_detail_id = sd.id
                                                         and ss.segment_id in (:segmentIds)
                                                         and ss.status = 'ACTIVE') or sd.global_segment = :globalSegment)
                                           or ((:segmentIds) is not null and coalesce(:globalSegment, '0') = '0'
                                           and exists(select 1
                                                      from service.service_segments ss
                                                      where ss.service_detail_id = sd.id
                                                        and ss.segment_id in (:segmentIds)
                                                        and ss.status = 'ACTIVE'))
                                           or
                                       ((:segmentIds) is null and coalesce(:globalSegment, '0') <> '0' and sd.global_segment = :globalSegment)
                                           or ((:segmentIds) is null and coalesce(:globalSegment, '0') = '0')
                                       )
                                     and (coalesce(:consumptionPurposes, '0') = '0' or
                                          CAST(sd.consumption_purpose as text[]) && CAST(:consumptionPurposes as text[]))
                    ),
                    service_groups AS (
                        SELECT sd.id AS service_detail_id, sg.name AS service_group_name
                        FROM service.service_details sd
                        LEFT JOIN nomenclature.service_groups sg ON sd.service_group_id = sg.id
                    ),
                    penalty_groups AS (
                        SELECT spg.service_detail_id, STRING_AGG(pgd.name,',') AS penalty_group_names
                        FROM service.service_penalty_groups spg
                        JOIN terms.penalty_groups pg2 ON spg.penalty_group_id = pg2.id
                        JOIN terms.penalty_group_details pgd ON pgd.penalty_group_id = pg2.id
                        WHERE spg.status = 'ACTIVE' AND pg2.status = 'ACTIVE'
                        GROUP BY spg.service_detail_id
                    ),
                    penalties as (
                           select sp.service_detail_id,STRING_AGG( p2.name,',') AS penalty_names
                            from service.service_penalties sp
                            join terms.penalties p2 on sp.penalty_id = p2.id
                             where sp.status = 'ACTIVE'  and p2.status = 'ACTIVE'
                        GROUP BY sp.service_detail_id
                    ),
                    termination_groups as
                            (select stg.service_detail_id,STRING_AGG( tgd.name,',') AS termination_group_names
                              from service.service_termination_groups stg
                              join product.termination_groups tg on stg.termination_group_id = tg.id
                              join product.termination_group_details tgd on tgd.termination_group_id = tg.id
                               where tg.status = 'ACTIVE'
                              GROUP BY stg.service_detail_id
                    ),
                    terminations AS (
                        SELECT st.service_detail_id, STRING_AGG( t.name,',') AS termination_names
                        FROM service.service_terminations st
                        JOIN product.terminations t ON st.termination_id = t.id
                        WHERE st.status = 'ACTIVE' AND t.status = 'ACTIVE'
                        GROUP BY st.service_detail_id
                    ),
                    terms as(
                    select t2.id as term_id, t2.name as term_names
                                                 from terms.terms t2
                                                 where t2.status = 'ACTIVE'
                    ),
                    term_groups as(
                                   select t3.id as term_group_id, tgd.name as term_group_names
                                    from terms.term_groups t3
                                    join terms.term_group_details tgd on tgd.group_id = t3.id
                                   where t3.status = 'ACTIVE'
                    ),
                    price_component_groups AS (
                        SELECT spcg.service_detail_id, STRING_AGG( pcgd.name,',') AS price_component_group_names
                        FROM service.service_price_component_groups spcg
                        JOIN price_component.price_component_groups pcg ON spcg.price_component_group_id = pcg.id
                        JOIN price_component.price_component_group_details pcgd ON pcgd.price_component_group_id = pcg.id
                        WHERE spcg.status = 'ACTIVE' AND pcg.status = 'ACTIVE'
                        GROUP BY spcg.service_detail_id
                    ),
                    price_components as(
                        select spc.service_detail_id, STRING_AGG( pc.name,',') as price_component_names
                        from service.service_price_components spc
                        join price_component.price_components pc on spc.price_component_id = pc.id
                        where spc.status = 'ACTIVE' and pc.status = 'ACTIVE'
                        GROUP BY spc.service_detail_id
                    ),
                    iaps as(
                        select siap.service_detail_id, STRING_AGG( iap.name,',') as iap_names
                                                 from service.service_interim_advance_payment_groups siap
                                                          join interim_advance_payment.interim_advance_payments iap
                                                               on siap.interim_advance_payment_group_id = iap.id
                                                 where siap.status = 'ACTIVE'
                                                   and iap.status = 'ACTIVE'
                                                GROUP BY siap.service_detail_id
                    ),
                    iapgs as(select siapg.service_detail_id,STRING_AGG( iapgd.name,',') as iapg_names
                                                 from service.service_interim_advance_payment_groups siapg
                                                          join interim_advance_payment.interim_advance_payment_groups iapg
                                                               on siapg.interim_advance_payment_group_id = iapg.id
                                                          join interim_advance_payment.interim_advance_payment_group_details iapgd
                                                               on iapgd.interim_advance_payment_group_id = iapg.id
                                                 where siapg.status = 'ACTIVE'
                                                   and iapg.status = 'ACTIVE'
                                                   GROUP BY siapg.service_detail_id),
                    filtered_search AS (
                        SELECT fss.id
                        FROM filtered_services fss
                        JOIN service.service_details sd ON sd.service_id = fss.id
                        LEFT JOIN service_groups sg ON sg.service_detail_id = sd.id
                        LEFT JOIN penalty_groups spg ON spg.service_detail_id = sd.id
                        LEFT join penalties p on p.service_detail_id = sd.id
                        left join termination_groups tg on tg.service_detail_id = sd.id
                        LEFT JOIN terminations tm ON tm.service_detail_id = sd.id
                        LEFT JOIN terms trm on sd.term_id = trm.term_id
                        LEFT JOIN term_groups trmg on sd.term_group_id = trmg.term_group_id
                        LEFT JOIN price_component_groups pcg ON pcg.service_detail_id = sd.id
                        LEFT JOIN price_components pc ON pc.service_detail_id = sd.id
                        LEFT JOIN iaps ON iaps.service_detail_id = sd.id
                        left join iapgs on iapgs.service_detail_id = sd.id
                        WHERE (coalesce(:prompt, '0') = '0'
                            OR (:searchBy = 'ALL' AND (
                                LOWER(sd.name) LIKE :prompt
                                OR LOWER(sg.service_group_name) LIKE :prompt
                                OR LOWER(spg.penalty_group_names) LIKE :prompt
                                OR LOWER(p.penalty_names) like :prompt
                                OR LOWER(tm.termination_names) LIKE :prompt
                                OR LOWER(trm.term_names) LIKE :prompt
                                OR LOWER(trmg.term_group_names) LIKE :prompt
                                OR LOWER(pcg.price_component_group_names) LIKE :prompt
                                OR LOWER(pc.price_component_names) LIKE :prompt
                                OR LOWER(tg.termination_group_names) LIKE :prompt
                                OR LOWER(iaps.iap_names) LIKE :prompt
                                OR LOWER(iapgs.iapg_names) LIKE :prompt
                            )) or ((:searchBy = 'NAME' and lower(sd.name) like :prompt)
                                           or (:searchBy = 'SERVICE_GROUP_NAME' and LOWER(sg.service_group_name) LIKE :prompt)
                                           or (:searchBy = 'PENALTIES_GROUP_NAME' and LOWER(spg.penalty_group_names) LIKE :prompt)
                                           or (:searchBy = 'PENALTY_NAME' and LOWER(p.penalty_names) like :prompt)
                                           or (:searchBy = 'TERMINATIONS_GROUP_NAME' and LOWER(trmg.term_group_names) LIKE :prompt)
                                           or (:searchBy = 'TERMINATION_NAME' and LOWER(tm.termination_names) LIKE :prompt)
                                           or (:searchBy = 'TERMS_NAME' and LOWER(trm.term_names) LIKE :prompt)
                                           or (:searchBy = 'TERMS_GROUP_NAME' and LOWER(tg.termination_group_names) LIKE :prompt)
                                           or (:searchBy = 'PRICE_COMPONENTS_GROUP_NAME' and LOWER(pcg.price_component_group_names) LIKE :prompt)
                                           or (:searchBy = 'PRICE_COMPONENT_NAME' and LOWER(pc.price_component_names) LIKE :prompt)
                                           or (:searchBy = 'IAP_NAME' and LOWER(iaps.iap_names) LIKE :prompt)
                                           or (:searchBy = 'IAP_GROUP_NAME' and LOWER(iapgs.iapg_names) LIKE :prompt)
                                         )
                        )
                    )
                    SELECT
                        count(* )
                                            FROM service.services s
                    JOIN service.service_details sd ON s.last_service_detail_id = sd.id
                    LEFT JOIN service_groups sg ON sg.service_detail_id = sd.id
                    JOIN nomenclature.service_types st ON sd.service_type_id = st.id
                    LEFT JOIN service.vw_service_contract_terms sct ON sct.service_details_id = sd.id
                    LEFT JOIN service.vw_service_sales_channels ssc ON ssc.service_detail_id = sd.id
                    WHERE s.id IN (SELECT id FROM filtered_search)
                                    """
    )
    Page<ServiceListMiddleResponse> list(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("serviceDetailStatuses") List<String> serviceDetailStatuses,
            @Param("serviceGroupIds") List<Long> serviceGroupIds,
            @Param("serviceTypeIds") List<Long> serviceTypeIds,
            @Param("serviceContractTermNames") List<String> serviceContractTermNames,
            @Param("contractTermsDirection") String contractTermsDirection,
            @Param("salesChannelsIds") List<Long> salesChannelsIds,
            @Param("globalSalesChannel") Boolean globalSalesChannel,
            @Param("salesChannelsDirection") String salesChannelsDirection,
            @Param("segmentIds") List<Long> segmentIds,
            @Param("globalSegment") Boolean globalSegment,
            @Param("consumptionPurposes") String consumptionPurposes,
            @Param("individualServiceOption") String individualServiceOption,
            @Param("standardServiceStatuses") List<String> standardServiceStatuses,
            @Param("individualServiceStatuses") List<String> individualServiceStatuses,
            @Param("excludeOldVersion") String excludeOldVersion,
            Pageable pageable
    );


    @Query("""
            select aps.id
            from VwAvailableProductAndServices aps
            where aps.type = 'SERVICE'
            """)
    List<Long> findAvailableServiceIdsForProduct();


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.service.AvailableServiceRelatedEntitiyResponse(
                        aps.id,
                        aps.displayName,
                        aps.type
                    )
                    from VwAvailableProductAndServices aps
                        where (:prompt is null or concat(lower(aps.name), aps.id) like :prompt)
                        and (:excludedId is null or (aps.type = 'SERVICE' AND aps.id <> :excludedId) OR aps.type <> 'SERVICE')
                        and (:excludedItemId is null or (text(aps.type) = :excludedItemType and aps.id <> :excludedItemId) or text(aps.type) <> :excludedItemType)
                        order by aps.name
                    """
    )
    Page<AvailableServiceRelatedEntitiyResponse> findAvailableProductsAndServices(
            @Param("prompt") String prompt,
            @Param("excludedId") Long excludedId,
            @Param("excludedItemId") Long excludedItemId,
            @Param("excludedItemType") String excludedItemType,
            Pageable pageable
    );


    @Query(
            value = """
                    select aps.id
                    from VwAvailableProductAndServices aps
                    where aps.type = 'SERVICE'
                    and aps.id in :ids
                    """
    )
    List<Long> findAvailableServiceIdsForService(List<Long> ids);

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse(
                        s.id,
                        concat(sd.name, ' (', s.id, ')')
                    )
                    from EPService s
                    join ServiceDetails sd on s.lastServiceDetailId = sd.id
                        where s.id in (
                            select distinct np.id from EPService np
                            join ServiceDetails npd on npd.service.id = np.id
                                where (:prompt is null or lower(npd.name) like :prompt or cast(np.id as string) like :prompt)
                                and np.status ='ACTIVE'
                                and ((:individualOption is null and np.customerIdentifier is null) or (:individualOption = 'INDIVIDUAL_SERVICE' and np.customerIdentifier is not null))
                        )
                    """
    )
    Page<CopyDomainWithVersionBaseResponse> findByCopyGroupBaseRequest(
            @Param("prompt") String prompt,
            @Param("individualOption") String individualOption,
            Pageable pageable
    );

    boolean existsByIdAndStatusIn(Long id, List<ServiceStatus> statuses);

    @Query(value = """
            select distinct  sd.id as detailId,
                             sd.service_id as id,
                             sd.name as name,
                             sd.version_id as versionId,
                             sd.execution_level as executionLevel,
                             s.customer_identifier
                        from
                        service.services s
                         join service.service_details sd
                                 left join service.service_sales_channels ssc on ssc.service_detail_id=sd.id and ssc.status='ACTIVE'
    left join nomenclature.sales_channels sc on sc.id=ssc.sales_channel_id
    left join customer.account_manager_tags amt on amt.portal_tag_id=sc.portal_tag_id
    left join customer.account_managers am on am.id=amt.account_manager_id
                         on sd.service_id = s.id
                        where sd.status = 'ACTIVE'
                        and (:serviceId is null or sd.service_id = :serviceId)
                        and (sd.sale_method) && '{CONTRACT}'
                          and s.status =  'ACTIVE'
                          and (coalesce(:prompt,'0') = '0' or  lower(sd.name) like concat('%',lower(:prompt),'%' ) )
                          and (sd.global_sales_channel or s.customer_identifier is not null or :username is null or am.user_name = :username)
                          and ((:customerDetailId  is null and (sd.available_for_sale = true and current_date between coalesce(sd.available_From,current_date) and coalesce(sd.available_To,current_date)))
                          or
                        (:customerDetailId is not null
                           and
                           (s.customer_identifier is not null
                             and exists(select 1
                                         from customer.customer_details cd
                                         join customer.customers c on c.id=cd.customer_id
                                        where c.identifier = s.customer_identifier
                                          and cd.id = :customerDetailId
                                          and c.status = 'ACTIVE')
                                          and not exists(select 1
                                                          from service_contract.contract_details cdt
                                                          join service_contract.contracts cnt
                                                            on cdt.contract_id = cnt.id
                                                          where cdt.service_detail_id = sd.id
                                                            and cnt.status = 'ACTIVE'
                                                            and cnt.contract_status in ('DRAFT',
                                                                                        'READY',
                                                                                        'SIGNED',
                                                                                        'ENTERED_INTO_FORCE',
                                                                                        'ACTIVE_IN_TERM',
                                                                                        'ACTIVE_IN_PERPETUITY',
                                                                                        'CHANGED_WITH_AGREEMENT')))
                         or
                          (s.customer_identifier is null
                            and
                          (sd.available_for_sale = true and current_date between coalesce(sd.available_From,current_date) and coalesce(sd.available_To,current_date))
                            and
                          (sd.global_segment = 'true'
                            or
                           exists(select 1
                            from customer.customer_details cd
                            join customer.customers c
                              on cd.customer_id = c.id
                            join customer.customer_segments cs
                              on cs.customer_detail_id = cd.id
                            where cd.id = :customerDetailId
                              and c.status = 'ACTIVE'
                              and cs.status = 'ACTIVE'
                              and exists(select 1 from service.service_segments ss
                                          where ss.service_detail_id = sd.id
                                            and ss.segment_id = cs.segment_id
                                            and ss.status = 'ACTIVE'))))))
                           order by s.customer_identifier, sd.name
                        """, nativeQuery = true)
    Page<ServiceContractServiceListingMiddleResponse> searchForContract(@Param("customerDetailId") Long customerDetailId,
                                                                        @Param("username") String username,
                                                                        @Param("prompt") String prompt,
                                                                        @Param("serviceId") Long serviceId,
                                                                        PageRequest pageRequest);

    @Query(value = """

            select coalesce(max('true'),'false')
                                                              from
                                                              service.services s
                                                               join service.service_details sd
                                                               on sd.service_id = s.id
                                                              where sd.id = :serviceDetailsId
                                                                --and '{CONTRACT}' in (sd.sale_method)
                                                                and sd.sale_method && '{CONTRACT}'
                                                                and sd.status = 'ACTIVE'
                                                                and s.status =  'ACTIVE'
                                                                and (:customerDetailId  is null
                                                                or
                                                              (:customerDetailId is not null
                                                                 and
                                                                 (s.customer_identifier is not null
                                                                   and exists(select 1
                                                                               from customer.customer_details cd
                                                                               join customer.customers c on c.id=cd.customer_id
                                                                              where c.identifier = s.customer_identifier
                                                                                and cd.id = :customerDetailId
                                                                                and c.status = 'ACTIVE')
                                                                                and not exists(select 1
                                                                                                from service_contract.contract_details cdt
                                                                                                join service_contract.contracts cnt
                                                                                                  on cdt.contract_id = cnt.id
                                                                                                where cdt.service_detail_id = sd.id
                                                                                                  and cnt.status = 'ACTIVE'
                                                                                                  and cnt.contract_status in ('DRAFT',
                                                                                                                              'READY',
                                                                                                                              'SIGNED',
                                                                                                                              'ENTERED_INTO_FORCE',
                                                                                                                              'ACTIVE_IN_TERM',
                                                                                                                              'ACTIVE_IN_PERPETUITY',
                                                                                                                              'CHANGED_WITH_AGREEMENT')))
                                                               or
                                                                (s.customer_identifier is null
                                                                  and
                                                                (sd.available_for_sale = true and current_timestamp between coalesce(sd.available_From,current_timestamp) and coalesce(sd.available_To,current_timestamp))
                                                                  and
                                                                (sd.global_segment = 'true'
                                                                  or
                                                                 exists(select 1
                                                                  from customer.customer_details cd
                                                                  join customer.customers c
                                                                    on cd.customer_id = c.id
                                                                  join customer.customer_segments cs
                                                                    on cs.customer_detail_id = cd.id
                                                                  where cd.id = :customerDetailId
                                                                    and c.status = 'ACTIVE'
                                                                    and cs.status = 'ACTIVE'
                                                                    and exists(select 1 from service.service_segments ss
                                                                                where ss.service_detail_id = sd.id
                                                                                  and sd.id = :serviceDetailsId
                                                                                  and ss.segment_id = cs.segment_id
                                                                                  and ss.status = 'ACTIVE'))))))
            """, nativeQuery = true)
    boolean validateContractServiceAndCustomerOnCreation(@Param("customerDetailId") Long customerDetailId, @Param("serviceDetailsId") Long serviceDetailsId);

    @Query(value = """
            select sd.id              as detailId,
                   sd.service_id      as id,
                   sd.name            as name,
                   sd.version_id      as versionId,
                   sd.execution_level as executionLevel,
                   sd.available_from,
                   date(sd.available_to)
            from service.services s
                     join service.service_details sd on sd.service_id = s.id
            where s.status = 'ACTIVE'
              and not exists(select 1
                             from service.service_additional_params sap
                             where sap.service_detail_id = sd.id
                               and sap.value is null
                               and sap.label is not null)
              and (sd.sale_method) && '{CONTRACT}'
              and sd.status = 'ACTIVE'
              and sd.available_For_Sale = true
              and current_date between coalesce(date(sd.available_From), current_date) and coalesce(date(sd.available_To), current_date)
              and s.customer_identifier is null
              and (:prompt is null or lower(sd.name) like :prompt)
              and (sd.global_sales_channel or exists(select 1
                                                     from customer.account_managers am
                                                              join customer.account_manager_tags amt
                                                                   on amt.account_manager_id = am.id and am.user_name = :userName
                                                              join nomenclature.sales_channels sc on sc.portal_tag_id = amt.portal_tag_id
                                                              join service.service_sales_channels ssc
                                                                   on ssc.sales_channel_id = sc.id and
                                                                      ssc.service_detail_id = sd.id and ssc.status = 'ACTIVE'))
              and 1 = (select count(1)
                       from service.service_contract_terms sct
                       where sct.service_details_id = sd.id
                         and sct.status = 'ACTIVE')
              and 1 = (select count(1)
                       from service.service_contract_terms sct
                       where sct.service_details_id = sd.id
                         and sct.status = 'ACTIVE'
                         and sct.contract_term_period_type <> 'CERTAIN_DATE')
              and (sd.term_id is null or
                   (
                       1 = (select count(1)
                            from terms.terms t
                                     join terms.invoice_payment_terms ipt on ipt.term_id = t.id
                            where t.id = sd.term_id
                              and t.status = 'ACTIVE'
                              and ipt.status = 'ACTIVE')
                           and
                       1 = (select count(1)
                            from terms.terms t
                                     join terms.invoice_payment_terms ipt on ipt.term_id = t.id
                            where t.id = sd.term_id
                              and ipt.value is not null
                              and t.status = 'ACTIVE'
                              and ipt.status = 'ACTIVE')
                           and
                       (
                           exists
                               (select 1
                                from terms.terms trm
                                where trm.id = sd.term_id
                                  and trm.status = 'ACTIVE'
                                  and array_length(trm.contract_entry_into_force, 1) = 1
                                  and trm.contract_entry_into_force not in ('{EXACT_DAY}', '{MANUAL}')
                                  and array_length(trm.start_initial_term_of_contract, 1) = 1
                                  and trm.start_initial_term_of_contract not in ('{EXACT_DATE}', '{MANUAL}'))
                           )
                       )
                )
              and (sd.term_group_id is null or
                   (1 = (select count(1)
                         from terms.terms t
                                  join terms.invoice_payment_terms ipt on ipt.term_id = t.id
                                  join terms.term_group_terms tgt on tgt.term_id = t.id
                                  join terms.term_group_details tgd on tgt.term_group_detail_id = tgd.id
                                  join terms.term_groups tg on tgd.group_id = tg.id
                         where sd.term_group_id = tg.id
                           and t.status = 'ACTIVE'
                           and ipt.status = 'ACTIVE'
                           and tgt.status = 'ACTIVE'
                           and tg.status = 'ACTIVE')
                       and
                    1 = (select count(1)
                         from terms.terms t
                                  join terms.invoice_payment_terms ipt on ipt.term_id = t.id
                                  join terms.term_group_terms tgt on tgt.term_id = t.id
                                  join terms.term_group_details tgd on tgt.term_group_detail_id = tgd.id
                                  join terms.term_groups tg on tgd.group_id = tg.id
                         where sd.term_group_id = tg.id
                           and ipt.value is not null
                           and t.status = 'ACTIVE'
                           and ipt.status = 'ACTIVE'
                           and tgt.status = 'ACTIVE'
                           and tg.status = 'ACTIVE')
                       and
                    (
                        exists
                            (select 1
                             from terms.terms trm
                                      join terms.term_group_terms tgt on tgt.term_id = trm.id
                                      join terms.term_group_details tgd on tgt.term_group_detail_id = tgd.id
                                      join terms.term_groups tg on tgd.group_id = tg.id
                             where sd.term_group_id = tg.id
                               and trm.status = 'ACTIVE'
                               and array_length(trm.contract_entry_into_force, 1) = 1
                               and trm.contract_entry_into_force not in ('{EXACT_DAY}', '{MANUAL}')
                               and array_length(trm.start_initial_term_of_contract, 1) = 1
                               and trm.start_initial_term_of_contract not in ('{EXACT_DATE}', '{MANUAL}'))
                        )
                       )
                )
              and array_length(sd.payment_guarantee, 1) = 1
              and (case
                       when sd.payment_guarantee = '{CASH_DEPOSIT}' then
                           sd.cash_deposit_amount is not null and sd.cash_deposit_currency_id is not null
                       else 1 = 1 end)
              and (case
                       when sd.payment_guarantee = '{BANK}' then
                           sd.bank_guarantee_amount is not null and sd.bank_guarantee_currency_id is not null
                       else 1 = 1 end)
              and (case
                       when sd.payment_guarantee = '{CASH_DEPOSIT_AND_BANK}' then
                           sd.bank_guarantee_amount is not null and sd.bank_guarantee_currency_id is not null and
                           sd.cash_deposit_amount is not null and
                           sd.cash_deposit_currency_id is not null
                       else 1 = 1 end)
              and (not exists (select 1
                               from service.service_interim_advance_payments siap
                               where siap.service_detail_id = sd.id
                                 and siap.status = 'ACTIVE')
                or
                   1 = (select count(1)
                        from service.service_interim_advance_payments siap
                                 join interim_advance_payment.interim_advance_payments iap
                                      on siap.interim_advance_payment_id = iap.id
                        where siap.service_detail_id = sd.id
                          and iap.payment_type = 'OBLIGATORY'
                          and (iap.match_term_of_standard_invoice = true
                            or
                               (
                                   1 = (select count(1)
                                        from interim_advance_payment.interim_advance_payment_terms iapt
                                        where iapt.interim_advance_payment_id = iap.id
                                          and iapt.status = 'ACTIVE')
                                       and
                                   1 = (select count(1)
                                        from interim_advance_payment.interim_advance_payment_terms iapt
                                        where iapt.interim_advance_payment_id = iap.id
                                          and iapt.status = 'ACTIVE'
                                          and iapt.value is not null)
                                   )
                            )
                          and (iap.value_type = 'PRICE_COMPONENT' or
                               ((iap.value_type = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT' or iap.value_type = 'EXACT_AMOUNT') and
                                iap.value is not null))
                          and (case
                                   when iap.date_of_issue_type in ('DATE_OF_THE_MONTH', 'WORKING_DAYS_AFTER_INVOICE_DATE')
                                       then iap.date_of_issue_value is not null
                                   else 1 = 1 end)
                          and iap.status = 'ACTIVE'
                          and siap.status = 'ACTIVE')
                )
              and (sd.equal_monthly_installments_activation = 'false' or
                   (sd.installment_number is not null and sd.amount is not null))
              and (:customerDetailId is null or sd.global_segment = 'true'
                or
                   exists
                       (select 1
                        from service.service_segments ss
                        where ss.service_detail_id = sd.id
                          and sd.status = 'ACTIVE'
                          and exists (select 1
                                      from customer.customer_segments cs
                                      where cs.customer_detail_id = :customerDetailId
                                        and cs.segment_id = ss.segment_id
                                        and cs.status = 'ACTIVE'))
                )
              and not exists(select siap
                             from service.service_interim_advance_payments siap
                                      join interim_advance_payment.interim_advance_payments iap
                                           on siap.interim_advance_payment_id = iap.id
                             where siap.service_detail_id = sd.id
                               and (iap.date_of_issue_type in ('WORKING_DAYS_AFTER_INVOICE_DATE', 'DATE_OF_THE_MONTH')
                                 and iap.date_of_issue_value is null)
                               and (iap.value_type in ('PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT', 'EXACT_AMOUNT')
                                 and iap.value is null))
              and not exists(select 1
                             from service.service_price_components spc
                                      join price_component.price_components pc
                                           on spc.price_component_id = pc.id
                                               and pc.status = 'ACTIVE'
                                      join price_component.price_component_formula_variables pcfv
                                           on pcfv.price_component_id = pc.id
                             where spc.service_detail_id = sd.id
                               and pcfv.value is null)
              and not exists
                (select 1
                 from service.service_interim_advance_payments siap
                          join
                      interim_advance_payment.interim_advance_payments iap
                      on siap.interim_advance_payment_id = iap.id
                          and siap.service_detail_id = sd.id
                          and siap.status = 'ACTIVE'
                          and iap.status = 'ACTIVE'
                          join price_component.price_components pc
                               on iap.price_component_id = pc.id
                                   and pc.status = 'ACTIVE'
                          join price_component.price_component_formula_variables pcfv
                               on pcfv.price_component_id = pc.id
                                   and pcfv.value is null)
            order by name
            """, nativeQuery = true)
    Page<ServiceContractServiceListingMiddleResponse> searchForContractForExpressContract(@Param("customerDetailId") Long customerId,@Param("userName") String username, @Param("prompt") String prompt, PageRequest customerIdentifier);

    @Query("""
            select count(scd.id) > 0
            from EPService s
            join ServiceDetails sd on s.id = sd.service.id
            join ServiceContractDetails scd on scd.serviceDetailId = sd.id
            join ServiceContracts sc on sc.id = scd.contractId
            where s.id = :id
            and sc.status = 'ACTIVE'
            """)
    boolean hasConnectionToServiceContract(Long id);

    @Query("""
            select count(so.id) > 0
            from EPService s
            join ServiceDetails sd on s.id = sd.service.id
            join ServiceOrder so on so.serviceDetailId = sd.id
            where s.id = :id
            and s.status = 'ACTIVE'
            """)
    boolean hasConnectionToServiceOrder(Long id);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.billing.billingRun.condition.ConditionParameterResponse(
                            s.id,
                            concat(sd.name, ' (Version ', sd.version, ')')
                        )
                        from EPService s
                        join ServiceDetails sd on s.lastServiceDetailId = sd.id
                        where s.id in :ids
                    """
    )
    List<ConditionParameterResponse> findByIdIn(List<Long> ids);

    @Modifying
    @Query("delete from ServiceContractAdditionalParams scap where scap.serviceAdditionalParamId in (:params)")
    void deleteContractAdditionalParams(List<Long> params);

    @Query("""
                            select count(sd.id)>0 
                        from ServiceDetails sd
                        join ServiceSalesChannel ssc on ssc.serviceDetails.id =sd.id
                        join SalesChannel sc on sc.id=ssc.salesChannel.id
                        left join AccountManagerTag amt on amt.portalTagId=sc.portalTagId
                        join AccountManager am on am.id = amt.accountManagerId
                        where sd.id= :detailId
                        and am.userName =:loggedInUserId
                        and ssc.status='ACTIVE'
            """)
    boolean checkSegments(Long detailId, String loggedInUserId);
}
