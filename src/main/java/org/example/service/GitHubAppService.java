package org.example.service;

import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * GitHub App authentication service.
 *
 * <p>Reads the GitHub App private key from {@code GITHUB_APP_PRIVATE_KEY_BASE64}
 * (set in Cloud Run from the {@code SUPREMEAI_APP_PRIVATE_KEY} GitHub Secret),
 * mints a short-lived RS256 JWT, and exchanges it for a GitHub App installation
 * token that can be used as a Bearer/token credential against the GitHub API.
 *
 * <p>The installation token is cached for 55 minutes (GitHub tokens last 60 min).
 */
@Service
public class GitHubAppService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubAppService.class);

    /** Env var populated from the SUPREMEAI_APP_PRIVATE_KEY GitHub Secret. */
    static final String PRIVATE_KEY_B64_ENV   = "GITHUB_APP_PRIVATE_KEY_BASE64";
    static final String APP_ID_ENV            = "GITHUB_APP_ID";
    static final String INSTALLATION_ID_ENV   = "GITHUB_APP_INSTALLATION_ID";

    private static final String GITHUB_API = "https://api.github.com";

    /** Cached installation token and its expiry timestamp (ms). */
    private volatile String cachedToken;
    private volatile long   tokenExpiresAtMs;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} when all three required env vars are non-blank.
     */
    public boolean isConfigured() {
        return nonBlank(PRIVATE_KEY_B64_ENV)
            && nonBlank(APP_ID_ENV)
            && nonBlank(INSTALLATION_ID_ENV);
    }

    /**
     * Returns a GitHub App installation token, fetching / refreshing as needed.
     * Returns {@code null} when the App is not configured or an error occurs.
     */
    public synchronized String getInstallationToken() {
        if (!isConfigured()) {
            return null;
        }

        // Serve from cache if still valid (refresh 5 min before expiry)
        if (cachedToken != null
                && System.currentTimeMillis() < tokenExpiresAtMs - 300_000L) {
            return cachedToken;
        }

        try {
            String privateKeyB64    = System.getenv(PRIVATE_KEY_B64_ENV);
            String appId            = System.getenv(APP_ID_ENV);
            String installationId   = System.getenv(INSTALLATION_ID_ENV);

            PrivateKey privateKey = loadRsaPrivateKey(privateKeyB64);
            String jwt            = createAppJwt(appId, privateKey);
            String token          = fetchInstallationToken(jwt, installationId);

            cachedToken      = token;
            tokenExpiresAtMs = System.currentTimeMillis() + 3_600_000L; // 60 min

            logger.info("✅ GitHub App installation token refreshed (app={})", appId);
            return token;
        } catch (Exception e) {
            logger.error("❌ GitHub App token refresh failed: {}", e.getMessage());
            return null;
        }
    }

    // ── JWT creation ──────────────────────────────────────────────────────────

    /**
     * Creates a 9-minute RS256 JWT signed with the App private key.
     * GitHub App JWTs must use {@code iss = appId} and expire within 10 minutes.
     */
    String createAppJwt(String appId, PrivateKey privateKey) {
        long nowMs = System.currentTimeMillis();
        return Jwts.builder()
                .issuer(appId)
                .issuedAt(new Date(nowMs - 60_000L))     // 60 s in the past (clock-skew buffer)
                .expiration(new Date(nowMs + 540_000L))  // 9 min from now
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    // ── Private key loading ───────────────────────────────────────────────────

    /**
     * Decodes a base64-encoded PEM file (PKCS#1 or PKCS#8) into an RSA
     * {@link PrivateKey}.
     *
     * <p>The env var value is the <em>entire PEM file</em> base64-encoded
     * (e.g. {@code base64 supremeai-bot.pem}).
     */
    PrivateKey loadRsaPrivateKey(String base64PemContent) throws Exception {
        // 1. Decode outer base64 to get the PEM text
        byte[] pemRaw = Base64.getDecoder().decode(
                base64PemContent.replaceAll("\\s", ""));
        String pem = new String(pemRaw, StandardCharsets.UTF_8);

        boolean isPkcs1 = pem.contains("BEGIN RSA PRIVATE KEY");

        // 2. Strip PEM headers/footers and whitespace → raw key base64
        String rawBase64 = pem
                .replaceAll("-+BEGIN[^-]+-+", "")
                .replaceAll("-+END[^-]+-+", "")
                .replaceAll("\\s+", "");
        byte[] keyDer = Base64.getDecoder().decode(rawBase64);

        // 3. Convert PKCS#1 to PKCS#8 if needed (GitHub App keys are PKCS#1)
        byte[] pkcs8Der = isPkcs1 ? wrapPkcs1InPkcs8(keyDer) : keyDer;

        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(pkcs8Der));
    }

    /**
     * Wraps raw PKCS#1 RSA key bytes in a PKCS#8 {@code PrivateKeyInfo} structure
     * using pure DER encoding (no external libraries needed).
     *
     * <pre>
     * SEQUENCE {
     *   INTEGER 0                          -- version
     *   SEQUENCE { OID rsaEncryption, NULL }  -- AlgorithmIdentifier
     *   OCTET STRING { &lt;pkcs1&gt; }          -- privateKey
     * }
     * </pre>
     */
    static byte[] wrapPkcs1InPkcs8(byte[] pkcs1) throws IOException {
        // AlgorithmIdentifier for rsaEncryption (OID 1.2.840.113549.1.1.1 + NULL)
        byte[] algId = {
            0x30, 0x0d,
            0x06, 0x09, 0x2a, (byte)0x86, 0x48, (byte)0x86, (byte)0xf7, 0x0d, 0x01, 0x01, 0x01,
            0x05, 0x00
        };
        // version INTEGER 0
        byte[] version = {0x02, 0x01, 0x00};

        // OCTET STRING wrapping pkcs1 bytes
        byte[] octetStringTag  = derLengthHeader(0x04, pkcs1.length);

        // Compute total inner length
        int innerLen = version.length + algId.length + octetStringTag.length + pkcs1.length;

        ByteArrayOutputStream baos = new ByteArrayOutputStream(4 + innerLen);
        // Outer SEQUENCE
        writeTag(baos, 0x30);
        writeDerLength(baos, innerLen);
        baos.write(version);
        baos.write(algId);
        baos.write(octetStringTag);
        baos.write(pkcs1);
        return baos.toByteArray();
    }

    // ── GitHub API – exchange JWT for installation token ──────────────────────

    private String fetchInstallationToken(String jwt, String installationId) throws IOException {
        String url = GITHUB_API + "/app/installations/" + installationId + "/access_tokens";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + jwt);
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("Content-Length", "0");
            conn.setDoOutput(false);
            conn.connect();

            int status = conn.getResponseCode();
            InputStream bodyStream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String body = new BufferedReader(new InputStreamReader(bodyStream, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining());

            if (status < 200 || status >= 300) {
                throw new IOException("GitHub API " + status + ": " + body);
            }

            return extractJsonStringField(body, "token");
        } finally {
            conn.disconnect();
        }
    }

    // ── DER encoding helpers ──────────────────────────────────────────────────

    /** Returns the tag + DER length bytes for a TLV entry. */
    private static byte[] derLengthHeader(int tag, int length) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
        writeTag(baos, tag);
        writeDerLength(baos, length);
        return baos.toByteArray();
    }

    private static void writeTag(ByteArrayOutputStream baos, int tag) {
        baos.write(tag & 0xff);
    }

    private static void writeDerLength(ByteArrayOutputStream baos, int length) throws IOException {
        if (length < 0x80) {
            baos.write(length);
        } else if (length < 0x100) {
            baos.write(0x81);
            baos.write(length);
        } else {
            baos.write(0x82);
            baos.write((length >> 8) & 0xff);
            baos.write(length & 0xff);
        }
    }

    // ── Minimal JSON extraction ───────────────────────────────────────────────

    /**
     * Extracts a string field from a flat JSON object without an external parser.
     * Example: {@code {"token":"ghs_abc","expires_at":"..."}} → {@code "ghs_abc"}
     */
    static String extractJsonStringField(String json, String fieldName) {
        String search = "\"" + fieldName + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) {
            throw new IllegalArgumentException("Field '" + fieldName + "' not found in: " + json);
        }
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) {
            throw new IllegalArgumentException("Malformed JSON near '" + fieldName + "'");
        }
        int valueStart = json.indexOf('"', colon + 1);
        if (valueStart < 0) {
            throw new IllegalArgumentException("Expected string value for '" + fieldName + "'");
        }
        int valueEnd = json.indexOf('"', valueStart + 1);
        if (valueEnd < 0) {
            throw new IllegalArgumentException("Unterminated string for '" + fieldName + "'");
        }
        return json.substring(valueStart + 1, valueEnd);
    }

    private static boolean nonBlank(String envKey) {
        String v = System.getenv(envKey);
        return v != null && !v.isBlank();
    }
}
