package bg.energo.phoenix.service.receivable.collectionChannel;

import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.PaymentFTPFiles;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.PaymentFTPFilesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class CollectionChannelHelperService {
    private final CollectionChannelRepository collectionChannelRepository;
    private final PaymentFTPFilesRepository paymentFTPFilesRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAll(Collection<CollectionChannel> collectionChannels) {
        collectionChannelRepository.saveAll(collectionChannels);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(PaymentFTPFiles paymentFTPFiles) {
        paymentFTPFilesRepository.save(paymentFTPFiles);
    }
}
