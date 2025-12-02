package bg.energo.phoenix.service.signing.signatus;

import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.service.signing.SignerChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SignatusChain implements SignerChain {



    @Override
    @Transactional
    public List<Document> sign(List<Document> documents) {

        return new ArrayList<>();
    }

    @Override
    public DocumentSigners getSigner() {
        return DocumentSigners.SIGNATUS;
    }


}
