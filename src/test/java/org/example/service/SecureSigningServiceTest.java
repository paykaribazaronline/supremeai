package org.example.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecureSigningServiceTest {

    @Mock
    private SigningAuditService signingAuditService;

    @Test
    void signApkFailsWhenFallbackDisabledAndKeystoreMissing() throws Exception {
        SecureSigningService service = new SecureSigningService();
        ReflectionTestUtils.setField(service, "signingAudit", signingAuditService);
        ReflectionTestUtils.setField(service, "keystorePath", "");
        ReflectionTestUtils.setField(service, "keystorePassword", "");
        ReflectionTestUtils.setField(service, "allowDevFallback", false);

        Path unsignedApk = createDummyApk();
        Path signedApk = Files.createTempFile("signed", ".apk");

        SecureSigningService.BuildContext context = new SecureSigningService.BuildContext(
            "build-1", unsignedApk, signedApk, "admin", "app", "1.0.0"
        );

        SecureSigningService.SigningResult result = service.signApk(context);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Keystore password not configured"));
    }

    @Test
    void signApkSucceedsInExplicitDevFallbackMode() throws Exception {
        SecureSigningService service = new SecureSigningService();
        ReflectionTestUtils.setField(service, "signingAudit", signingAuditService);
        ReflectionTestUtils.setField(service, "keystorePath", "");
        ReflectionTestUtils.setField(service, "keystorePassword", "");
        ReflectionTestUtils.setField(service, "allowDevFallback", true);

        Path unsignedApk = createDummyApk();
        Path signedApk = Files.createTempFile("signed-dev", ".apk");

        SecureSigningService.BuildContext context = new SecureSigningService.BuildContext(
            "build-2", unsignedApk, signedApk, "admin", "app", "1.0.0"
        );

        SecureSigningService.SigningResult result = service.signApk(context);

        assertTrue(result.isSuccess());
        assertNotNull(result.getSignedApkPath());
        assertTrue(Files.exists(result.getSignedApkPath()));
    }

    private Path createDummyApk() throws IOException {
        Path apkPath = Files.createTempFile("unsigned", ".apk");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(apkPath))) {
            zos.putNextEntry(new ZipEntry("classes.dex"));
            zos.write("dummy".getBytes());
            zos.closeEntry();
        }
        return apkPath;
    }
}
