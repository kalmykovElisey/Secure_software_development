package com.example.lab2.exception;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApi(
            ApiException exception,
            WebRequest request
    ) {
        return reply(
                exception.status(),
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<Object> handleMissingRecord(WebRequest request) {
        return reply(
                HttpStatus.NOT_FOUND,
                "Запись не найдена или была удалена другим запросом",
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleIntegrity(WebRequest request) {
        return reply(
                HttpStatus.CONFLICT,
                "Операция нарушает ограничения данных. "
                        + "Проверьте уникальность и связанные записи",
                request
        );
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<Object> handleConcurrency(WebRequest request) {
        return reply(
                HttpStatus.CONFLICT,
                "Данные изменились параллельно. Повторите запрос",
                request
        );
    }

    @ExceptionHandler({
            DataAccessResourceFailureException.class,
            CannotCreateTransactionException.class
    })
    public ResponseEntity<Object> handleUnavailableDatabase(
            Exception exception,
            WebRequest request
    ) {
        LOG.error("База данных недоступна", exception);

        return reply(
                HttpStatus.SERVICE_UNAVAILABLE,
                "База данных временно недоступна",
                request
        );
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Object> handleDatabase(
            DataAccessException exception,
            WebRequest request
    ) {
        LOG.error("Ошибка операции с базой данных", exception);

        return reply(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Не удалось выполнить операцию с базой данных",
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String message = switch (status.value()) {
            case 400 ->
                    "Некорректный JSON, тип параметра или неизвестное поле запроса";
            case 404 ->
                    "Ресурс не найден";
            case 405 ->
                    "HTTP-метод не поддерживается для этого адреса";
            case 415 ->
                    "Используйте Content-Type: application/json";
            default ->
                    "Запрос не может быть обработан";
        };

        return new ResponseEntity<>(
                error(status, message, request),
                headers,
                status
        );
    }

    private ResponseEntity<Object> reply(
            HttpStatusCode status,
            String message,
            WebRequest request
    ) {
        return ResponseEntity.status(status)
                .body(error(status, message, request));
    }

    private ApiError error(
            HttpStatusCode status,
            String message,
            WebRequest request
    ) {
        HttpStatus known = HttpStatus.resolve(status.value());

        String path = ((ServletWebRequest) request)
                .getRequest()
                .getRequestURI();

        return new ApiError(
                clock.instant(),
                status.value(),
                known == null ? "Error" : known.getReasonPhrase(),
                message,
                path
        );
    }
}