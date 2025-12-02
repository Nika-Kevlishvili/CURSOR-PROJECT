package bg.energo.phoenix.model.response.receivable.disconnectionPowerSupply;

public record TypeOfDisconnectionList(
        Long gridOperatorTaxId,
        String disconnectionType
) {
}
