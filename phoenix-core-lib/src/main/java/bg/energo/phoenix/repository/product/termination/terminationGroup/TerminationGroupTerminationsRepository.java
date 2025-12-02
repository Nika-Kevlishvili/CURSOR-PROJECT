package bg.energo.phoenix.repository.product.termination.terminationGroup;

import bg.energo.phoenix.model.entity.product.termination.terminationGroup.TerminationGroupTermination;
import bg.energo.phoenix.model.enums.product.termination.terminationGroup.TerminationGroupTerminationStatus;
import bg.energo.phoenix.model.response.terminationGroup.TerminationGroupTerminationResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TerminationGroupTerminationsRepository extends JpaRepository<TerminationGroupTermination, Long> {

    @Query(
            value = """
            select new bg.energo.phoenix.model.response.terminationGroup.TerminationGroupTerminationResponse(
                terminationGroupTermination.id,
                terminationGroupTermination.terminationId,
                termination.name
            ) 
            from TerminationGroupTermination as terminationGroupTermination
            join Termination as termination on terminationGroupTermination.terminationId = termination.id
                where terminationGroupTermination.terminationGroupDetailId = :terminationGroupDetailId
                and terminationGroupTermination.status in (:statuses)
        """
    )
    List<TerminationGroupTerminationResponse> findByTerminationGroupDetailIdAndStatusIn(
            @Param("terminationGroupDetailId") Long terminationGroupDetailId,
            @Param("statuses") List<TerminationGroupTerminationStatus> statuses
    );

    @Query(
            value = """
            select tgt from TerminationGroupTermination tgt
            join TerminationGroupDetails tgd on tgd.id = tgt.terminationGroupDetailId
                where tgd.terminationGroupId = :terminationGroupId
                and tgd.versionId = :terminationGroupVersion
                and tgt.status in (:statuses)
        """
    )
    List<TerminationGroupTermination> findByTerminationGroupVersionAndStatusIn(
            @Param("terminationGroupId") Long terminationGroupId,
            @Param("terminationGroupVersion") Long terminationGroupVersion,
            @Param("statuses") List<TerminationGroupTerminationStatus> statuses
    );

    boolean existsByTerminationIdAndStatus(Long terminationId, TerminationGroupTerminationStatus status);

}