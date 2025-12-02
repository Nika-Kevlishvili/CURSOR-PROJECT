package bg.energo.phoenix.repository.nomenclature.product.product;

import bg.energo.phoenix.model.entity.nomenclature.product.product.ProductGroups;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
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
public interface ProductGroupsRepository extends JpaRepository<ProductGroups, Long> {
    @Query(
            "select pg from ProductGroups as pg" +
                    " where (:prompt is null or lower(pg.name) like :prompt " +
                    " or (:prompt is null or lower(pg.nameTransliterated) like :prompt))" +
                    " and (pg.status in (:statuses))" +
                    " and (:excludedItemId is null or pg.id <> :excludedItemId) " +
                    " order by pg.defaultSelection desc, pg.orderingId asc"
    )
    Page<ProductGroups> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(pg.id, pg.name, pg.orderingId, pg.defaultSelection, pg.status) " +
                    "from ProductGroups as pg" +
                    " where (:prompt is null or lower(pg.name) like :prompt" +
                    " or (:prompt is null or lower(pg.nameTransliterated) like :prompt))" +
                    " and (:excludedItemId is null or pg.id <> :excludedItemId) " +
                    " and (pg.status in (:statuses))" +
                    " order by pg.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    Optional<ProductGroups> findByDefaultSelectionTrue();

    @Query("select max(pg.orderingId) from ProductGroups pg")
    Long findLastOrderingId();

    @Query(
            "select pg from ProductGroups as pg" +
                    " where pg.id <> :currentId " +
                    " and (pg.orderingId >= :start and pg.orderingId <= :end) "
    )
    List<ProductGroups> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query("""
            select pg from ProductGroups as pg
                where pg.orderingId is not null
                order by pg.name
            """
    )
    List<ProductGroups> orderByName();

    @Query("""
             select pg from ProductGroups pg
             where pg.id = :id and pg.status in(:statuses)
            """
    )
    Optional<ProductGroups> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("""
           select pg from ProductGroups pg
           where pg.name like(:name)
           and pg.status in(:statuses)
           """)
    List<ProductGroups> findByNameAndStatuses(@Param("name") String name,@Param("statuses") List<NomenclatureItemStatus> statuses);


    @Query("""
            select count(1) from  ProductGroups sa
             where sa.id = :id
             and
             ( exists
             (select 1 from Product p
               join ProductDetails pd on pd.product.id = p.id
                 and pd.productGroups.id = sa.id
               where
                 pd.productDetailStatus in (:productDetailStatuses)
                 and p.productStatus = 'ACTIVE'))
           """)
    Long activeConnectionCount(@Param("id") Long id,
                               @Param("productDetailStatuses") List<ProductDetailStatus> productDetailStatuses);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            p.id,
                            p.name
                        )
                        from ProductGroups p
                        where p.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
