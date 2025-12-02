package bg.energo.phoenix.repository.receivable.collectionChannel;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannelExcludePrefix;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionChannelExcludePrefixRepository extends JpaRepository<CollectionChannelExcludePrefix, Long> {
    Optional<List<CollectionChannelExcludePrefix>> findByCollectionChannelIdAndStatus(Long id, EntityStatus status);

    @Query(
            """
                    select new bg.energo.phoenix.model.response.shared.ShortResponse(prefix.id, prefix.name)
                    from CollectionChannelExcludePrefix excludePrefix
                    join Prefix prefix on prefix.id = excludePrefix.prefixId
                    where excludePrefix.collectionChannelId = :id
                    and excludePrefix.status = :status
                    """
    )
    Optional<List<ShortResponse>> findExcludePrefixIdsByCollectionChannelIdAndStatus(
            @Param("id") Long id,
            @Param("status") EntityStatus status
    );
}
