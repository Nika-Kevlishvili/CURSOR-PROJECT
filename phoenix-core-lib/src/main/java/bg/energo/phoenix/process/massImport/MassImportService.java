package bg.energo.phoenix.process.massImport;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MassImportService {

    private final List<MassImportBaseService> massImportBaseServiceList;

    public byte[] downloadMassImportTemplate(String domain) {
        return findMassImportBaseService(domain).getMassImportTemplate();
    }

    public void uploadMassImportFile(String domain, MultipartFile file, LocalDate date,Long collectionChannelId) {
        if ((domain.equals(DomainType.SUPPLY_AUTOMATIC_ACTIVATIONS.getValue()) || domain.equals(DomainType.SUPPLY_AUTOMATIC_DEACTIVATIONS.getValue())) &&
            date == null){
            throw new AccessDeniedException("You should provide date to start this process");
        }
        if(domain.equals(DomainType.PAYMENT.getValue())) {
            StringBuilder messages = new StringBuilder();
            if(date==null) {
                messages.append("You should provide date to start this process;");
            }
            if(collectionChannelId==null) {
                messages.append("You should provide collection channel to start this process;");
            }
            if(!messages.isEmpty()) {
                throw new ClientException(messages.toString(), ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }
        findMassImportBaseService(domain).uploadMassImportFile(file, date,collectionChannelId,false);
    }

    /**
     * Finds and returns the {@link MassImportBaseService} object that corresponds to the specified domain name.
     *
     * @param domainName the name of the domain for which to find the corresponding {@link MassImportBaseService}.
     * @return {@link MassImportBaseService} object
     * @throws DomainEntityNotFoundException if the specified domain name does not exist or if a {@link MassImportBaseService} object
     * cannot be found for the domain type.
     */
    private MassImportBaseService findMassImportBaseService(String domainName) {
        Optional<DomainType> domainOptional = Optional.ofNullable(DomainType.fromValue(domainName));
        if (domainOptional.isEmpty()) {
            log.error("Requested domainName type does not exist");
            throw new DomainEntityNotFoundException("Request domainName type does not exist");
        }

        DomainType domainType = domainOptional.get();

        Optional<MassImportBaseService> massImportServiceOptional = massImportBaseServiceList
                .stream()
                .filter(massImportBaseService -> massImportBaseService.getDomainType().equals(domainType))
                .findFirst();

        if (massImportServiceOptional.isEmpty()) {
            log.error("Service does not exist for domain type: {}", domainType);
            throw new DomainEntityNotFoundException("Service does not exist for domain type: %s".formatted(domainType));
        }

        return massImportServiceOptional.get();
    }

}
