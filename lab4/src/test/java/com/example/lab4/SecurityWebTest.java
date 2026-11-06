package com.example.lab4;

import com.example.lab4.controller.*;
import com.example.lab4.dto.AuthRequests.AccountView;
import com.example.lab4.model.*;
import com.example.lab4.repository.*;
import com.example.lab4.security.*;
import com.example.lab4.service.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class, UserController.class, RecyclingTypeController.class,
        CollectionPointController.class, RecyclingBatchController.class,
        BonusEntryController.class, OperationsController.class},
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfig.class, SecurityErrors.class, AccessRules.class})
class SecurityWebTest {
    private static final String PASSWORD = "Long-Password!42";
    private static final String HASH = new BCryptPasswordEncoder(4).encode(PASSWORD);
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean AuthService auth;
    @MockitoBean RecyclingService recycling;
    @MockitoBean RecyclingBatchRepository batches;
    @MockitoBean BonusEntryRepository bonuses;

    @BeforeEach
    void accounts() {
        for (String role : List.of("USER", "OPERATOR", "ADMIN")) {
            when(auth.loadUserByUsername(role)).thenReturn(
                    new AccountPrincipal(10L, role, HASH, role, true));
        }
        when(auth.loadUserByUsername("BLOCKED")).thenReturn(
                new AccountPrincipal(11L, "BLOCKED", HASH, "USER", false));
    }

    @Test
    void authenticationFailuresHaveGenericJsonAndChallenge() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/auth/me").with(httpBasic("USER", "wrong")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").with(httpBasic("BLOCKED", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void writeWithoutCsrfOrWithFakeCsrfIsForbidden() throws Exception {
        mvc.perform(post("/api/users").with(httpBasic("ADMIN", PASSWORD))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").exists());
        mvc.perform(post("/api/auth/register").with(csrf().useInvalidToken())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(recycling);
    }

    @Test
    void csrfEndpointTokenWorksWithSameSessionForPublicRegistration() throws Exception {
        var result = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        String token = mapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/api/recycling-types").session(session).with(httpBasic("USER", PASSWORD)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/recycling-types").session(session).with(httpBasic("OPERATOR", PASSWORD)))
                .andExpect(status().isOk());
        when(auth.register(any())).thenReturn(new AccountView(20L, "Анна", "a@example.com", "USER", true));
        mvc.perform(post("/api/auth/register").session(session).header("X-CSRF-TOKEN", token)
                        .contentType("application/json")
                        .content("{\"name\":\"Анна\",\"email\":\"a@example.com\",\"password\":\"Long-Password!42\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(post("/api/auth/register").header("X-CSRF-TOKEN", token)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrationCannotSupplyRole() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                        .content("{\"name\":\"A\",\"email\":\"a@example.com\",\"password\":\"Long-Password!42\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
        verify(auth, never()).register(any());
    }

    @ParameterizedTest
    @CsvSource({
        "USER,GET,/api/users", "USER,GET,/api/users/11", "USER,PATCH,/api/users/11",
        "USER,POST,/api/users", "USER,DELETE,/api/users/11",
        "USER,POST,/api/recycling-types", "USER,PATCH,/api/recycling-types/1", "USER,DELETE,/api/recycling-types/1",
        "USER,POST,/api/collection-points", "USER,PATCH,/api/collection-points/1", "USER,DELETE,/api/collection-points/1",
        "USER,POST,/api/recycling-batches", "USER,PATCH,/api/recycling-batches/1", "USER,DELETE,/api/recycling-batches/1",
        "USER,POST,/api/bonus-entries", "USER,PATCH,/api/bonus-entries/1", "USER,DELETE,/api/bonus-entries/1",
        "USER,POST,/api/operations/accept", "USER,POST,/api/operations/batches/1/correct-weight",
        "USER,POST,/api/operations/users/10/accrue-bonuses", "USER,PATCH,/api/auth/accounts/11",
        "OPERATOR,POST,/api/users", "OPERATOR,PATCH,/api/users/11", "OPERATOR,DELETE,/api/users/11",
        "OPERATOR,POST,/api/recycling-types", "OPERATOR,PATCH,/api/recycling-types/1", "OPERATOR,DELETE,/api/recycling-types/1",
        "OPERATOR,POST,/api/collection-points", "OPERATOR,PATCH,/api/collection-points/1", "OPERATOR,DELETE,/api/collection-points/1",
        "OPERATOR,DELETE,/api/recycling-batches/1", "OPERATOR,POST,/api/bonus-entries",
        "OPERATOR,PATCH,/api/bonus-entries/1", "OPERATOR,DELETE,/api/bonus-entries/1",
        "OPERATOR,POST,/api/operations/users/10/accrue-bonuses", "OPERATOR,PATCH,/api/auth/accounts/11"
    })
    void roleRestrictionsCoverCrudAndBusinessRoutes(String role, String method, String path) throws Exception {
        mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), path)
                        .with(httpBasic(role, PASSWORD)).with(csrf())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(recycling);
    }

    @Test
    void userSeesOnlyOwnBatchesAndCannotGuessForeignId() throws Exception {
        RecyclingBatch own = batch(1L, 10L), foreign = batch(2L, 11L);
        when(recycling.listBatches()).thenReturn(List.of(own, foreign));
        when(batches.findById(2L)).thenReturn(Optional.of(foreign));
        mvc.perform(get("/api/recycling-batches").with(httpBasic("USER", PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(10));
        mvc.perform(get("/api/recycling-batches/2").with(httpBasic("USER", PASSWORD)))
                .andExpect(status().isForbidden());
        verify(recycling, never()).getBatch(2L);
    }

    @Test
    void basicCredentialsRequiredEvenWhenSessionExists() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/api/recycling-types").session(session).with(httpBasic("USER", PASSWORD)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/recycling-types").session(session))
                .andExpect(status().isUnauthorized());
    }

    private RecyclingBatch batch(Long id, Long userId) {
        return new RecyclingBatch(id, userId, 1L, 1L, BigDecimal.ONE,
                Instant.now(), BatchStatus.ACCEPTED, BigDecimal.TEN);
    }
}
