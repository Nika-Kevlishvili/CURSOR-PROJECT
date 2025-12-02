package bg.energo.phoenix.repository.pod.pod;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDeliveryDetailsAdditionalParameters;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointOfDeliveryDetailsAdditionalParametersRepository extends JpaRepository<PointOfDeliveryDetailsAdditionalParameters, Long> {

    List<PointOfDeliveryDetailsAdditionalParameters> findAllByPodDetailIdAndStatus(Long podDetailId, EntityStatus status);

    @Query("""
            select new bg.energo.phoenix.model.response.shared.ShortResponse(
                  pap.id,
                  pap.name
            )
            from PointOfDeliveryDetailsAdditionalParameters pddap
            join PodAdditionalParameters pap on pddap.podAdditionalParamId = pap.id
            where pddap.podDetailId = :podDetailId
            and pddap.status='ACTIVE'
            """
    )
    List<ShortResponse> findAllAdditionalParametersByPodDetailId(Long podDetailId);

}
