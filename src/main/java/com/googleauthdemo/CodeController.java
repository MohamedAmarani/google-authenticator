package com.googleauthdemo;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/")
public class CodeController {

    private final GoogleAuthenticator gAuth;
    private final CredentialRepository credentialRepository;


    @GetMapping("/sayHello/{email}")
    public ResponseEntity<Object> getHello(@PathVariable String email) {
        try {
            verifyEmail(email);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        return ResponseEntity.ok("Hello " + email + "!");
    }

    @SneakyThrows
    @GetMapping("/code/generate/{email}")
    public void generate(@PathVariable String email, HttpServletResponse response) {
        final GoogleAuthenticatorKey key = gAuth.createCredentials(email);

        String otpAuthURL = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL("GCS-demo", email, key);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        BitMatrix bitMatrix = qrCodeWriter.encode(otpAuthURL, BarcodeFormat.QR_CODE, 200, 200);

        ServletOutputStream outputStream = response.getOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        outputStream.close();
    }

    @PostMapping("/code/validate/key")
    public Validation validateKey(@RequestBody ValidateCodeDto body) {
        return new Validation(gAuth.authorizeUser(body.getEmail(), body.getCode()));
    }

    @GetMapping("/scratches/{email}")
    public List<Integer> getScratches(@PathVariable String email) {
        return verifyEmail(email);
    }

    private List<Integer> verifyEmail(@PathVariable String username) {
        return credentialRepository.getUser(username).getScratchCodes();
    }

    @PostMapping("/scratches/")
    public Validation validateScratch(@RequestBody ValidateCodeDto body) {
        List<Integer> scratchCodes = verifyEmail(body.getEmail());
        Validation validation = new Validation(scratchCodes.contains(body.getCode()));
        scratchCodes.remove(body.getCode());
        return validation;
    }
}
