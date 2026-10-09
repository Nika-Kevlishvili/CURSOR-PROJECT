package bg.energo.phoenix.virtualpos.controller;

import bg.energo.phoenix.model.entity.Configuration;
import bg.energo.phoenix.repository.ConfigurationRepository;
import bg.energo.phoenix.utils.EPBSignatureUtils;
import bg.energo.phoenix.virtualpos.model.request.VirtualPosConfirmPayRequest;
import bg.energo.phoenix.virtualpos.model.request.VirtualPosInitPayRequest;
import bg.energo.phoenix.virtualpos.model.response.VirtualPosConfirmPaymentResponse;
import bg.energo.phoenix.virtualpos.model.response.VirtualPosInitPaymentResponse;
import bg.energo.phoenix.virtualpos.service.VirtualPosConfirmPaymentService;
import bg.energo.phoenix.virtualpos.service.VirtualPosInitPaymentService;
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
@RequestMapping("virtualpos")
public class VirtualPosController {

    private final VirtualPosInitPaymentService virtualPosInitPaymentService;
    private final VirtualPosConfirmPaymentService virtualPosConfirmPaymentService;
    private final ConfigurationRepository configurationRepository;

    @Value("${virtualpos.secret.key}")
    protected String secretKey;

    @GetMapping("/init-pay")
    public ResponseEntity<VirtualPosInitPaymentResponse> initPay(@Valid VirtualPosInitPayRequest request, @Parameter(name = "CHECKSUM") @RequestParam(name = "CHECKSUM") String CHECKSUM) {
        return ResponseEntity.ok(virtualPosInitPaymentService.initPay(request, CHECKSUM));
    }

    @GetMapping("/confirm-pay")
    public ResponseEntity<VirtualPosConfirmPaymentResponse> confirmPay(@Valid VirtualPosConfirmPayRequest request, @Parameter(name = "CHECKSUM") @RequestParam(name = "CHECKSUM") String CHECKSUM) {
        return ResponseEntity.ok(virtualPosConfirmPaymentService.confirmPay(request, CHECKSUM));
    }

    @GetMapping("/calculate-check-sum-confirm")
    public String calculateCheckSumConf(@Valid VirtualPosConfirmPayRequest request) {
        Optional<Configuration> currentConfiguration = configurationRepository.findLatestConfiguration();
        String secretKeyFromConfig = null;
        if (currentConfiguration.isPresent()) {
            secretKeyFromConfig = currentConfiguration.get().getVirtualPosSecretKey();
        }
        String finalSecretKey = secretKeyFromConfig == null ? secretKey : secretKeyFromConfig;

        try {
            return EPBSignatureUtils.calculateHMAC(finalSecretKey, EPBSignatureUtils.formatVirtualPosConfirmPayRequestFields(request));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/calculate-check-sum-init")
    public String calculateCheckSumInit(@Valid VirtualPosInitPayRequest request) {
        Optional<Configuration> currentConfiguration = configurationRepository.findLatestConfiguration();
        String secretKeyFromConfig = null;
        if (currentConfiguration.isPresent()) {
            secretKeyFromConfig = currentConfiguration.get().getVirtualPosSecretKey();
        }
        String finalSecretKey = secretKeyFromConfig == null ? secretKey : secretKeyFromConfig;
        try {
            return EPBSignatureUtils.calculateHMAC(finalSecretKey, EPBSignatureUtils.formatVirtualPosInitRequestFields(request));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
}


