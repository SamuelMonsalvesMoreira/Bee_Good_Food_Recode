package br.com.beegoodfood.auth;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    public static final String ISSUER = "bee-good-food";
    public static final String AUDIENCE = "bee-good-food-api";
    public static final long VALIDADE_SEGUNDOS = 900;
    private final JwtEncoder encoder;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, Clock clock) { this.encoder = encoder; this.clock = clock; }

    public TokenResponse emitir(UsuarioResponse usuario) {
        var agora = clock.instant();
        var claims = JwtClaimsSet.builder().issuer(ISSUER).audience(List.of(AUDIENCE))
                .subject(Long.toString(usuario.id())).issuedAt(agora).notBefore(agora)
                .expiresAt(agora.plusSeconds(VALIDADE_SEGUNDOS)).id(UUID.randomUUID().toString()).build();
        var jwt = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
        return new TokenResponse(jwt.getTokenValue(), "Bearer", VALIDADE_SEGUNDOS, usuario);
    }

    public record TokenResponse(String accessToken, String tipoToken, long expiraEmSegundos, UsuarioResponse usuario) {
        @Override public String toString() { return "TokenResponse[conteudo omitido]"; }
    }
}
