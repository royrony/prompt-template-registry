package dev.ssec2.ptr.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ssec2.ptr.domain.PromptTemplate;
import dev.ssec2.ptr.service.PromptTemplateService;
import dev.ssec2.ptr.service.RenderResult;
import dev.ssec2.ptr.service.TemplateNotFoundException;
import dev.ssec2.ptr.service.VariableValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T5 - web slice (spec FR-001, FR-008, FR-009, FR-010; 404/400 via ApiExceptionHandler).
 */
@WebMvcTest(PromptTemplateController.class)
@Import(ApiExceptionHandler.class)
class PromptTemplateControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PromptTemplateService service;

    private PromptTemplate template(String name, int version, String body) {
        PromptTemplate t = new PromptTemplate(name, version, body);
        return t;
    }

    @Test
    void create_returns201WithBody() throws Exception {
        when(service.register(eq("greeting"), any())).thenReturn(template("greeting", 1, "Hi {{name}}"));
        when(service.declaredVariablesOf(any())).thenReturn(List.of("name"));

        mvc.perform(post("/api/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "greeting", "body", "Hi {{name}}"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("greeting"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.declaredVariables[0]").value("name"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mvc.perform(post("/api/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "", "body", "Hi"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void create_illegalNameChars_returns400() throws Exception {
        mvc.perform(post("/api/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "bad name!", "body", "Hi"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returns200() throws Exception {
        when(service.listLatest()).thenReturn(List.of(template("greeting", 2, "g")));
        when(service.declaredVariablesOf(any())).thenReturn(List.of());

        mvc.perform(get("/api/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("greeting"))
                .andExpect(jsonPath("$[0].version").value(2));
    }

    @Test
    void getLatest_returns200() throws Exception {
        when(service.getLatest("greeting")).thenReturn(template("greeting", 3, "g"));
        when(service.declaredVariablesOf(any())).thenReturn(List.of());

        mvc.perform(get("/api/templates/greeting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    void getLatest_unknown_returns404() throws Exception {
        when(service.getLatest("missing")).thenThrow(new TemplateNotFoundException("missing"));

        mvc.perform(get("/api/templates/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getVersions_returns200() throws Exception {
        when(service.getVersions("greeting"))
                .thenReturn(List.of(template("greeting", 1, "v1"), template("greeting", 2, "v2")));
        when(service.declaredVariablesOf(any())).thenReturn(List.of());

        mvc.perform(get("/api/templates/greeting/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void render_returns200() throws Exception {
        when(service.render(eq("greeting"), any()))
                .thenReturn(new RenderResult("greeting", 1, "Hi Rony", Map.of("name", "Rony")));

        mvc.perform(post("/api/templates/greeting/render")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("variables", Map.of("name", "Rony")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendered").value("Hi Rony"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void render_missingVariable_returns400WithDetails() throws Exception {
        when(service.render(eq("greeting"), any()))
                .thenThrow(new VariableValidationException(List.of("product"), List.of()));

        mvc.perform(post("/api/templates/greeting/render")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("variables", Map.of("name", "Rony")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void render_unknownName_returns404() throws Exception {
        when(service.render(eq("missing"), any()))
                .thenThrow(new TemplateNotFoundException("missing"));

        mvc.perform(post("/api/templates/missing/render")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("variables", Map.of()))))
                .andExpect(status().isNotFound());
    }
}
