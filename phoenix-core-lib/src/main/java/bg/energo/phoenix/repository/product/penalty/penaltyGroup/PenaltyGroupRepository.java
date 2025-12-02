package bg.energo.phoenix.repository.product.penalty.penaltyGroup;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.penalty.penaltyGroups.PenaltyGroup;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse;
import bg.energo.phoenix.model.response.penaltyGroup.PenaltyGroupListResponse;
import bg.energo.phoenix.model.response.product.AvailableProductRelatedGroupEntityResponse;
import bg.energo.phoenix.model.response.service.AvailableServiceRelatedGroupEntityResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PenaltyGroupRepository extends JpaRepository<PenaltyGroup, Long> {

    @Query(
            nativeQuery = true,
            value = """
                        select
                            tbl.id as id,
                            tbl.name as name,
                            tbl.numPenalties as numPenalties,
                            tbl.dateOfCreation as dateOfCreation,
                            tbl.status as status
                        from (
                        select
                            pg.id as id,
                            pgd.name as name,
                            pgd.start_date as startDate,
                            (select count(1) from terms.penalty_group_penalties pgp where pgp.penalty_group_detail_id = pgd.id and pgp.status = 'ACTIVE') as numPenalties,
                            (select max(start_date) from terms.penalty_group_details tt where tt.penalty_group_id = pg.id and start_date <= now()) as currentstartdate,
                            pg.create_date as dateOfCreation,
                            pg.status as status
                        from
                          terms.penalty_group_details pgd
                          join terms.penalty_groups pg on pg.id = pgd.penalty_group_id
                            where pg.id in(
                                select pg.id from terms.penalty_group_details pgd
                                left join terms.penalty_group_penalties pgp on pgp.penalty_group_detail_id = pgd.id and pgp.status = 'ACTIVE'
                                left join terms.penalties p on pgp.penalty_id = p.id
                                join terms.penalty_groups pg on pg.id = pgd.penalty_group_id
                                where text(pg.status) in (:statuses)
                                           and (coalesce(:excludeVersion,'0') = '0' or
                                                 (:excludeVersion = 'PASTVERSION' and
                                                  pgd.start_date >=
                                                  (select max(start_date) from terms.penalty_group_details tt
                                                    where tt.penalty_group_id = pg.id and start_date <= current_date)
                                                 )
                                                or
                                                 (:excludeVersion = 'RESPECTVERSION' and
                                                  pgd.start_date <=
                                                  (select max(start_date) from terms.penalty_group_details tt
                                                    where tt.penalty_group_id = pg.id and start_date <= current_date)
                                                 )
                                                or
                                                 (:excludeVersion = 'PASTANDRESPECTVERSION' and
                                                  pgd.start_date =
                                                  (select max(start_date) from terms.penalty_group_details tt
                                                    where tt.penalty_group_id = pg.id and start_date <= current_date)
                                                 )                                                
                                                )
                                and (
                                    :searchBy is null
                                    or (:searchBy = 'ALL' and (lower(pgd.name) like :prompt or lower(p.name) like :prompt))
                                    or (
                                        (:searchBy = 'PENALTY_GROUP_NAME' and lower(pgd.name) like :prompt)
                                        or (:searchBy = 'PENALTY_NAME' and lower(p.name) like :prompt)
                                    )
                                )
                            )
                        ) as tbl
                        where startDate = currentstartdate
                    """,
            countQuery = """
                        select
                            count(1)
                        from (
                        select
                            pg.id as id,
                            pgd.name as name,
                            pgd.start_date as startDate,
                            (select count(1) from terms.penalty_group_penalties pgp where pgp.penalty_group_detail_id = pgd.id and pgp.status = 'ACTIVE') as numPenalties,
                            (select max(start_date) from terms.penalty_group_details tt where tt.penalty_group_id = pg.id and start_date <= now()) as currentstartdate,
                            pg.create_date as dateOfCreation,
                            pg.status as status
                        from
                          terms.penalty_group_details pgd
                          join terms.penalty_groups pg on pg.id = pgd.penalty_group_id
                            where pg.id in(
                                select pg.id from terms.penalty_group_details pgd
                                left join terms.penalty_group_penalties pgp on pgp.penalty_group_detail_id = pgd.id and pgp.status = 'ACTIVE'
                                left join terms.penalties p on pgp.penalty_id = p.id
                                join terms.penalty_groups pg on pg.id = pgd.penalty_group_id
                                where text(pg.status) in (:statuses)
                                           and (coalesce(:excludeVersion,'0') = '0' or
                                                 (:excludeVersion = 'PASTVERSION' and
                                                  pgd.start_date >=
                                                  (select max(start_date) from terms.penalty_group_details tt
                                                    where tt.penalty_group_id = pg.id and start_date <= current_date)
                                                 )
                                                or
                                                 (:excludeVersion = 'RESPECTVERSION' and
                                                  pgd.start_date <=
                                                  (select max(start_date) from terms.penalty_group_details tt
                                                    where tt.penalty_group_id = pg.id and start_date <= current_date)
                                                 )
                                                or
                                                 (:excludeVersion = 'PASTANDRESPECTVERSION' and
                                                  pgd.start_date =
                                                  (select max(start_date) from terms.penalty_group_details tt
                                                    where tt.penalty_group_id = pg.id and start_date <= current_date)
                                                 )                                                
                                                )
                                and (
                                    :searchBy is null
                                    or (:searchBy = 'ALL' and (lower(pgd.name) like :prompt or lower(p.name) like :prompt))
                                    or (
                                        (:searchBy = 'PENALTY_GROUP_NAME' and lower(pgd.name) like :prompt)
                                        or (:searchBy = 'PENALTY_NAME' and lower(p.name) like :prompt)
                                    )
                                )
                            )
                        ) as tbl
                        where startDate = currentstartdate
                    """
    )
    Page<PenaltyGroupListResponse> list(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("statuses") List<String> statuses,
            @Param("excludeVersion") String excludeVersion,
            Pageable pageable
    );

    Optional<PenaltyGroup> findByIdAndStatusIn(Long id, List<EntityStatus> status);

    @Query(
            """
            select new bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse(
                pg.id,
                concat(pgd.name, ' (', pg.id, ')')
            )
            from PenaltyGroup pg
            join PenaltyGroupDetails pgd on pgd.penaltyGroupId = pg.id
            where pg.id in (
                select distinct npg.id from PenaltyGroup npg
                join PenaltyGroupDetails npgd on npgd.penaltyGroupId = npg.id
                    where (:prompt is null or (lower(npgd.name) like :prompt or cast(npg.id as string) like :prompt))
                    and npg.status ='ACTIVE'
            )
            and pgd.startDate = (
                select max(pgd1.startDate) from PenaltyGroupDetails pgd1
                where pgd1.penaltyGroupId = pg.id
                and pgd1.startDate <= :currentDate
            )
            order by pg.createDate DESC
            """
    )
    Page<CopyDomainWithVersionBaseResponse> findByCopyGroupBaseRequest(
            String prompt,
            LocalDate currentDate,
            Pageable pageable
    );

    boolean existsByIdAndStatus(Long groupId, EntityStatus active);

    List<PenaltyGroup> findByIdInAndStatusIn(List<Long> ids, List<EntityStatus> statuses);


    @Query(
            value = """
            select count (pd.id) > 0 from ProductDetails pd
            join ProductPenaltyGroups ppg on pd.id = ppg.productDetails.id
                where pd.product.productStatus in (:productStatuses)
                and ppg.productSubObjectStatus in (:ppgStatuses)
                and ppg.penaltyGroup.id = :pgId
        """
    )
    boolean hasConnectionToProduct(
            @Param("pgId") Long pgId,
            @Param("productStatuses") List<ProductStatus> productStatuses,
            @Param("ppgStatuses") List<ProductSubObjectStatus> ppgStatuses
    );


    @Query(
            value = """
            select count (sd.id) > 0 from ServiceDetails sd
            join ServicePenaltyGroup spg on sd.id = spg.serviceDetails.id
                where sd.service.status in (:serviceStatuses)
                and spg.status in (:spgStatuses)
                and spg.penaltyGroup.id = :pgId
        """
    )
    boolean hasConnectionToService(
            @Param("pgId") Long pgId,
            @Param("serviceStatuses") List<ServiceStatus> serviceStatuses,
            @Param("spgStatuses") List<ServiceSubobjectStatus> spgStatuses
    );


    @Query(nativeQuery = true,
            value = """
                    select
                        tg.id,
                        tgd.name||' ('||tg.id||')' as name
                    from terms.penalty_groups tg
                    join terms.penalty_group_details tgd on tgd.penalty_group_id = tg.id
                        where tg.id in (
                            select tg1.id from terms.penalty_groups tg1
                            join terms.penalty_group_details tgd1 on tgd1.penalty_group_id = tg1.id
                                where (:prompt is null or lower(tgd1.name) like :prompt or cast(tg1.id as text) like :prompt)
                        )
                        and tg.status = 'ACTIVE'
                        and tgd.start_date = (
                            select max(tgd3.start_date) from terms.penalty_group_details tgd3
                                where tgd3.penalty_group_id = tg.id
                                and tgd3.start_date <= current_date
                        )
                    order by tg.create_date desc
            """,
            countQuery = """
                    select count(1) from terms.penalty_groups tg
                    join terms.penalty_group_details tgd on tgd.penalty_group_id = tg.id
                        where tg.id in (
                            select tg1.id from terms.penalty_groups tg1
                            join terms.penalty_group_details tgd1 on tgd1.penalty_group_id = tg1.id
                                where (:prompt is null or lower(tgd1.name) like :prompt or cast(tg1.id as text) like :prompt)
                        )
                        and tg.status = 'ACTIVE'
                        and tgd.start_date = (
                            select max(tgd3.start_date) from terms.penalty_group_details tgd3
                                where tgd3.penalty_group_id = tg.id
                                and tgd3.start_date <= current_date
                        )
                    """
    )
    Page<AvailableProductRelatedGroupEntityResponse> findAvailablePenaltyGroupsForProduct(
            @Param("prompt") String prompt,
            PageRequest pageRequest
    );


    @Query(nativeQuery = true,
            value = """
                    select
                        tg.id,
                        tgd.name||' ('||tg.id||')' as name
                    from terms.penalty_groups tg
                    join terms.penalty_group_details tgd on tgd.penalty_group_id = tg.id
                        where tg.id in (
                            select tg1.id from terms.penalty_groups tg1
                            join terms.penalty_group_details tgd1 on tgd1.penalty_group_id = tg1.id
                                where (:prompt is null or lower(tgd1.name) like :prompt or cast(tg1.id as text) like :prompt)
                        )
                        and tg.status = 'ACTIVE'
                        and tgd.start_date = (
                            select max(tgd3.start_date) from terms.penalty_group_details tgd3
                                where tgd3.penalty_group_id = tg.id
                                and tgd3.start_date <= current_date
                        )
                    order by tg.create_date desc
                    """,
            countQuery = """
                    select count(1) from terms.penalty_groups tg
                    join terms.penalty_group_details tgd on tgd.penalty_group_id = tg.id
                        where tg.id in (
                            select tg1.id from terms.penalty_groups tg1
                            join terms.penalty_group_details tgd1 on tgd1.penalty_group_id = tg1.id
                                where (:prompt is null or lower(tgd1.name) like :prompt or cast(tg1.id as text) like :prompt)
                        )
                        and tg.status = 'ACTIVE'
                        and tgd.start_date = (
                            select max(tgd3.start_date) from terms.penalty_group_details tgd3
                                where tgd3.penalty_group_id = tg.id
                                and tgd3.start_date <= current_date
                        )
                    """
    )
    Page<AvailableServiceRelatedGroupEntityResponse> findAvailablePenaltyGroupsForService(
            @Param("prompt") String prompt,
            Pageable pageable
    );

    @Query(nativeQuery = true,
            value = """
                    select coalesce(max('true'),'false') as is_connected from terms.penalty_groups pg  
                    where pg.id = :penaltygroupid and
                     (exists(select 1 
                    	from product.products p
                    	join product.product_details pd
                     			  on pd.product_id = p.id
                    	 and p.status = 'ACTIVE'
                    	join product.product_penalty_groups ppg
                    	  on ppg.product_detail_id = pd.id
                    	 and ppg.penalty_group_id = pg.id
                    	 and ppg.status = 'ACTIVE'
                    	join terms.penalty_group_details pgd
                    	  on pgd.penalty_group_id = pg.id
                    	join terms.penalty_group_penalties pgp  
                    	  on pgp.penalty_group_detail_id = pgd.id
                    	 and pgp.status = 'ACTIVE'
                    	join product_contract.contract_details cd 
                    	  on cd.product_detail_id =  pd.id
                    	join product_contract.contracts c
                    	  on cd.contract_id =  c.id
                    	 and c.status = 'ACTIVE')
                       or
                      exists(select 1 
                    	from service.services s
                    	join service.service_details sd
                     			  on sd.service_id = s.id
                    	 and s.status = 'ACTIVE'
                    	join service.service_penalty_groups spg
                    	  on spg.service_detail_id = sd.id
                    	 and spg.penalty_group_id = pg.id
                    	 and spg.status = 'ACTIVE'
                    	join terms.penalty_group_details pgd
                    	  on pgd.penalty_group_id = pg.id
                    	join terms.penalty_group_penalties pgp  
                    	  on pgp.penalty_group_detail_id = pgd.id
                    	 and pgp.status = 'ACTIVE'
                    	join service_contract.contract_details cd 
                    	  on cd.service_detail_id = sd.id
                    	join service_contract.contracts c
                    	  on cd.contract_id = c.id
                    	 and c.status = 'ACTIVE')
                       or
                      exists(select 1 
                    	from service.services s
                    	join service.service_details sd
                     			  on sd.service_id = s.id
                    	 and s.status = 'ACTIVE'
                    	join service.service_penalty_groups spg
                    	  on spg.service_detail_id = sd.id
                    	 and spg.penalty_group_id = pg.id
                    	 and spg.status = 'ACTIVE'
                    	join terms.penalty_group_details pgd
                    	  on pgd.penalty_group_id = pg.id
                    	join terms.penalty_group_penalties pgp  
                    	  on pgp.penalty_group_detail_id = pgd.id
                    	 and pgp.status = 'ACTIVE'
                    	join service_order.orders o 
                                on o.service_detail_id =  sd.id
                               and o.status = 'ACTIVE')
                     )
                    """
    )
    boolean hasLockedConnection(
            @Param("penaltygroupid") Long id
    );

}
