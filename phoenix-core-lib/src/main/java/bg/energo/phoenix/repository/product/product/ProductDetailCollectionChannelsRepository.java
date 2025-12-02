package bg.energo.phoenix.repository.product.product;

import bg.energo.phoenix.model.entity.product.product.ProductDetailCollectionChannels;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductDetailCollectionChannelsRepository extends JpaRepository<ProductDetailCollectionChannels, Long> {

    @Query(
            """
                       select pdc.collectionChannelId from ProductDetailCollectionChannels pdc
                       where pdc.productDetailsId = :productDetailId
                    """
    )
    List<Long> getCollectionChannelIdsByProductDetailId(Long productDetailId);

    void deleteByCollectionChannelIdInAndProductDetailsId(List<Long> collectionChannelId, Long productDetailId);

    @Query("""
            select new bg.energo.phoenix.model.response.shared.ShortResponse(
                collectionChannel.id,
                collectionChannel.name
            )
            from ProductDetailCollectionChannels pdc
            join CollectionChannel collectionChannel on collectionChannel.id = pdc.collectionChannelId
            where pdc.productDetailsId = :detailId
            """
    )
    List<ShortResponse> getByDetailId(Long detailId);
}
