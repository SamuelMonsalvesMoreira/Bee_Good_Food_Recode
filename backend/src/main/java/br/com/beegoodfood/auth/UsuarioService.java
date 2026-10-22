package br.com.beegoodfood.auth;

import br.com.beegoodfood.comum.PaginaResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UsuarioService {
    private final UsuarioRepository repository;
    private final PasswordEncoder passwords;
    private final Clock clock;
    private final String dummyHash;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwords, Clock clock) {
        this.repository = repository;
        this.passwords = passwords;
        this.clock = clock;
        this.dummyHash = passwords.encode(java.util.UUID.randomUUID().toString());
    }

    @Transactional
    public UsuarioResponse cadastrar(CadastroRequest request) {
        // Unicidade e concorrencia garantidas pela constraint MySQL. Papel nunca vem do cliente.
        var usuario = new Usuario(request, normalizar(request.email()), passwords.encode(request.senha()),
                LocalDateTime.now(clock));
        return UsuarioResponse.from(repository.saveAndFlush(usuario));
    }

    public UsuarioResponse autenticar(LoginRequest request) {
        var usuario = repository.findByEmailNormalizado(normalizar(request.email()));
        // Faz o trabalho de hash tambem quando a conta nao existe; erro identico para conta inativa.
        String hash = usuario.map(Usuario::senhaHash).filter(h -> h.startsWith("$2")).orElse(dummyHash);
        boolean confere = passwords.matches(request.senha(), hash);
        if (!confere || usuario.isEmpty() || !usuario.get().ativo()) {
            throw new BadCredentialsException("E-mail ou senha inválidos.");
        }
        return UsuarioResponse.from(usuario.get());
    }

    public UsuarioResponse ativo(long id) {
        return repository.findByIdAndAtivoTrue(id).map(UsuarioResponse::from)
                .orElseThrow(() -> new BadCredentialsException("Sessão inválida."));
    }

    public PaginaResponse<UsuarioResponse> listar(int pagina, int tamanho) {
        return PaginaResponse.from(repository.findAll(PageRequest.of(pagina, tamanho, Sort.by("id")))
                .map(UsuarioResponse::from));
    }

    private String normalizar(String email) { return email.strip().toLowerCase(Locale.ROOT); }
}
