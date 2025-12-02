package bg.energo.phoenix.repository.receivable.massOperationForBlocking;

import bg.energo.phoenix.model.entity.receivable.massOperationForBlocking.ReceivableBlockingExclusionPrefix;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.PrefixesShortResponse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReceivableBlockingExclusionPrefixesRepository extends JpaRepository<ReceivableBlockingExclusionPrefix, Long> {

    @Query("""
            select new bg.energo.phoenix.model.response.receivable.massOperationForBlocking.PrefixesShortResponse(prefix.id, prefix.name)
            from ReceivableBlockingExclusionPrefix exclusionPrefix
            join Prefix prefix on prefix.id = exclusionPrefix.prefixId
            where exclusionPrefix.receivableBlockingId = :blockingId
            and exclusionPrefix.status = :status
            """
    )
    Optional<List<PrefixesShortResponse>> findPrefixesByBlockingId(@Param("blockingId") Long blockingId, @Param("status") ReceivableSubObjectStatus status);

    Optional<List<ReceivableBlockingExclusionPrefix>> findByReceivableBlockingIdAndStatus(Long blockingId, ReceivableSubObjectStatus status);

}
