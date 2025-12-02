package bg.energo.phoenix.repository.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.UnwantedCustomerReason;
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

public interface UnwantedCustomerReasonRepository extends JpaRepository<UnwantedCustomerReason, Long> {
    /**
     * <h1>Unwanted customer reason filter</h1>
     * @param prompt search key
     * @param statuses array of Nomenclature statuses
     * @param excludedItemId excluded item id
     * @param pageable pagination object
     * @return returns paginated list of unwanted customer reason
     */
    @Query(
            "select c from UnwantedCustomerReason as c" +
                    " where (:prompt is null or lower(c.name) like :prompt)" +
                    " and (c.status in (:statuses))" +
                    " and (:excludedItemId is null or c.id <> :excludedItemId) " +
                    " order by c.defaultSelection desc, c.orderingId asc"
    )
    Page<UnwantedCustomerReason> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    /**
     * <h1>Unwanted customer reason filter nomenclature</h1>
     * @param prompt search key which searches in UnwantedCustomerReason nomenclature name
     * @param statuses nomenclature statuses array
     * @param pageable pagination object
     * @return list of paginated full nomenclature objects
     */
    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(c.id, c.name, c.orderingId, c.defaultSelection, c.status) " +
                    "from UnwantedCustomerReason as c" +
                    " where (:prompt is null or lower(c.name) like :prompt)" +
                    " and (c.status in (:statuses))" +
                    " order by c.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    /**
     * <h1>Unwanted customer reason findByDefaultSelection</h1>
     * returns unwantedCustomerReason nomenclature which has default selection true
     * @return UnwantedCustomerReason object
     */
    Optional<UnwantedCustomerReason> findByDefaultSelectionTrue();

    /**
     * <h1>Unwanted customer reason findLastOrderingId</h1>
     * @return returns id of last ordering id
     */
    @Query("select max(c.orderingId) from UnwantedCustomerReason c")
    Long findLastOrderingId();

    /**
     * <h1>Unwanted customer reason findInOrderingIdRange</h1>
     * @param start
     * @param end
     * @param currentId
     * @param sort
     * @return sorted UnwantedCustomerReason objects list
     */
    @Query(
            "select c from UnwantedCustomerReason as c" +
                    " where c.id <> :currentId " +
                    " and (c.orderingId >= :start and c.orderingId <= :end) "
    )
    List<UnwantedCustomerReason> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    /**
     * <h1>Unwanted customer reason orderByName</h1>
     * @return ordered UnwantedCustomerReason objects list
     */
    @Query(
            "select c from UnwantedCustomerReason as c" +
                    " where c.orderingId is not null" +
                    " order by c.name"
    )
    List<UnwantedCustomerReason> orderByName();

    /**
     * <h1>Unwanted customer reason findByIdAndStatusIn</h1>
     * @param id
     * @param status nomenclature statues list
     * @return Object of unwanted customer reason
     */
    Optional<UnwantedCustomerReason> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> status);

    /**
     * <h1>Unwanted customer reason getActiveConnectionsCount</h1>
     * @param id unwanted customer reason id
     * @return returns count of active connections
     */
    @Query(
            """
            select count(1) from UnwantedCustomerReason ucr
                where ucr.id = :id
                and exists (select 1 from UnwantedCustomer uc
                    where uc.unwantedCustomerReasonId = :id
                    and uc.status = 'ACTIVE')
            """
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );

    @Query(
            """
            select count(c.id) from UnwantedCustomerReason c
                where lower(c.name) = lower(:name)
                and c.status in (:statuses)
            """
    )
    Long countUnwantedCustomerReasonByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    Optional<UnwantedCustomerReason> findByNameAndStatus(
            String name, NomenclatureItemStatus status
    );

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            u.id,
                            u.name
                        )
                        from UnwantedCustomerReason u
                        where u.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
