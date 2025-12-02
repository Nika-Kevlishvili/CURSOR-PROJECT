package bg.energo.phoenix.repository.product.term.termsGroups;

import bg.energo.phoenix.model.entity.product.term.termsGroups.TermGroupDetails;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TermsGroupDetailsRepository extends JpaRepository<TermGroupDetails, Long> {

    Optional<TermGroupDetails> findByGroupIdAndVersionId(Long id, Long version);

    Optional<TermGroupDetails> findFirstByGroupIdOrderByVersionIdDesc(Long id);

    Optional<List<TermGroupDetails>> findByGroupId(Long id);
    Optional<List<TermGroupDetails>> findByGroupIdOrderByStartDateAsc(Long id);

    @Query("""
            select max(tgd.versionId) FROM TermGroupDetails tgd where tgd.groupId = :groupId
            """)
    Optional<Long> findMaxVersionByGroupId(Long groupId);

    Optional<TermGroupDetails> findByGroupIdAndStartDate(Long groupId, LocalDateTime startDate);

    List<TermGroupDetails> findAllByGroupId(Long groupId);

    @Query("""
            select new bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse(tgd.id,tgd.versionId,tgd.startDate)
            from TermsGroups t
            join TermGroupDetails tgd on t.id = tgd.groupId
            where t.id=:id
            order by tgd.startDate ASC
            """)
    List<CopyDomainWithVersionMiddleResponse> findByCopyDomainWithVersionBaseRequest(@Param("id") Long id);

    Optional<TermGroupDetails> findByVersionIdAndGroupId(Long id,Long groupId);

    Optional<TermGroupDetails> findFirstByGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(Long termGroupId, LocalDateTime startDate);
}
