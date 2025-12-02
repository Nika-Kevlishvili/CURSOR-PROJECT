package bg.energo.phoenix.service.xEnergie;

import bg.energo.phoenix.model.request.communication.xEnergie.*;
import bg.energo.phoenix.model.response.communication.xEnergie.ETRMDealDataResponse;
import bg.energo.phoenix.util.epb.EPBXmlUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class XEnergieCommunicationMapper {
    /**
     * Creating xEnergie create customer request
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
     */
    public ETRMDealDataRequest createRequest(
            LocalDateTime dealCreationTime,
            LocalDateTime contractStartDate,
            LocalDateTime contractEndDate,
            String customerNumber,
            String customerIdentifier,
            String contractNumber,
            LocalDate contractDate,
            String balancingSystemProductName,
            List<Parametric.Formula> balancingSystemFormulas
    ) {
        return ETRMDealDataRequest
                .builder()
                .dateCreated(dealCreationTime.toString())
                .messageIdentification(ETRMDealDataRequest.IdentificationProperties
                        .builder()
                        .v("MSG20232604145246") // const
                        .build())
                .senderIdentification(ETRMDealDataRequest.IdentificationProperties
                        .builder()
                        .v("MACRO-XENERGIE") // const
                        .build())
                .receiverIdentification(ETRMDealDataRequest.IdentificationProperties
                        .builder()
                        .v("XENERGIE") // const
                        .build())
                .deal(Deal.builder()
                        .type(Deal.Type.builder()
                                .deleteValue(false)
                                .value(Deal.Type.Value.builder()
                                        .code(Deal.Type.Value.Code.builder()
                                                .deleteValue(false)
                                                .value(Value.builder()
                                                        .v("ESBC") // const
                                                        .isKey(true)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .dateTimeFrom(Deal.DateTimeParameter.builder()
                                .deleteValue(false)
                                .value(Value.builder()
                                        .v(contractStartDate.format(DateTimeFormatter.ISO_DATE_TIME))
                                        .build())
                                .build())
                        .dateTimeTo(Deal.DateTimeParameter.builder()
                                .deleteValue(false)
                                .value(Value.builder()
                                        .v(contractEndDate.format(DateTimeFormatter.ISO_DATE_TIME))
                                        .build())
                                .build())
                        .outParty(Deal.Party.builder()
                                .deleteValue(false)
                                .value(Deal.Party.Value.builder()
                                        .ean(Deal.Party.Value.Ean.builder()
                                                .deleteValue(false)
                                                .value(Value.builder()
                                                        .v("CBG005") // const
                                                        .isKey(true)
                                                        .build())
                                                .build())
                                        .attributes(Attributes.builder()
                                                .attribute(List.of(
                                                        Attributes.Attribute.builder()
                                                                .name("Tax ID") // const
                                                                .deleteValue(false)
                                                                .value(Value.builder()
                                                                        .v("201398872") // const
                                                                        .isKey(true)
                                                                        .build())
                                                                .build()
                                                ))
                                                .build())
                                        .build())
                                .build())
                        .outArea(Deal.Area.builder()
                                .deleteValue(false)
                                .value(Deal.Area.Value.builder()
                                        .id(Deal.Area.Value.Id.builder()
                                                .deleteValue(false)
                                                .value(Value.builder()
                                                        .v("41") // const
                                                        .isKey(true)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .inParty(Deal.Party.builder()
                                .deleteValue(false)
                                .value(Deal.Party.Value.builder()
                                        .ean(Deal.Party.Value.Ean.builder()
                                                .deleteValue(false)
                                                .value(Value.builder()
                                                        .v(customerNumber)
                                                        .isKey(true)
                                                        .build())
                                                .build())
                                        .attributes(Attributes.builder()
                                                .attribute(List.of(
                                                        Attributes.Attribute.builder()
                                                                .name("Tax ID") // const
                                                                .deleteValue(false)
                                                                .value(Value.builder()
                                                                        .v(customerIdentifier)
                                                                        .isKey(true)
                                                                        .build())
                                                                .build()
                                                )).build())
                                        .build())
                                .build())
                        .inArea(Deal.Area.builder()
                                .deleteValue(false)
                                .value(Deal.Area.Value.builder()
                                        .id(Deal.Area.Value.Id.builder()
                                                .deleteValue(false)
                                                .value(Value.builder()
                                                        .v("41") // const
                                                        .isKey(true)
                                                        .build())
                                                .build())
                                        .build())
                                .build()
                        )
                        .direction(Deal.Direction.builder()
                                .deleteValue(false)
                                .value(Value.builder()
                                        .v("S")  // const
                                        .isKey(false)
                                        .build())
                                .build())
                        .shortName(Deal.ShortName.builder()
                                .deleteValue(false)
                                .value(Value.builder()
                                        .v("%s/%s".formatted(contractNumber, contractDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))))
                                        .isKey(false)
                                        .build())
                                .build())
                        .dealData(Deal.DealData.builder()
                                .name(balancingSystemProductName)
                                .resolution("PT60M") // const
                                .parametric(Parametric.builder()
                                        .formulas(balancingSystemFormulas)
                                        .build())
                                .build())
                        .build())
                .build();
    }

    public ETRMDealDataRequest updateRequest(
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
    ) {
        return ETRMDealDataRequest
                .builder()
                .dateCreated(dealCreationTime.toString())
                .messageIdentification(ETRMDealDataRequest.IdentificationProperties
                        .builder()
                        .v("MSG20232604145246") // const
                        .build())
                .senderIdentification(ETRMDealDataRequest.IdentificationProperties
                        .builder()
                        .v("MACRO-XENERGIE") // const
                        .build())
                .receiverIdentification(ETRMDealDataRequest.IdentificationProperties
                        .builder()
                        .v("XENERGIE") // const
                        .build())
                .deal(Deal.builder()
                        .number(Deal.Number.builder()
                                .deleteValue(false)
                                .value(Value.builder()
                                        .v(dealNumber)
                                        .isKey(true)
                                        .build())
                                .build())
                        .type(Deal.Type.builder()
                                .deleteValue(false)
                                .value(Deal.Type.Value.builder()
                                        .code(Deal.Type.Value.Code.builder()
                                                .deleteValue(false)
                                                .value(Value.builder()
                                                        .v("ESBC") // const
                                                        .isKey(true)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .dateTimeFrom(Deal.DateTimeParameter.builder()
                                .deleteValue(false)
                                .value(Value.builder()
                                        .v(contractStartDate.format(DateTimeFormatter.ISO_DATE_TIME))
                                        .build())
                                .build())
                        .dateTimeTo(Deal.DateTimeParameter.builder()
                                .deleteValue(false)
                                .value(Value.builder()
                                        .v(contractEndDate.format(DateTimeFormatter.ISO_DATE_TIME))
                                        .build())
                                .build())
                        .outParty(Deal.Party.builder()
                                .deleteValue(false)
                                .value(Deal.Party.Value.builder()
                                        .ean(Deal.Party.Value.Ean.builder()
                                                .deleteValue(false)
                                                .value(Value.builder()
                                                        .v("CBG005") // const
                                                        .isKey(true)
                                                        .build())
                                                .build())
                                        .attributes(Attributes.builder()
                                                .attribute(List.of(
                                                        Attributes.Attribute.builder()
                                                                .name("Tax ID") // const
                                                                .deleteValue(false)
                                                                .value(Value.builder()
                                                                        .v("201398872") // const
                                                                        .isKey(true)
                                                                        .build())
                                                                .build()
                                                ))
                                                .build())
                                        .build())
                                .build())
                        .outArea(Deal.Area.builder()
                                .deleteValue(false)
                                .value(Deal.Area.Value.builder()
                                        .id(Deal.Area.Value.Id.builder()
                                                .deleteValue(false)
                                                .value(Value.builder()
                                                        .v("41") // const
                                                        .isKey(true)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .inParty(Deal.Party.builder()
                                .deleteValue(false)
                                .value(Deal.Party.Value.builder()
                                        .ean(Deal.Party.Value.Ean.builder()
                                                .deleteValue(false)
                                                .value(Value.builder()
                                                        .v(customerNumber)
                                                        .isKey(true)
                                                        .build())
                                                .build())
                                        .attributes(Attributes.builder()
                                                .attribute(List.of(
                                                        Attributes.Attribute.builder()
                                                                .name("Tax ID") // const
                                                                .deleteValue(false)
                                                                .value(Value.builder()
                                                                        .v(customerIdentifier)
                                                                        .isKey(true)
                                                                        .build())
                                                                .build()
                                                )).build())
                                        .build())
                                .build())
                        .inArea(Deal.Area.builder()
                                .deleteValue(false)
                                .value(Deal.Area.Value.builder()
                                        .id(Deal.Area.Value.Id.builder()
                                                .deleteValue(false)
                                                .value(Value.builder()
                                                        .v("41") // const
                                                        .isKey(true)
                                                        .build())
                                                .build())
                                        .build())
                                .build()
                        )
                        .direction(Deal.Direction.builder()
                                .deleteValue(false)
                                .value(Value.builder()
                                        .v("S")  // const
                                        .isKey(false)
                                        .build())
                                .build())
                        .shortName(Deal.ShortName.builder()
                                .deleteValue(false)
                                .value(Value.builder()
                                        .v("%s/%s".formatted(contractNumber, contractDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))))
                                        .isKey(false)
                                        .build())
                                .build())
                        .dealData(Deal.DealData.builder()
                                .name(balancingSystemProductName)
                                .resolution("PT60M") // const
                                .parametric(Parametric.builder()
                                        .formulas(balancingSystemFormulas)
                                        .build())
                                .build())
                        .build())
                .build();
    }

    public ETRMDealDataResponse mapResponseFromString(String response) {
        return EPBXmlUtils.writeValueAs(response, ETRMDealDataResponse.class);
    }
}
