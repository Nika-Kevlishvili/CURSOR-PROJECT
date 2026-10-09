package bg.energo.phoenix.controller;

import bg.energo.phoenix.model.entity.Configuration;
import bg.energo.phoenix.model.request.ConfirmPayRequest;
import bg.energo.phoenix.model.request.InitPayRequest;
import bg.energo.phoenix.model.response.EasyPayConfirmPaymentResponse;
import bg.energo.phoenix.model.response.EasyPayInitPaymentResponse;
import bg.energo.phoenix.repository.ConfigurationRepository;
import bg.energo.phoenix.service.EasyPayConfirmPaymentService;
import bg.energo.phoenix.service.EasyPayInitPaymentService;
import bg.energo.phoenix.utils.EPBSignatureUtils;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("epay")
public class EPayController {

    private final EasyPayInitPaymentService easyPayInitPaymentService;
    private final EasyPayConfirmPaymentService ePayConfirmPayService;
    private final ConfigurationRepository configurationRepository;

    @Value("${easypay.secret.key}")
    protected String secretKey;

    @GetMapping("/init-pay")
    public ResponseEntity<EasyPayInitPaymentResponse> initPay(@Valid InitPayRequest request, @Parameter(name = "CHECKSUM") @RequestParam(name = "CHECKSUM") String CHECKSUM) {
        return ResponseEntity.ok(easyPayInitPaymentService.initPay(request, CHECKSUM));
    }

    @GetMapping("/confirm-pay")
    public ResponseEntity<EasyPayConfirmPaymentResponse> confirmPay(@Valid ConfirmPayRequest request, @Parameter(name = "CHECKSUM") @RequestParam(name = "CHECKSUM") String CHECKSUM) {
        return ResponseEntity.ok(ePayConfirmPayService.confirmPay(request, CHECKSUM));
    }

    @GetMapping("/calculate-check-sum-confirm")
    public String calculateCheckSumConf(@Valid ConfirmPayRequest request) {

        Optional<Configuration> currentConfiguration = configurationRepository.findLatestConfiguration();
        String secretKeyFromConfig = null;
        if (currentConfiguration.isPresent()) {
            secretKeyFromConfig = currentConfiguration.get().getEasyPaySecretKey();
        }
        String finalSecretKey = secretKeyFromConfig == null ? secretKey : secretKeyFromConfig;

        String checkSum;
        try {
            checkSum = EPBSignatureUtils.calculateHMAC(finalSecretKey, EPBSignatureUtils.formatConfirmPayRequestFields(request));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        return checkSum;
    }

    @GetMapping("/calculate-check-sum-init")
    public String calculateCheckSumInit(@Valid InitPayRequest request) {
        String checkSum;
        Optional<Configuration> currentConfiguration = configurationRepository.findLatestConfiguration();
        String secretKeyFromConfig = null;
        if (currentConfiguration.isPresent()) {
            secretKeyFromConfig = currentConfiguration.get().getEasyPaySecretKey();
        }
        String finalSecretKey = secretKeyFromConfig == null ? secretKey : secretKeyFromConfig;

        try {
            checkSum = EPBSignatureUtils.calculateHMAC(finalSecretKey, EPBSignatureUtils.formatInitRequestFields(request));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        return checkSum;
    }

}
