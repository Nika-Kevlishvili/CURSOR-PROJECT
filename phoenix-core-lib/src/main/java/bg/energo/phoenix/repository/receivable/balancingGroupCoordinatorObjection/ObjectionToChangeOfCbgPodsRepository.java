package bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgPods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ObjectionToChangeOfCbgPodsRepository extends JpaRepository<ObjectionToChangeOfCbgPods, Long> {

    @Query("""
            select objPods.pod
            from ObjectionToChangeOfCbgPods objPods
            where objPods.objectionToCbg = :objectionToCbgId
            """)
    Set<Long> findPodsByObjectionToCbgId(Long objectionToCbgId);


    @Query("""
            select pod.id
            from PointOfDelivery pod
            where pod.identifier in (:podIdentifiers)
            and pod.id not in (:podIds)
            and pod.status = 'ACTIVE'
            """)
    List<Long> findPodIds(Set<String> podIdentifiers, Set<Long> podIds);

    @Query("""
            select pod.identifier
            from PointOfDelivery pod
            where pod.gridOperatorId = :gridOperatorId
            and pod.identifier in :podIdentifiers
            """)
    Set<String> filterPodIdentifiersByGridOperator(Long gridOperatorId, Set<String> podIdentifiers);

    @Query("""
            select cbgp.id
            from ObjectionToChangeOfCbgPods cbgp
            where cbgp.objectionToCbg = :cbgId
            """)
    Set<Long> findCbgPodIdsByCbgId(Long cbgId);

    @Query("""
            select cbgp.id
            from ObjectionToChangeOfCbgPods cbgp
            join ObjectionToChangeOfCbg cbg on cbgp.objectionToCbg = cbg.id
            where cbg.id != :cbgId
            and text(cbg.changeOfCbgStatus) = 'DRAFT'
            and cbgp.pod in (
                select cbgp1.pod
                from ObjectionToChangeOfCbgPods cbgp1
                where cbgp1.objectionToCbg = :cbgId
            )
            """)
    Set<Long> findCbgPodIdsInDraftObjections(Long cbgId);
}
