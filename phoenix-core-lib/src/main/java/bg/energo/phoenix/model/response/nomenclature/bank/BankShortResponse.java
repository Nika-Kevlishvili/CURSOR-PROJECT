package bg.energo.phoenix.model.response.nomenclature.bank;

public record BankShortResponse(
        Long id,
        String name,
        String bic
) {
}
