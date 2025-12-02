package bg.energo.phoenix.repository.nomenclature.crm;

import bg.energo.phoenix.model.entity.nomenclature.crm.TopicOfCommunication;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TopicOfCommunicationRepository extends JpaRepository<TopicOfCommunication, Long> {

    @Query(
            """
                    select ra from TopicOfCommunication as ra
                        where ra.id<> :currentId
                        and (ra.orderingId >= :start and ra.orderingId <= :end)
                    """
    )
    List<TopicOfCommunication> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            """
                    select ra from TopicOfCommunication as ra
                        where ra.orderingId is not null
                        order by ra.name
                    """
    )
    List<TopicOfCommunication> orderByName();

    @Query(
            """
                    select count(1) from TopicOfCommunication ra
                        where lower(ra.name) = lower(:name)
                        and ra.status in :statuses
                    """
    )
    Long countTopicOfCommunicationsByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    Optional<TopicOfCommunication> findByDefaultSelectionTrue();

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            s.id,
                            s.name
                        )
                        from TopicOfCommunication s
                        where s.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query(
            """
                    select sa from TopicOfCommunication as sa
                        where (:prompt is null or (
                            lower(sa.name) like :prompt
                        ))
                        and sa.status in (:statuses)
                        and :excludedItemId is null or sa.id <> :excludedItemId
                        order by sa.orderingId asc
                    """
    )
    Page<TopicOfCommunication> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            """
                    select max(ra.orderingId) from TopicOfCommunication ra
                    """
    )
    Long findLastOrderingId();

    @Query(
            "select c from TopicOfCommunication as c" +
                    " where (:prompt is null or lower(c.name) like :prompt)" +
                    " and (((c.status in (:statuses))" +
                    " and (:excludedItemId is null or c.id <> :excludedItemId)) " +
                    " or (c.id in (:includedItemIds)))" +
                    " order by case when c.id in (:includedItemIds) then 1 else 2 end," +
                    " case when c.isHardcoded = true then 0 when c.defaultSelection = true then 1 else 2 END , c.orderingId asc"
    )
    Page<TopicOfCommunication> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            @Param("includedItemIds") List<Long> includedItemIds,
            Pageable pageable
    );

    Optional<TopicOfCommunication> findByIdAndStatus(Long id, NomenclatureItemStatus status);

    Optional<TopicOfCommunication> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> status);

    @Query("""
            select count(1) from TopicOfCommunication tc 
                                         where tc.id = :id 
                                         and  
                                          (
                                               exists (select 1 from EmailCommunication ec
                                               where ec.communicationTopicId = :id
                                               and cast(ec.entityStatus as string ) ='ACTIVE')
                                               or exists(
                                                    select 1 from SmsCommunication sc
                                                    where sc.communicationTopicId = :id
                                                    and cast(sc.status as string ) ='ACTIVE')
                                               )
                                         
            """)
    Long getActiveConnectionsCount(Long id);

    @Query("""
                    select tpc
                    from TopicOfCommunication tpc
                    where trim(both from tpc.name) = trim(both from :name)
                    and tpc.status = :status
                    and tpc.isHardcoded = true
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<TopicOfCommunication> findByNameAndStatusAndIsHardcodedTrue(String name, NomenclatureItemStatus status);

}
