package bg.energo.phoenix.service.signing;

import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SignerChainManager {

    private final Map<DocumentSigners, SignerChain> signerChainMap;

    public SignerChainManager(List<SignerChain> signerChains) {
        this.signerChainMap = signerChains.stream().collect(Collectors.toMap(SignerChain::getSigner, j -> j));
    }

    public void startSign(List<Document> documents) {
        if (MDC.get("SignKey") == null) {
            MDC.put("SignKey", UUID.randomUUID().toString());
        }

        log.debug("StartedSigningForDocs {}", documents.size());
        if (documents.isEmpty()) {
            return;
        }

        Map<DocumentSigners, List<Document>> signersListMap = documents
                .stream()
                .collect(Collectors.groupingBy(x -> getCurrentSigner(x.getSigners(), x.getSignedBy())));
        List<Document> signedDocs = new ArrayList<>();
        signersListMap.forEach((key, value) -> signedDocs.addAll(signerChainMap.get(key).sign(value))
        );
        if (!signedDocs.isEmpty()) {
            startSign(signedDocs);
        }
    }


    public static DocumentSigners getCurrentSigner(List<DocumentSigners> signers, List<DocumentSigners> signedBy) {
        Collection<DocumentSigners> documentSigners = CollectionUtils.removeAll(signers, signedBy);
        return documentSigners.stream().min(Comparator.comparing(DocumentSigners::ordinal)).orElseThrow();
    }
}
