package br.com.beegoodfood.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailNormalizado(String emailNormalizado);
    Optional<Usuario> findByIdAndAtivoTrue(Long id);
}
