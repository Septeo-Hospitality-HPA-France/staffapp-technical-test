package com.staffapp.housekeeping.security;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecurityConfigurationTest {

    private final SecurityConfiguration configuration = new SecurityConfiguration();

    @Test
    @DisplayName("Rejects a signing key shorter than the 256 bits HS256 requires")
    void rejectsASigningKeyTooShortForHs256() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> configuration.jwtDecoder("x".repeat(31)))
                .withMessageContaining("256 bits");
    }

    @Test
    @DisplayName("Accepts a 256-bit signing key")
    void acceptsA256BitSigningKey() {
        assertThatNoException().isThrownBy(() -> configuration.jwtDecoder("x".repeat(32)));
    }
}
