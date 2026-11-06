package br.com.beegoodfood.comum;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail corpoInvalido(MethodArgumentNotValidException error) {
        var body = problem(HttpStatus.BAD_REQUEST, "Dados inválidos", "Confira os campos informados.");
        Map<String, String> campos = new LinkedHashMap<>();
        error.getBindingResult().getFieldErrors().forEach(field -> campos.put(field.getField(), field.getDefaultMessage()));
        body.setProperty("campos", campos);
        return body;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail jsonInvalido() {
        return problem(HttpStatus.BAD_REQUEST, "JSON inválido", "Confira o formato e os campos permitidos.");
    }

    // Somente o nome da constraint e usado; a mensagem do banco pode conter valores enviados e nunca e devolvida.
    private static final Map<String, String> CONFLITOS = Map.of(
            "uk_usuario_email_normalizado", "Não foi possível cadastrar com os dados informados. O e-mail pode já estar em uso.",
            "uk_categoria_nome_normalizado", "Já existe uma categoria com esse nome.",
            "uk_categoria_slug", "Já existe uma categoria com esse identificador.",
            "uk_oferta_produto_ativa", "Este produto já possui uma oferta publicada ou pausada.",
            "uk_pedido_cliente_idempotencia", "Esta chave de idempotência já foi usada em outro pedido.");

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflito(DataIntegrityViolationException error) {
        String causa = String.valueOf(error.getMostSpecificCause().getMessage());
        String detalhe = CONFLITOS.entrySet().stream().filter(e -> causa.contains(e.getKey()))
                .map(Map.Entry::getValue).findFirst()
                .orElse("Não foi possível salvar: os dados conflitam com um registro existente.");
        return problem(HttpStatus.CONFLICT, "Conflito de dados", detalhe);
    }

    @ExceptionHandler(RegraNegocioException.class)
    ProblemDetail regraNegocio(RegraNegocioException error) {
        var body = problem(HttpStatus.valueOf(error.status()), error.titulo(), error.getMessage());
        if (!error.campos().isEmpty()) { body.setProperty("campos", new LinkedHashMap<>(error.campos())); }
        return body;
    }

    @ExceptionHandler({PessimisticLockingFailureException.class, CannotAcquireLockException.class})
    ProblemDetail recursoOcupado() {
        return problem(HttpStatus.CONFLICT, "Recurso ocupado", "Outra operação alterou este registro ao mesmo tempo. Tente novamente.");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ProblemDetail cabecalhoAusente(MissingRequestHeaderException error) {
        var body = problem(HttpStatus.BAD_REQUEST, "Cabeçalho obrigatório", "Informe o cabeçalho obrigatório.");
        body.setProperty("campos", Map.of(error.getHeaderName(), "Cabeçalho obrigatório."));
        return body;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail parametroAusente(MissingServletRequestParameterException error) {
        var body = problem(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório", "Informe os parâmetros obrigatórios.");
        body.setProperty("campos", Map.of(error.getParameterName(), "Parâmetro obrigatório."));
        return body;
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail credenciaisInvalidas() {
        return problem(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", "E-mail ou senha inválidos.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail acessoNegado() {
        return problem(HttpStatus.FORBIDDEN, "Acesso negado", "Você não tem permissão para esta operação.");
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException error) {
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", error.getMessage());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail validacao(HandlerMethodValidationException error) {
        var body = problem(HttpStatus.BAD_REQUEST, "Parâmetros inválidos", "Confira os parâmetros informados.");
        Map<String, String> campos = new LinkedHashMap<>();
        error.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(message -> campos.put(name, message.getDefaultMessage()));
        });
        body.setProperty("campos", campos);
        return body;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail tipoInvalido(MethodArgumentTypeMismatchException error) {
        Class<?> tipo = error.getRequiredType();
        String mensagem = tipo != null && java.time.temporal.Temporal.class.isAssignableFrom(tipo)
                ? "Informe data e hora no formato AAAA-MM-DDTHH:MM."
                : tipo != null && tipo.isEnum() ? "Valor não permitido."
                : "Informe um número inteiro válido.";
        var body = problem(HttpStatus.BAD_REQUEST, "Parâmetro inválido", mensagem);
        body.setProperty("campos", Map.of(error.getName(), mensagem));
        return body;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail rotaInexistente() {
        return problem(HttpStatus.NOT_FOUND, "Rota não encontrada", "O endereço solicitado não existe.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> metodoNaoPermitido(HttpRequestMethodNotSupportedException error) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(error.getHeaders())
                .body(problem(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido", "Esta rota não permite a operação solicitada."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ProblemDetail tipoDeCorpoNaoSuportado() {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato não suportado", "Envie o corpo como application/json.");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ProblemDetail respostaNaoSuportada() {
        return problem(HttpStatus.NOT_ACCEPTABLE, "Formato não disponível", "Esta API responde em JSON.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception error) {
        LOGGER.error("Falha inesperada ao atender requisicao", error);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Não foi possível atender a solicitação.");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("about:blank"));
        problem.setTitle(title);
        return problem;
    }
}
