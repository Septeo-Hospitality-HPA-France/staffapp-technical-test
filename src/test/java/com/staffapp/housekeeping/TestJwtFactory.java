package com.staffapp.housekeeping;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.staffapp.housekeeping.security.SecurityConfiguration;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Generates signed tokens for the demo users, as the identity provider would in production.
 * <p>
 * Run {@link #main} to print a token per user, for manual calls with curl or an HTTP client.
 */
public final class TestJwtFactory {

    // Must match app.security.jwt.secret in application.yml
    private static final String SECRET = "dev-only-signing-key-for-the-technical-test-0123456789";

    private static final Map<String, DemoUser> USERS = Map.of(
            "alice", new DemoUser(DemoData.ESTABLISHMENT_A, "STAFF"),
            "bob", new DemoUser(DemoData.ESTABLISHMENT_A, "STAFF"),
            "marc", new DemoUser(DemoData.ESTABLISHMENT_A, "MANAGER"),
            "claire", new DemoUser(DemoData.ESTABLISHMENT_B, "STAFF"),
            "nina", new DemoUser(DemoData.ESTABLISHMENT_B, "MANAGER"));

    private static final NimbusJwtEncoder ENCODER = new NimbusJwtEncoder(
            new ImmutableSecret<>(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));

    private TestJwtFactory() {
    }

    /**
     * Returns a token valid for one hour for a demo user: alice, bob, marc, claire or nina.
     */
    public static String token(String username) {
        var user = USERS.get(username);
        if (user == null) {
            throw new IllegalArgumentException("Unknown demo user: " + username);
        }
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .claim(SecurityConfiguration.ESTABLISHMENT_CLAIM, user.establishmentId())
                .claim(SecurityConfiguration.ROLE_CLAIM, user.role())
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return ENCODER.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Returns a token valid for one hour carrying exactly the given claims, to test malformed tokens.
     */
    public static String tokenWithClaims(Map<String, Object> claims) {
        var now = Instant.now();
        var builder = JwtClaimsSet.builder().issuedAt(now).expiresAt(now.plus(1, ChronoUnit.HOURS));
        claims.forEach(builder::claim);
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return ENCODER.encode(JwtEncoderParameters.from(header, builder.build())).getTokenValue();
    }

    public static void main(String[] args) {
        USERS.keySet().stream().sorted().forEach(username -> System.out.println(username + ": " + token(username)));
    }

    private record DemoUser(Long establishmentId, String role) {
    }
}
