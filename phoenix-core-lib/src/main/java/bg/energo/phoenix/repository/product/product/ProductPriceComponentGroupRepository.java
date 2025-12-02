package bg.energo.phoenix.repository.product.product;

import bg.energo.phoenix.model.entity.product.product.ProductPriceComponentGroups;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProductPriceComponentGroupRepository extends JpaRepository<ProductPriceComponentGroups, Long> {
    List<ProductPriceComponentGroups> findByProductDetailsIdAndProductSubObjectStatusIn(Long productDetailsId, List<ProductSubObjectStatus> statuses);


    @Query(value = """
            select price_component_id from(
            select
             pcgd.start_date,
             pcgd.price_component_group_id,
             pcgd.id as group_detail_id,
              coalesce(lead(pcgd.start_date, 1) OVER (partition by pcgd.price_component_group_id order by pcgd.start_date), date '9999-12-31') as next_date,
              pc.id  as price_component_id,
              pc.price_formula,
              pc.contract_template_tag
            from
            product.product_price_component_groups ppcg
            join
            price_component.price_component_groups pcg
              on ppcg.price_component_group_id  = pcg.id
              and PPCG.product_detail_id = :productDetailId
              and ppcg.status = 'ACTIVE'
              and pcg.status  = 'ACTIVE'
            join
            price_component.price_component_group_details pcgd
            on pcgd.price_component_group_id = pcg.id
             join
            price_component.pc_group_pcs pgp
              on pgp.price_component_group_detail_id = pcgd.id
             and pgp.status = 'ACTIVE'
              join
            price_component.price_components pc
              on pgp.price_component_id = pc.id
            and pc.status = 'ACTIVE') as tbl
            where :executionDate between tbl.start_date and tbl.next_date-1
            and tbl.price_formula not like '%PRICE_PROFILE%'
            and tbl.contract_template_tag = :contractTemplateTag""",
            nativeQuery = true
    )
    List<Long> getPriceComponentIdsFromRespectiveGroups(
            @Param("executionDate") LocalDate executionDate,
            @Param("contractTemplateTag") String tag,
            @Param("productDetailId") Long productDetailId);
}

