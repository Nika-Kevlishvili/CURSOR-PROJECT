package bg.energo.phoenix.repository.nomenclature.billing;

import bg.energo.phoenix.model.entity.nomenclature.billing.IncomeAccountName;
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
public interface IncomeAccountNameRepository extends JpaRepository<IncomeAccountName, Long> {

    @Query(
            "select b from IncomeAccountName as b" +
                    " where (:prompt is null or (" +
                    " lower(b.name) like :prompt or " +
                    " lower(b.number) like :prompt" +
                    "))" +
                    " and (b.status in (:statuses))" +
                    " and (:excludedItemId is null or b.id <> :excludedItemId) " +
                    " order by b.defaultSelection desc, b.orderingId asc"
    )
    Page<IncomeAccountName> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(" +
                    "b.id," +
                    " CONCAT(b.name, ' - ', b.number), " +
                    " b.orderingId," +
                    " b.defaultSelection," +
                    " b.status" +
                    ") " +
                    "from IncomeAccountName as b" +
                    " where (:prompt is null or (" +
                    " lower(b.name) like :prompt or " +
                    " lower(b.number) like :prompt" +
                    "))" +
                    " and (b.status in (:statuses))" +
                    " order by b.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );


    @Query(
            "select b from IncomeAccountName as b" +
                    " where b.id <> :currentId " +
                    " and (b.orderingId >= :start and b.orderingId <= :end) "
    )
    List<IncomeAccountName> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select b from IncomeAccountName as b" +
                    " where b.orderingId is not null" +
                    " order by b.name"
    )
    List<IncomeAccountName> orderByName();

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            b.id,
                            b.name
                        )
                        from IncomeAccountName b
                        where b.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query(
            """
                    select count(b.id) from IncomeAccountName b
                        where lower(b.number) = lower(:name)
                        and b.status in (:statuses)
                    """
    )
    Long countNumberByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );


    @Query("select max(b.orderingId) from IncomeAccountName b")
    Long findLastOrderingId();

    Optional<IncomeAccountName> findByDefaultSelectionTrue();

    @Query(nativeQuery = true, value =
            """
                     SELECT b.name
                     FROM nomenclature.income_account b
                     WHERE ((:defaultAssignmentType)::nomenclature.default_assignment_type = ANY (b.default_assignment_type)
                         or 'ALL'::nomenclature.default_assignment_type = ANY (b.default_assignment_type))
                     and b.status = 'ACTIVE'
                    """
    )
    String findByDefaultAssignmentType(String defaultAssignmentType);

    @Query(nativeQuery = true,
            value = """
                    SELECT b.number
                     FROM nomenclature.income_account b
                     WHERE ((:defaultAssignmentType)::nomenclature.default_assignment_type = ANY (b.default_assignment_type)
                            or 'ALL'::nomenclature.default_assignment_type = ANY (b.default_assignment_type))
                     and b.status = 'ACTIVE'
                    """
    )
    String findNumberByDefaultAssignmentType(String defaultAssignmentType);

    @Query(nativeQuery = true,
            value = """
                    SELECT b.number
                     FROM nomenclature.income_account b
                     WHERE ((:defaultAssignmentType)::nomenclature.default_assignment_type = ANY (b.default_assignment_type)
                            or 'ALL'::nomenclature.default_assignment_type = ANY (b.default_assignment_type))
                     and b.status = 'ACTIVE'
                    """
    )
    Optional<String> findNumberByDefaultAssignmentTypeOptional(String defaultAssignmentType);

    @Query(nativeQuery = true, value = """
            select b.name
            from nomenclature.income_account b
            where b.default_assignment_type is not null
              and array_length(b.default_assignment_type::text[], 1) > 0
            """)
    List<String> findWhereDefaultAssignmentTypeIsNotNull();

    Optional<IncomeAccountName> findByName(String name);

}
