package bg.energo.phoenix.cashterminal.controller;

import bg.energo.phoenix.cashterminal.model.request.CashTerminalConfirmPayRequest;
import bg.energo.phoenix.cashterminal.model.request.CashTerminalInitPayRequest;
import bg.energo.phoenix.cashterminal.model.response.CashTerminalConfirmPaymentResponse;
import bg.energo.phoenix.cashterminal.model.response.CashTerminalInitPaymentResponse;
import bg.energo.phoenix.cashterminal.service.CashTerminalConfirmPaymentService;
import bg.energo.phoenix.cashterminal.service.CashTerminalInitPaymentService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("cashterminal")
public class CashTerminalController {

    private final CashTerminalInitPaymentService cashTerminalInitPaymentService;
    private final CashTerminalConfirmPaymentService cashTerminalConfirmPaymentService;

    @Value("${cashterminal.secret.key}")
    protected String secretKey;

    @GetMapping("/init-pay")
    public ResponseEntity<CashTerminalInitPaymentResponse> initPay(@Valid CashTerminalInitPayRequest request, @Parameter(name = "CHECKSUM") @RequestParam(name = "CHECKSUM") String CHECKSUM) {
        return ResponseEntity.ok(cashTerminalInitPaymentService.initPay(request, CHECKSUM));
    }

    @GetMapping("/confirm-pay")
    public ResponseEntity<CashTerminalConfirmPaymentResponse> confirmPay(@Valid CashTerminalConfirmPayRequest request, @Parameter(name = "CHECKSUM") @RequestParam(name = "CHECKSUM") String CHECKSUM) {
        return ResponseEntity.ok(cashTerminalConfirmPaymentService.confirmPay(request, CHECKSUM));
    }

    @GetMapping("/calculate-check-sum-confirm")
    public String calculateCheckSumConf(@Valid CashTerminalConfirmPayRequest request) {
        try {
            return EPBSignatureUtils.calculateHMAC(secretKey, EPBSignatureUtils.formatCashTerminalConfirmPayRequestFields(request));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/calculate-check-sum-init")
    public String calculateCheckSumInit(@Valid CashTerminalInitPayRequest request) {
        try {
            return EPBSignatureUtils.calculateHMAC(secretKey, EPBSignatureUtils.formatCashTerminalInitRequestFields(request));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
}

