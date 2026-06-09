package com.supremeai.service.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.KeyAgreement;
import java.security.*;
import java.util.Base64;
import java.util.Map;

@Service
public class ECCToolsService {
    private static final Logger logger = LoggerFactory.getLogger(ECCToolsService.class);
    private static final String KEY_ALG = "EC";
    private static final String KEY_AGREEMENT = "ECDH";

    public Map<String, Object> generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALG);
        kpg.initialize(256);
        KeyPair kp = kpg.generateKeyPair();
        return Map.of(
                "public", Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()),
                "private", Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded()));
    }

    public byte[] ecdhAgree(String ownPrivateBase64, String peerPublicBase64) throws GeneralSecurityException {
        KeyFactory kf = KeyFactory.getInstance(KEY_ALG);
        PrivateKey ownPrivate = kf.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(Base64.getDecoder().decode(ownPrivateBase64)));
        PublicKey peerPublic = kf.generatePublic(new java.security.spec.X509EncodedKeySpec(Base64.getDecoder().decode(peerPublicBase64)));
        KeyAgreement ka = KeyAgreement.getInstance(KEY_AGREEMENT);
        ka.init(ownPrivate);
        ka.doPhase(peerPublic, true);
        return Base64.getEncoder().encode(ka.generateSecret());
    }

    public boolean isAvailable() {
        try {
            return KeyPairGenerator.getInstance(KEY_ALG) != null && KeyAgreement.getInstance(KEY_AGREEMENT) != null;
        } catch (GeneralSecurityException e) {
            logger.warn("[ECC] JCA provider missing", e);
            return false;
        }
    }
}
