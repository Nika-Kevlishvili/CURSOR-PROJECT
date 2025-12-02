package bg.energo.phoenix.service.xEnergie;

import bg.energo.phoenix.exception.XEnergieCommunicationException;
import bg.energo.phoenix.model.request.communication.xEnergie.ETRMDealDataRequest;
import bg.energo.phoenix.model.request.communication.xEnergie.Parametric;
import bg.energo.phoenix.model.response.communication.xEnergie.ETRMDealDataResponse;
import bg.energo.phoenix.util.epb.EPBXmlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class XEnergieCommunicationService {
    private final XEnergieCommunicationMapper mapper;
    private final WebClient webClient;

    @Value("${xEnergie.api.url}")
    private String xEnergieApiUrl;

    XEnergieCommunicationService(WebClient.Builder webClient, XEnergieCommunicationMapper mapper) {
        this.webClient = webClient.build();
        this.mapper = mapper;
    }

    /**
     * Creating xEnergie deal
     *
     * @param dealCreationTime           - deal creation time
     * @param contractStartDate          - contract start date
     * @param contractEndDate            - contract end date
     * @param customerNumber             - customer number
     * @param customerIdentifier         - customer identifier
     * @param contractNumber             - contract number
     * @param contractDate               - contract date
     * @param balancingSystemProductName - balancing system product name
     * @param balancingSystemFormulas    - balancing system formulas - Formula values must be decimals on a scale of 2
     * @return {@link ETRMDealDataRequest}
     * @throws XEnergieCommunicationException - throws exception on error response
     * @throws RuntimeException               - throws exception if unexpected exception handled
     */
    public ETRMDealDataResponse createDeal(
            LocalDateTime dealCreationTime,
            LocalDateTime contractStartDate,
            LocalDateTime contractEndDate,
            String customerNumber,
            String customerIdentifier,
            String contractNumber,
            LocalDate contractDate,
            String balancingSystemProductName,
            List<Parametric.Formula> balancingSystemFormulas
    ) throws XEnergieCommunicationException, RuntimeException {
        String request = EPBXmlUtils.writeValueAsString(
                mapper.createRequest(
                        dealCreationTime,
                        contractStartDate,
                        contractEndDate,
                        customerNumber,
                        customerIdentifier,
                        contractNumber,
                        contractDate,
                        balancingSystemProductName,
                        balancingSystemFormulas
                )
        );

        return executeCall(request);
    }

    public ETRMDealDataResponse updateDeal(
            String dealNumber,
            LocalDateTime dealCreationTime,
            LocalDateTime contractStartDate,
            LocalDateTime contractEndDate,
            String customerNumber,
            String customerIdentifier,
            String contractNumber,
            LocalDate contractDate,
            String balancingSystemProductName,
            List<Parametric.Formula> balancingSystemFormulas
    ) throws XEnergieCommunicationException {
        String request = EPBXmlUtils.writeValueAsString(
                mapper.updateRequest(
                        dealNumber,
                        dealCreationTime,
                        contractStartDate,
                        contractEndDate,
                        customerNumber,
                        customerIdentifier,
                        contractNumber,
                        contractDate,
                        balancingSystemProductName,
                        balancingSystemFormulas
                )
        );

        return executeCall(request);
    }

    private ETRMDealDataResponse executeCall(String request) throws XEnergieCommunicationException {
        if (request == null) {
            log.error("Cannot serialize request object, object was null");
            throw new RuntimeException("Cannot serialize request object, object was null");
        }

        ResponseEntity<String> clientResponse = webClient
                .post()
                .uri(URI.create(xEnergieApiUrl))
                .body(BodyInserters
                        .fromValue(request))
                .exchangeToMono((response) -> {
                    if (response == null) {
                        log.error("xEnergie response was null");
                        throw new RuntimeException("xEnergie response was null");
                    }

                    return response.toEntity(String.class);
                })
                .timeout(Duration.of(30, ChronoUnit.SECONDS))
                .block();

        if (clientResponse == null) {
            log.error("Cannot extract response entity from xEnergie response");
            throw new RuntimeException("Cannot extract response entity from xEnergie response");
        }

        ETRMDealDataResponse etrmDealDataResponse = mapper.mapResponseFromString(clientResponse.getBody());

        if (etrmDealDataResponse == null) {
            log.error("Cannot deserialize response object, object was null");
            throw new RuntimeException("Cannot deserialize response object, object was null");
        }

        ETRMDealDataResponse.Reason reason = etrmDealDataResponse.getReason();
        if (reason != null) {
            throw new XEnergieCommunicationException(clientResponse.getStatusCodeValue(), reason.getCode(), reason.getDescription());
        }
        return etrmDealDataResponse;
    }

    public void exportPods(ByteArrayOutputStream excelContent) {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("attachment", excelContent.toByteArray(), MediaType.MULTIPART_FORM_DATA)
                .filename("%s/PodExport.xlsx".formatted(LocalDateTime.now()));

        webClient
                .post()
                .uri("")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                .retrieve(); // TODO: 18.10.23 not finished yet
    }
}
