package bg.energo.phoenix.repository.customer;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.customer.PortalTag;
import bg.energo.phoenix.model.response.nomenclature.contract.PortalTagResponse;
import bg.energo.phoenix.service.portal.PortalTagManagerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortalTagRepository extends JpaRepository<PortalTag, Long> {

    @Query("""
            select pt from PortalTag pt
            where lower(pt.name) like 'group-%'
            and pt.id=:id
            and pt.status=:status
            """)
    Optional<PortalTag> findPortalTagForGroup(Long id, EntityStatus status);


    Optional<PortalTag> findByIdAndStatus(Long id, EntityStatus status);

    List<PortalTag> findAllByStatus(EntityStatus status);

    @Query("""
            select count(pt.id)>0 from PortalTag pt
            where lower(pt.name) like 'group-%'
            and pt.id=:id
            and pt.status=:status
            """)
    boolean existsPortalTagForGroup(Long id, EntityStatus status);


    @Query("""
        select new bg.energo.phoenix.model.response.nomenclature.contract.PortalTagResponse(pt)
        from PortalTag pt
        where ((:withGroupPrefix = true  and ((:prompt is null and lower(pt.name) like 'group-%') or (:prompt is not null and lower(pt.name) like 'group-%' and lower(pt.name) like concat('%',lower(:prompt),'%')) ) )
        or (:withGroupPrefix = false and (:prompt is null or lower(pt.name) like concat('%',lower(:prompt),'%') or lower(pt.nameBg) like concat('%',lower(:prompt),'%') )))
       and pt.status = :status
        order by pt.name
""")
    Page<PortalTagResponse> findAllByPrefix(boolean withGroupPrefix,String prompt, EntityStatus status,Pageable pageable);

    @Query(value = """
                select 'TAG' as performerType ,pt.id as id ,pt.portal_id as portalId, pt.name as name, pt.name_bg as nameBg
              from customer.portal_tags pt 
            where ((:withGroupPrefix = true  and ((:prompt is null and lower(pt.name) like 'group-%') or (:prompt is not null and lower(pt.name) like 'group-%' and lower(pt.name) like concat('%',lower(:prompt),'%')) )  )
            or (:withGroupPrefix = false and (:prompt is null or lower(pt.name) like concat('%',lower(:prompt),'%') or lower(pt.name_bg) like concat('%',lower(:prompt),'%'))))
            and status='ACTIVE'
            union 
            select 'MANAGER' as tag ,am.id as id ,am.user_name as portalId,concat(am.display_name, ' (', am.user_name, ')') as name, concat(am.display_name, ' (', am.user_name, ')') as nameBg
            from customer.account_managers am
            where am.status='ACTIVE'
            and (:prompt is null or (
                    lower(am.user_name) like concat('%',lower(:prompt),'%')
                    or lower(am.last_name) like concat('%',lower(:prompt),'%')
                    or lower(am.first_name) like concat('%',lower(:prompt),'%')
                    or lower(am.display_name) like concat('%',lower(:prompt),'%')
                ))
        
                """, countQuery = """
                      select count(1)
                      from (
                               select pt.id
                               from customer.portal_tags pt
                               where ((:withGroupPrefix = true  and ((:prompt is null and lower(pt.name) like 'group-%') or (:prompt is not null and lower(pt.name) like concat('group-','%',lower(:prompt),'%')) )  )
                                   or (:withGroupPrefix = false and (:prompt is null or lower(pt.name) like concat('%',lower(:prompt),'%') or lower(pt.name_bg) like concat('%',lower(:prompt),'%'))))
                                 and status='ACTIVE'
                               union
                               select am.id
                               from customer.account_managers am
                               where am.status='ACTIVE'
                                 and (:prompt is null or (
                                   lower(am.user_name) like concat('%',lower(:prompt),'%')
                                       or lower(am.last_name) like concat('%',lower(:prompt),'%')
                                       or lower(am.first_name) like concat('%',lower(:prompt),'%')
                                       or lower(am.display_name) like concat('%',lower(:prompt),'%')
                                   ))
                           ) as bla
""",nativeQuery = true)
    Page<PortalTagManagerResponse> findAllByPrefixAndManagers(boolean withGroupPrefix, String prompt, Pageable pageable);

    @Query("""
            select amt.accountManagerId from PortalTag pt 
            join AccountManagerTag amt on amt.portalTagId=pt.id
            where pt.status=:status
            and pt.id=:performer
            """)
    List<Long> findManagerIdsByTagId(Long performer,EntityStatus status);
}
