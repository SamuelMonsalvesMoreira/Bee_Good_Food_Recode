package br.com.beegoodfood.config;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SecurityConfigTest {
    @Test
    void invalidBase64IsRejectedWithoutEchoingSecret() {
        assertThatThrownBy(() -> new SecurityConfig().jwtKey("segredo-invalido!"))
                .isInstanceOf(IllegalStateException.class).hasMessage("JWT_SECRET_BASE64 deve conter Base64 válido.");
    }

    @Test
    void shortKeyIsRejected() {
        assertThatThrownBy(() -> new SecurityConfig().jwtKey(Base64.getEncoder().encodeToString(new byte[31])))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("32 bytes");
    }

    @Test
    void randomlyGeneratedKeyIsAccepted() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        var key = new SecurityConfig().jwtKey(Base64.getEncoder().encodeToString(bytes));
        assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(key.getEncoded()).isEqualTo(bytes);
    }
}
