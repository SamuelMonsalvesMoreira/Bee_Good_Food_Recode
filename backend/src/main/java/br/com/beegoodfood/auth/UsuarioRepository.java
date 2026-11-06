package br.com.beegoodfood.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailNormalizado(String emailNormalizado);
    Optional<Usuario> findByIdAndAtivoTrue(Long id);

    // Idempotente e seguro sob concorrencia: a chave primaria (usuario, papel) impede duplicidade.
    @Modifying
    @Query(nativeQuery = true, value = "INSERT INTO usuario_papel (usuario_id, papel_codigo) VALUES (:usuarioId, :papel) "
            + "ON DUPLICATE KEY UPDATE papel_codigo = papel_codigo")
    int concederPapel(@Param("usuarioId") long usuarioId, @Param("papel") String papel);
}
