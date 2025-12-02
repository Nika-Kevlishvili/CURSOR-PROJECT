package bg.energo.phoenix.repository.pod.meter;

import bg.energo.phoenix.model.entity.pod.meter.MeterScale;
import bg.energo.phoenix.model.enums.pod.PodSubObjectStatus;
import bg.energo.phoenix.model.response.pod.meter.MeterScaleResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeterScaleRepository extends JpaRepository<MeterScale, Long> {

    List<MeterScale> findByMeterIdAndStatusIn(Long meterId, List<PodSubObjectStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.pod.meter.MeterScaleResponse(
                        s.id,
                        s.name
                    )
                    from MeterScale ms
                    join Scales s on s.id = ms.scaleId
                        where ms.meterId = :meterId
                        and ms.status in :statuses
                    """
    )
    List<MeterScaleResponse> findAllByMeterIdAndStatusIn(
            @Param("meterId") Long meterId,
            @Param("statuses") List<PodSubObjectStatus> statuses
    );

    Optional<List<MeterScale>> findByMeterIdAndScaleIdAndStatus(Long meterId, Long scaleId, PodSubObjectStatus status);

}
