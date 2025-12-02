package bg.energo.phoenix.repository.receivable.collectionChannel;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannelBanks;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionChannelBanksRepository extends JpaRepository<CollectionChannelBanks, Long> {
    Optional<List<CollectionChannelBanks>> findByCollectionChannelIdAndStatus(Long id, EntityStatus status);

    @Query(
            """
                    select new bg.energo.phoenix.model.response.shared.ShortResponse(bank.id, bank.name)
                    from CollectionChannelBanks collectionChannelBank
                    join Bank bank on bank.id = collectionChannelBank.bankId
                    where collectionChannelBank.collectionChannelId = :id
                    and collectionChannelBank.status = :status
                    """
    )
    Optional<List<ShortResponse>> findBankIdsByCollectionChannelIdAndStatus(
            @Param("id") Long id,
            @Param("status") EntityStatus status
    );
}
