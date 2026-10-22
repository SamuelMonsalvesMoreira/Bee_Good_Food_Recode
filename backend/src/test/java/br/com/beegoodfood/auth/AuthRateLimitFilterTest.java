package br.com.beegoodfood.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {
    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void twentyRequestsAreAllowedThen429WithRetryAfter() throws Exception {
        var filter = new AuthRateLimitFilter(20, new MutableClock());
        for (int i = 0; i < 20; i++) { assertThat(call(filter, "127.0.0.1", "/api/v1/auth/login").getStatus()).isEqualTo(204); }
        var rejected = call(filter, "127.0.0.1", "/api/v1/auth/login");
        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(rejected.getHeader("Retry-After")).isEqualTo("60");
        assertThat(rejected.getContentType()).startsWith("application/problem+json");
    }

    @Test
    void loginAndSignupShareLimitButOtherAddressesDoNot() throws Exception {
        var filter = new AuthRateLimitFilter(1, new MutableClock());
        assertThat(call(filter, "127.0.0.1", "/api/v1/auth/login").getStatus()).isEqualTo(204);
        assertThat(call(filter, "127.0.0.1", "/api/v1/auth/cadastro").getStatus()).isEqualTo(429);
        assertThat(call(filter, "127.0.0.2", "/api/v1/auth/cadastro").getStatus()).isEqualTo(204);
    }

    @Test
    void windowExpiresWithoutSleepingAndNonAuthRoutesAreNotLimited() throws Exception {
        var clock = new MutableClock();
        var filter = new AuthRateLimitFilter(1, clock);
        assertThat(call(filter, "127.0.0.1", "/api/v1/auth/login").getStatus()).isEqualTo(204);
        assertThat(call(filter, "127.0.0.1", "/outra-rota").getStatus()).isEqualTo(204);
        clock.now = clock.now.plusSeconds(60);
        assertThat(call(filter, "127.0.0.1", "/api/v1/auth/login").getStatus()).isEqualTo(204);
    }

    private MockHttpServletResponse call(AuthRateLimitFilter filter, String address, String path) throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        request.setRemoteAddr(address);
        // Cabeçalhos nao confiaveis nao alteram a chave usada para o limite.
        request.addHeader("X-Forwarded-For", java.util.UUID.randomUUID().toString());
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(204));
        return response;
    }
}
