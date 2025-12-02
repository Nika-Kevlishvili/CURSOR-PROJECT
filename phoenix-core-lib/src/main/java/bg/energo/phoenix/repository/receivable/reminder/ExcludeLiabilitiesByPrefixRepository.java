package bg.energo.phoenix.repository.receivable.reminder;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.reminder.ExcludeLiabilitiesByPrefix;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.PrefixesShortResponse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExcludeLiabilitiesByPrefixRepository extends JpaRepository<ExcludeLiabilitiesByPrefix, Long> {

    @Query("""
            select new bg.energo.phoenix.model.response.receivable.massOperationForBlocking.PrefixesShortResponse(prefix.id, prefix.name)
            from ExcludeLiabilitiesByPrefix elbp
            join Prefix prefix on prefix.id = elbp.prefixId
            where elbp.reminderId = :reminderId
            and elbp.status = :status
            """
    )
    Optional<List<PrefixesShortResponse>> findExcludeLiabilitiesPrefixesByReminderId(@Param("reminderId") Long reminderId, @Param("status") EntityStatus status);

    Optional<List<ExcludeLiabilitiesByPrefix>> findByReminderIdAndStatus(Long reminderId, EntityStatus status);

}
