package bg.energo.phoenix.virtualpos.controller;

import bg.energo.phoenix.utils.EPBSignatureUtils;
import bg.energo.phoenix.virtualpos.model.enums.VirtualPosStatusCode;
import bg.energo.phoenix.virtualpos.model.request.VirtualPosConfirmPayRequest;
import bg.energo.phoenix.virtualpos.model.request.VirtualPosInitPayRequest;
import bg.energo.phoenix.virtualpos.model.response.VirtualPosConfirmPaymentResponse;
import bg.energo.phoenix.virtualpos.model.response.VirtualPosInitPaymentResponse;
import bg.energo.phoenix.virtualpos.service.VirtualPosConfirmPaymentService;
import bg.energo.phoenix.virtualpos.service.VirtualPosInitPaymentService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("virtualpos")
public class VirtualPosController {

    private final VirtualPosInitPaymentService virtualPosInitPaymentService;
    private final VirtualPosConfirmPaymentService virtualPosConfirmPaymentService;

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
        String checkSum;
        try {
            checkSum = EPBSignatureUtils.calculateHMAC(secretKey, EPBSignatureUtils.formatVirtualPosConfirmPayRequestFields(request));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        return checkSum;
    }

    @GetMapping("/calculate-check-sum-init")
    public String calculateCheckSumInit(@Valid VirtualPosInitPayRequest request) {
        String checkSum;
        try {
            checkSum = EPBSignatureUtils.calculateHMAC(secretKey, EPBSignatureUtils.formatVirtualPosInitRequestFields(request));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        return checkSum;
    }

}


