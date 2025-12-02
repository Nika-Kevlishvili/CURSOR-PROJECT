package bg.energo.phoenix.repository.product.termination.terminationGroup;

import bg.energo.phoenix.model.entity.product.termination.terminationGroup.TerminationGroupDetails;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse;
import bg.energo.phoenix.model.response.terminationGroup.TerminationGroupVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TerminationGroupDetailsRepository extends JpaRepository<TerminationGroupDetails, Long> {

    @Query(
        value = """
            select new bg.energo.phoenix.model.response.terminationGroup.TerminationGroupVersion(
                terminationGroupDetails.versionId,
                terminationGroupDetails.startDate
            )
            from TerminationGroupDetails terminationGroupDetails
                where terminationGroupDetails.terminationGroupId = :groupId
                order by terminationGroupDetails.startDate asc
        """
    )
    List<TerminationGroupVersion> getTerminationGroupVersions(
            @Param("groupId") Long groupId
    );

    Optional<TerminationGroupDetails> findFirstByTerminationGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(Long terminationGroupId, LocalDate startDate);

    Optional<TerminationGroupDetails> findByTerminationGroupIdAndVersionId(Long terminationGroupId, Long versionId);

    Optional<TerminationGroupDetails> findByTerminationGroupIdAndStartDate(Long terminationGroupId, LocalDate startDate);

    @Query("select max(tgd.versionId) from TerminationGroupDetails tgd where tgd.terminationGroupId = :terminationGroupId")
    Long findLastVersionByTerminationGroupId(@Param("terminationGroupId") Long terminationGroupId);

    @Query("""
            select new bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse(tgd.id,tgd.versionId,tgd.startDate)
            from TerminationGroupDetails tgd
            where tgd.terminationGroupId =:id
            order by tgd.startDate ASC
            """)
    List<CopyDomainWithVersionMiddleResponse> findByCopyGroupBaseRequest(@Param("id") Long id);

    Optional<TerminationGroupDetails> findByVersionIdAndTerminationGroupId(Long id,Long groupId);

}
