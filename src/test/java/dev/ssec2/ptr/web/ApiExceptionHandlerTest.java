package dev.ssec2.ptr.web;

import dev.ssec2.ptr.domain.MalformedTemplateException;
import dev.ssec2.ptr.service.TemplateNotFoundException;
import dev.ssec2.ptr.service.VariableValidationException;
import dev.ssec2.ptr.web.dto.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T6 - the one standard error shape across every exception type (spec FR-007, FR-009, SC-004).
 * Pure unit test: the handler is exercised directly with a mock request, no Spring context.
 */
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI(uri);
        return req;
    }

    @Test
    void notFound_maps404_withAllStandardFields() {
        ResponseEntity<ApiError> response = handler.handleNotFound(
                new TemplateNotFoundException("missing"), request("/api/templates/missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.status()).isEqualTo(404);
        assertThat(body.error()).isEqualTo("Not Found");
        assertThat(body.message()).contains("missing");
        assertThat(body.path()).isEqualTo("/api/templates/missing");
        assertThat(body.details()).isNull();
    }

    @Test
    void malformed_maps400() {
        ResponseEntity<ApiError> response = handler.handleMalformed(
                new MalformedTemplateException("bad placeholder"), request("/api/templates"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).contains("bad placeholder");
    }

    @Test
    void variableValidation_maps400_withMissingAndUnknownInDetails() {
        ResponseEntity<ApiError> response = handler.handleVariableValidation(
                new VariableValidationException(List.of("product"), List.of("extra")),
                request("/api/templates/greeting/render"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.details()).contains("missing: product", "unknown: extra");
    }

    @Test
    void allErrorBodies_shareTheSameShape() {
        ApiError notFound = handler.handleNotFound(
                new TemplateNotFoundException("x"), request("/a")).getBody();
        ApiError malformed = handler.handleMalformed(
                new MalformedTemplateException("y"), request("/b")).getBody();
        ApiError validation = handler.handleVariableValidation(
                new VariableValidationException(List.of("m"), List.of()), request("/c")).getBody();

        // Same record type, every common field populated the same way.
        for (ApiError e : List.of(notFound, malformed, validation)) {
            assertThat(e.timestamp()).isNotNull();
            assertThat(e.status()).isPositive();
            assertThat(e.error()).isNotBlank();
            assertThat(e.message()).isNotBlank();
            assertThat(e.path()).isNotBlank();
        }
    }
}
