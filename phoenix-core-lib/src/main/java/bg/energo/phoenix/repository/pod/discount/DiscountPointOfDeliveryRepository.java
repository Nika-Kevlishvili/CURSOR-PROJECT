package bg.energo.phoenix.repository.pod.discount;

import bg.energo.phoenix.model.entity.pod.discount.DiscountPointOfDeliveries;
import bg.energo.phoenix.model.response.pod.discount.DiscountPointOfDeliveryShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscountPointOfDeliveryRepository extends JpaRepository<DiscountPointOfDeliveries, Long> {

    @Query("""
            select new bg.energo.phoenix.model.response.pod.discount.DiscountPointOfDeliveryShortResponse(
                pod.id,
                podd.name,
                pod.identifier
            )
            from DiscountPointOfDeliveries dpd
            join Discount d on dpd.discountId = d.id
            join PointOfDelivery pod on dpd.pointOfDeliveryId = pod.id
            join PointOfDeliveryDetails podd on pod.lastPodDetailId = podd.id
            where d.id = :discountId
            and dpd.status = 'ACTIVE'
            order by dpd.createDate
            """)
    List<DiscountPointOfDeliveryShortResponse> findAllActiveDiscountPointOfDeliveriesByDiscountId(@Param("discountId") Long discountId);


    @Query("""
            select dpd from DiscountPointOfDeliveries dpd
            join Discount d on dpd.discountId = d.id
            join PointOfDelivery pod on dpd.pointOfDeliveryId = pod.id
            join PointOfDeliveryDetails podd on podd.podId = pod.id
            where d.id = :discountId
            """)
    List<DiscountPointOfDeliveries> findAllActivePointOfDeliveriesByDiscountId(@Param("discountId") Long discountId);

}
