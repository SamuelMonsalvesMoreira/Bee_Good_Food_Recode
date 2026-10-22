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

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

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
        var body = problem(HttpStatus.BAD_REQUEST, "Parâmetro inválido", "Informe um número inteiro válido.");
        body.setProperty("campos", Map.of(error.getName(), "Informe um número inteiro válido."));
        return body;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail rotaInexistente() {
        return problem(HttpStatus.NOT_FOUND, "Rota não encontrada", "O endereço solicitado não existe.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ProblemDetail metodoNaoPermitido() {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido", "Esta rota não permite a operação solicitada.");
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
