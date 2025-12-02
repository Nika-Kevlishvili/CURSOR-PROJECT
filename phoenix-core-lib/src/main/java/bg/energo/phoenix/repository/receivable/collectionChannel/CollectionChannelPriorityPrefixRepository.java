package bg.energo.phoenix.repository.receivable.collectionChannel;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannelPriorityPrefix;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionChannelPriorityPrefixRepository extends JpaRepository<CollectionChannelPriorityPrefix, Long> {
    Optional<List<CollectionChannelPriorityPrefix>> findByCollectionChannelIdAndStatus(Long id, EntityStatus status);

    @Query(
            """
                    select new bg.energo.phoenix.model.response.shared.ShortResponse(prefix.id, prefix.name)
                    from CollectionChannelPriorityPrefix priorityPrefix
                    join Prefix prefix on prefix.id = priorityPrefix.prefixId
                    where priorityPrefix.collectionChannelId = :id
                    and priorityPrefix.status = :status
                    """
    )
    Optional<List<ShortResponse>> findPriorityPrefixIdsByCollectionChannelIdAndStatus(
            @Param("id") Long id,
            @Param("status") EntityStatus status
    );

}
