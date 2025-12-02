package bg.energo.phoenix.repository.nomenclature.pod;

import bg.energo.phoenix.model.entity.nomenclature.pod.Profiles;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfilesRepository extends JpaRepository<Profiles, Long> {
    @Query(
            "select p from Profiles as p" +
                    " where (:prompt is null or lower(p.name) like :prompt)" +
                    " and ((p.status in (:statuses)" +
                    " and (:excludedItemId is null or p.id <> :excludedItemId)" +
                    " and (coalesce(:excludeHardcodedValues, '0') = '0' or (p.isHardCoded <> :excludeHardcodedValues)))" +
                    " or p.id in (:includedItemIds)) " +
                    " ORDER BY case when p.id in (:includedItemIds) then 1 else 2 end," +
                    " case when p.isHardCoded = true then 0 when p.isDefault = true then 1 else 2 END, p.orderingId ASC"
    )
    Page<Profiles> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            @Param("includedItemIds") List<Long> includedItemIds,
            @Param("excludeHardcodedValues") Boolean excludeHardcodedValues,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(p.id, p.name, p.orderingId, p.isDefault, p.status) " +
                    "from Profiles as p" +
                    " where (:prompt is null or lower(p.name) like :prompt)" +
                    " and (p.status in (:statuses))" +
                    " order by p.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<Profiles> findByIsDefaultTrue();

    @Query("select max(p.orderingId) from Profiles p")
    Long findLastOrderingId();

    @Query(
            "select p from Profiles as p" +
                    " where p.id <> :currentId " +
                    " and (p.orderingId >= :start and p.orderingId <= :end) "
    )
    List<Profiles> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select p from Profiles as p" +
                    " where p.orderingId is not null" +
                    " order by p.name"
    )
    List<Profiles> orderByName();

    @Query(
            """
                                select p from Profiles p
                                where p.id = :id
                                and p.status in :statuses
                    """
    )
    Optional<Profiles> findByIdAndStatus(@Param("id") Long id, @Param("statuses") List<NomenclatureItemStatus> statuses);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            """
                    select count(1) from Profiles as p
                    where lower(p.name) = :name and p.status in :statuses
                    """
    )
    Integer getExistingRecordsCountByName(@Param("name") String name, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query(
            """
                    select p from Profiles as p
                    where (:prompt is null or lower(p.name) like :prompt)
                    and (p.status in (:statuses))
                    and (:excludedItemId is null or p.id <> :excludedItemId) 
                    and p.isHardCoded = false 
                    """
    )
    Page<Profiles> listForApplicationModel(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            p.id,
                            p.name
                        )
                        from Profiles p
                        where p.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query(
            value = """
                        select p.id from Profiles p
                        where p.id in :ids
                        and p.status in :statuses
                    """
    )
    List<Long> findProfileIdsByIdInAndStatus(@Param("ids") List<Long> ids,
                                             @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("""
            select count(1)
             from Profiles p
            where p.id = :profileid
              and
               (exists(select 1
                       from BillingByProfile bbp
                      where bbp.profileId = p.id
                        and bbp.status = 'ACTIVE'
                      )
                   or
            	exists(select 1
                       from SettlementPeriodsProfiles am
                      where am.profileId = p.id
                        and am.status = 'ACTIVE'
               )
               )
            """)
    Integer canDeleteProfile(@Param("profileid")Long id);
}
