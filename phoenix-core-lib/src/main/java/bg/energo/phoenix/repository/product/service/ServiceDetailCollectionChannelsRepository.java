package bg.energo.phoenix.repository.product.service;

import bg.energo.phoenix.model.entity.product.service.ServiceDetailCollectionChannels;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceDetailCollectionChannelsRepository extends JpaRepository<ServiceDetailCollectionChannels, Long> {

    @Query(
            """
                       select pdc.collectionChannelId from ServiceDetailCollectionChannels pdc
                       where pdc.serviceDetailsId = :serviceDetailId
                    """
    )
    List<Long> getCollectionChannelIdsByServiceDetailId(Long serviceDetailId);

    void deleteByCollectionChannelIdInAndServiceDetailsId(List<Long> collectionChannelId, Long serviceDetailId);

    @Query("""
            select new bg.energo.phoenix.model.response.shared.ShortResponse(
                collectionChannel.id,
                collectionChannel.name
            )
            from ServiceDetailCollectionChannels pdc
            join CollectionChannel collectionChannel on collectionChannel.id = pdc.collectionChannelId
            where pdc.serviceDetailsId = :detailId
            """
    )
    List<ShortResponse> getByDetailId(Long detailId);
}
