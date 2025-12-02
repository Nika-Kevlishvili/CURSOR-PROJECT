package bg.energo.phoenix.model.request.contract.order.service.proxy;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.customAnotations.product.product.ValidEmail;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ServiceOrderAuthorizedProxyRequest {

    @NotNull(message = "basicParameters.proxies.authorizedProxy.foreignEntity-Foreign entity selection is required;")
    private Boolean foreignEntity;

    @NotBlank(message = "basicParameters.proxies.authorizedProxy.name-Name is required;")
    @Pattern(regexp = "^[А-Яа-яA-Za-z0-9–@#$&*()-+\\-:.,‘€№= ]*$", message = "basicParameters.proxies.authorizedProxy.name-Name does not match allowed symbols;")
    @Size(min = 1, max = 512, message = "basicParameters.proxies.authorizedProxy.name-Name length should be between {min} and {max} symbols;")
    private String name;

    @NotNull(message = "basicParameters.proxies.authorizedProxy.customerIdentifier-UIC/personal number is required;")
    private String customerIdentifier;

    @ValidEmail(field = "basicParameters.proxies.authorizedProxy.email")
    private String email;

    @Size(min = 1, max = 32, message = "basicParameters.proxies.authorizedProxy.phone-Phone number length should be between {min} and {max} symbols;")
    @Pattern(regexp = "^[0-9–+\\-*]+$", message = "basicParameters.proxies.authorizedProxy.phone-Phone number does not match allowed symbols;")
    private String phone;

    @Pattern(regexp= "^[0-9A-Za-zА-Яа-я–-]+$", message = "basicParameters.proxies.authorizedProxy.powerOfAttorneyNumber-Number of power of attorney does not match allowed symbols;")
    @NotBlank(message = "basicParameters.proxies.authorizedProxy.powerOfAttorneyNumber-Number of power of attorney is required;")
    @Size(min = 1, max = 32, message = "basicParameters.proxies.authorizedProxy.powerOfAttorneyNumber-Number of power of attorney length should be between {min} and {max} symbols;")
    private String powerOfAttorneyNumber;

    @NotNull(message = "basicParameters.proxies.authorizedProxy.date-Date is required;")
    @DateRangeValidator(fieldPath = "basicParameters.proxies.authorizedProxy.date", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @PastOrPresent(message = "basicParameters.proxies.authorizedProxy.date-[proxyDate] must be in the past or in the present;")
    private LocalDate date;

    @DateRangeValidator(fieldPath = "basicParameters.proxies.authorizedProxy.validTill", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @FutureOrPresent(message = "basicParameters.proxies.authorizedProxy.validTill-[Valid Till] date must be in the present or in the future;")
    private LocalDate validTill;

    @Size(min = 1, max = 512, message = "basicParameters.proxies.authorizedProxy.notaryPublic-Field length should be between {min} and {max} symbols;")
    @Pattern(regexp= "^[А-Яа-яA-Za-z0-9@#$&*()–+\\-:.,‘€№= ]*$", message = "basicParameters.proxies.authorizedProxy.notaryPublic-Field does not match allowed symbols;")
    private String notaryPublic;

    @Size(min = 1, max = 32, message = "basicParameters.proxies.authorizedProxy.registrationNumber-Registration number length should be between {min} and {max} symbols;")
    @Pattern(regexp= "^[0-9A-Za-zА-Яа-я–-]+$", message = "basicParameters.proxies.authorizedProxy.registrationNumber-Registration number does not match allowed symbols;")
    private String registrationNumber;

    @Size(min = 1, max = 512, message = "basicParameters.proxies.authorizedProxy.operationArea-[Operation area] length should be between {min} and {max} symbols;")
    @Pattern(regexp= "^[А-ЯA-Zа-яa-z0-9@#$&*()–+\\-:.,‘€№= ]*$", message = "basicParameters.proxies.authorizedProxy.operationArea-[Operation area] does not match allowed symbols;")
    private String operationArea;

}
