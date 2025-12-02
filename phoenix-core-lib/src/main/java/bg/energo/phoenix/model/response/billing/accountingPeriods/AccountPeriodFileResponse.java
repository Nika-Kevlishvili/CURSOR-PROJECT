package bg.energo.phoenix.model.response.billing.accountingPeriods;

public record AccountPeriodFileResponse(
        Long id,
        String url,
        String name,
        String type
) {
}
