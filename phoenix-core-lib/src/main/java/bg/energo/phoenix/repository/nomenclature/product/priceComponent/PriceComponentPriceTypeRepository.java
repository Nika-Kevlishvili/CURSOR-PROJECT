package bg.energo.phoenix.repository.nomenclature.product.priceComponent;

import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.PriceComponentPriceType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentStatus;
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

public interface PriceComponentPriceTypeRepository extends JpaRepository<PriceComponentPriceType, Long> {
    @Query(
            "select pcpt from PriceComponentPriceType as pcpt" +
            " where (:prompt is null or lower(pcpt.name) like :prompt)" +
            " and (pcpt.status in (:statuses))" +
            " and (:excludedItemId is null or pcpt.id <> :excludedItemId) " +
            " order by pcpt.isHardcoded, pcpt.isDefault, pcpt.orderingId asc"
    )
    Page<PriceComponentPriceType> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(pcpt.id, pcpt.name, pcpt.orderingId, pcpt.isDefault, pcpt.status) " +
            "from PriceComponentPriceType as pcpt" +
            " where (:prompt is null or lower(pcpt.name) like :prompt)" +
            " and (pcpt.status in (:statuses))" +
            " order by pcpt.isHardcoded, pcpt.isDefault, pcpt.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<PriceComponentPriceType> findByIsDefaultTrue();

    @Query("select max(pcpt.orderingId) from PriceComponentPriceType pcpt")
    Long findLastOrderingId();

    @Query(
            "select pcpt from PriceComponentPriceType as pcpt" +
            " where pcpt.id <> :currentId " +
            " and (pcpt.orderingId >= :start and pcpt.orderingId <= :end) "
    )
    List<PriceComponentPriceType> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select pcpt from PriceComponentPriceType as pcpt" +
            " where pcpt.orderingId is not null" +
            " order by pcpt.name"
    )
    List<PriceComponentPriceType> orderByName();

    @Query(
            """
                                select pcpt from PriceComponentPriceType pcpt
                                where pcpt.id = :id
                                and pcpt.status in :statuses
                    """
    )
    Optional<PriceComponentPriceType> findByIdAndStatus(@Param("id") Long id, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query(
            """
                    select count(1) from PriceComponentPriceType as pcpt
                    where lower(pcpt.name) = :name and pcpt.status in :statuses
                    """
    )
    Integer getExistingRecordsCountByName(@Param("name") String name, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("""
             select count(1) from  PriceComponentPriceType sa
                 where sa.id = :id
                 and
                 ( exists
                 (select 1 from PriceComponent pc
                   where
                    pc.priceType.id  = sa.id
                    and
                    pc.status in (:priceComponentStatuses)))
            """)
    Long activeConnectionCount(@Param("id") Long id,
                               @Param("priceComponentStatuses") List<PriceComponentStatus> priceComponentStatuses);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            p.id,
                            p.name
                        )
                        from PriceComponentPriceType p
                        where p.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
