package com.staffapp.housekeeping.dev;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.staffapp.housekeeping.security.SecurityConfiguration;
import com.staffapp.housekeeping.staff.Staff;
import com.staffapp.housekeeping.staff.StaffRepository;
import io.swagger.v3.oas.annotations.Hidden;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev only: issues a token per demo user, as the identity provider would, so that Swagger UI can call the API without
 * copying tokens by hand. Never active outside the {@code dev} profile.
 */
@Hidden
@Profile("dev")
@RestController
class DevTokenController {

    private final StaffRepository staffRepository;
    private final NimbusJwtEncoder encoder;

    DevTokenController(StaffRepository staffRepository, @Value("${app.security.jwt.secret}") String secret) {
        this.staffRepository = staffRepository;
        this.encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
    }

    @GetMapping("/dev/tokens")
    @Transactional(readOnly = true)
    List<DevToken> tokens() {
        return staffRepository.findAll().stream()
                .sorted(Comparator.comparing(Staff::getUsername))
                .map(this::token)
                .toList();
    }

    private DevToken token(Staff staff) {
        var now = Instant.now();
        var establishmentId = staff.getEstablishment().getId();
        var claims = JwtClaimsSet.builder()
                .subject(staff.getUsername())
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .claim(SecurityConfiguration.ESTABLISHMENT_CLAIM, establishmentId)
                .claim(SecurityConfiguration.ROLE_CLAIM, staff.getRole().name())
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new DevToken(staff.getUsername(), staff.getRole().name(), establishmentId, token);
    }

    record DevToken(String username, String role, Long establishmentId, String token) {
    }
}
