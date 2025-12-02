package bg.energo.phoenix.repository.nomenclature.contract;

import bg.energo.phoenix.model.entity.nomenclature.contract.BaseInterestRate;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BaseInterestRateRepository extends JpaRepository<BaseInterestRate, Long> {

    Optional<BaseInterestRate> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);


    @Query(
            value = """
                    select bir from BaseInterestRate as bir
                        where (:prompt is null or (bir.name like :prompt))
                        and bir.status in (:statuses)
                        and (:excludedItemId is null or bir.id <> :excludedItemId)
                        order by bir.defaultSelection desc, bir.orderingId asc
                    """
    )
    Page<BaseInterestRate> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );


    @Query(
            value = """
                    select bir from BaseInterestRate as bir
                        where (:prompt is null or (bir.name like :prompt))
                        and bir.status in (:statuses)
                        and (:excludedItemId is null or bir.id <> :excludedItemId)
                        order by bir.defaultSelection desc, bir.orderingId asc
                    """
    )
    Page<BaseInterestRate> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );


    Optional<BaseInterestRate> findByDefaultSelectionTrue();


    @Query("select max(bir.orderingId) from BaseInterestRate as bir")
    Long findLastOrderingId();


    @Query(
            value = """
                    select bir from BaseInterestRate bir
                        where bir.id <> :currentId
                        and bir.orderingId between :start and :end
                    """
    )
    List<BaseInterestRate> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );


    @Query(
            value = """
                    select bir from BaseInterestRate bir
                        where bir.orderingId is not null
                        order by bir.dateFrom asc
                    """
    )
    List<BaseInterestRate> orderByDateFrom();


    @Query(
            value = """
                    select count(bir) > 0 from BaseInterestRate bir
                        where bir.dateFrom = :dateFrom
                        and bir.status in (:statuses)
                        and (:id is null or bir.id <> :id)
                    """
    )
    boolean existsByDateFromAndStatusInAndIdNullOrIdNot(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("id") Long id
    );

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            b.id,
                            b.name
                        )
                        from BaseInterestRate b
                        where b.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
