package bg.energo.phoenix.repository.receivable.reminder;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.reminder.OnlyLiabilitiesWithPrefix;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.PrefixesShortResponse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OnlyLiabilitiesWithPrefixRepository extends JpaRepository<OnlyLiabilitiesWithPrefix, Long> {

    @Query("""
            select new bg.energo.phoenix.model.response.receivable.massOperationForBlocking.PrefixesShortResponse(prefix.id, prefix.name)
            from OnlyLiabilitiesWithPrefix olwp
            join Prefix prefix on prefix.id = olwp.prefixId
            where olwp.reminderId = :reminderId
            and olwp.status = :status
            """
    )
    Optional<List<PrefixesShortResponse>> findOnlyLiabilitiesPrefixesByReminderId(@Param("reminderId") Long reminderId, @Param("status") EntityStatus status);

    Optional<List<OnlyLiabilitiesWithPrefix>> findByReminderIdAndStatus(Long reminderId, EntityStatus status);

}
