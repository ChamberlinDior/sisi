package com.pnis.backend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Enveloppe de réponse homogène conforme au §11 du CDC.
 * Tous les endpoints retournent cette structure.
 *
 * NOTE : @Builder Lombok ne fonctionne pas correctement sur les classes
 * génériques avec méthodes statiques typées → constructeur privé manuel.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final String errorCode;
    private final List<String> errors;
    private final Map<String, List<String>> validationErrors;
    private final String correlationId;
    private final Instant timestamp;
    private final PageMeta page;

    private ApiResponse(boolean success, String message, T data, String errorCode,
                        List<String> errors, Map<String, List<String>> validationErrors,
                        String correlationId, Instant timestamp, PageMeta page) {
        this.success          = success;
        this.message          = message;
        this.data             = data;
        this.errorCode        = errorCode;
        this.errors           = errors;
        this.validationErrors = validationErrors;
        this.correlationId    = correlationId;
        this.timestamp        = timestamp;
        this.page             = page;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data, null, null, null, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, null, null, null, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, "Ressource créée avec succès.", data, null, null, null, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> noContent(String message) {
        return new ApiResponse<>(true, message, null, null, null, null, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, message, null, errorCode, null, null, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> validationError(Map<String, List<String>> validationErrors) {
        return new ApiResponse<>(false, "Les données fournies sont invalides.", null,
                "VALIDATION_FAILED", null, validationErrors, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> paged(T data, PageMeta page) {
        return new ApiResponse<>(true, null, data, null, null, null, null, Instant.now(), page);
    }

    /** Métadonnées de pagination */
    @Getter
    public static class PageMeta {
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;
        private final boolean first;
        private final boolean last;

        public PageMeta(int page, int size, long totalElements, int totalPages,
                        boolean first, boolean last) {
            this.page          = page;
            this.size          = size;
            this.totalElements = totalElements;
            this.totalPages    = totalPages;
            this.first         = first;
            this.last          = last;
        }
    }
}
