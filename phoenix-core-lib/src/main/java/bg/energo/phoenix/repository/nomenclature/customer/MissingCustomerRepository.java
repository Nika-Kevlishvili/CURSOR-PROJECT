package bg.energo.phoenix.repository.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.MissingCustomer;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MissingCustomerRepository extends JpaRepository<MissingCustomer, Long> {

    @Query(
            "select ms from MissingCustomer as ms" +
                    " where (:prompt is null or lower(ms.name) like :prompt)" +
                    " and (ms.status in (:statuses))" +
                    " and (:excludedItemId is null or ms.id <> :excludedItemId) " +
                    " order by ms.isDefault desc, ms.orderingId asc"
    )
    Page<MissingCustomer> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(ms.id, ms.name, ms.orderingId, ms.isDefault, ms.status) " +
                    "from MissingCustomer as ms" +
                    " where (:prompt is null or lower(ms.name) like :prompt)" +
                    " and (ms.status in (:statuses))" +
                    " order by ms.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<MissingCustomer> findByIsDefaultTrue();

    @Query("select ms from MissingCustomer ms where ms.status = 'ACTIVE'and ms.isDefault = true")
    Optional<MissingCustomer> findDefaultSelection();

    @Query("select max(ms.orderingId) from MissingCustomer ms")
    Long findLastOrderingId();

    @Query(
            "select ms from MissingCustomer as ms" +
                    " where ms.id <> :currentId " +
                    " and (ms.orderingId >= :start and ms.orderingId <= :end) "
    )
    List<MissingCustomer> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select ms from MissingCustomer as ms" +
                    " where ms.orderingId is not null" +
                    " order by ms.name"
    )
    List<MissingCustomer> orderByName();

    @Query(
            """
                    select ms from MissingCustomer ms
                    where ms.id = :id
                    and ms.status in :statuses
                    """
    )
    Optional<MissingCustomer> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    @Query(
            """
                    select count(1) from MissingCustomer as ms
                    where lower(ms.name) = :name and ms.status in :statuses
                    """
    )
    Integer getExistingRecordsCountByName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                        ms.id,
                        ms.name
                    )
                    from MissingCustomer ms
                    where ms.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);
}
