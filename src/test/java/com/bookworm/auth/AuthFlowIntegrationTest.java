package com.bookworm.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bookworm.user.Role;
import com.bookworm.user.User;
import com.bookworm.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Runs the whole application (real security filter chain, real JWT signing and verification, in-memory database)
 * to prove that authentication, logout revocation and role checks work together.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:authflow;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "bookworm.security.jwt.secret=test-only-secret-that-is-at-least-32-bytes-long"
})
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    private static final String PASSWORD = "S3cur3P@ss!";
    private static final Pattern TOKEN = Pattern.compile("\"accessToken\":\"([^\"]+)\"");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static String register(String email) {
        return "{\"name\":\"Arjun Patel\",\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD);
    }

    private String registerAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(register(email)))
                .andExpect(status().isCreated())
                .andReturn();
        Matcher matcher = TOKEN.matcher(result.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void registerReturnsTokenAndProfileWithoutLeakingPassword() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(register("new.user@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.email").value("new.user@example.com"))
                .andExpect(jsonPath("$.user.giftPoints").value(0))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(PASSWORD))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("passwordHash"))));
    }

    @Test
    void passwordIsStoredHashed() throws Exception {
        registerAndGetToken("hashed@example.com");

        User stored = userRepository.findByEmail("hashed@example.com").orElseThrow();

        assertThat(stored.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, stored.getPasswordHash())).isTrue();
        assertThat(stored.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void duplicateEmailIgnoringCaseIsConflict() throws Exception {
        registerAndGetToken("dupe@example.com");

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(register("DUPE@Example.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void weakPasswordIsRejectedWithoutEchoingIt() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Arjun\",\"email\":\"weak@example.com\",\"password\":\"alllowercase1\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("alllowercase1"))));
    }

    @Test
    void loginWithCorrectCredentialsWorksCaseInsensitivelyOnEmail() throws Exception {
        registerAndGetToken("login@example.com");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"LOGIN@example.com\",\"password\":\"%s\"}".formatted(PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameResponse() throws Exception {
        registerAndGetToken("known@example.com");

        String wrongPassword = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"known@example.com\",\"password\":\"Wr0ng!Password\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String unknownEmail = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"Wr0ng!Password\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(wrongPassword).contains("Invalid email or password.");
        assertThat(unknownEmail).contains("Invalid email or password.");
    }

    @Test
    void protectedEndpointWithoutTokenIs401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void tokenGrantsAccessToOwnProfileAndGiftPoints() throws Exception {
        String token = registerAndGetToken("me@example.com");

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"));
        mockMvc.perform(get("/users/me/gift-points").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.giftPoints").value(0));
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        String token = registerAndGetToken("tamper@example.com");
        String tampered = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(tampered)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenIsUnusableAfterLogout() throws Exception {
        String token = registerAndGetToken("logout@example.com");

        mockMvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesOnlyThePresentedToken() throws Exception {
        String first = registerAndGetToken("twice@example.com");
        MvcResult login = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"twice@example.com\",\"password\":\"%s\"}".formatted(PASSWORD)))
                .andReturn();
        Matcher matcher = TOKEN.matcher(login.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        String second = matcher.group(1);

        mockMvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(first)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(second)))
                .andExpect(status().isOk());
    }

    @Test
    void logoutWithoutTokenIs401() throws Exception {
        mockMvc.perform(post("/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotCreateCategoriesButAdminCan() throws Exception {
        String customer = registerAndGetToken("customer@example.com");
        userRepository.save(new User("Admin", "admin@example.com", passwordEncoder.encode(PASSWORD), Role.ADMIN));
        MvcResult adminLogin = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"%s\"}".formatted(PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        Matcher matcher = TOKEN.matcher(adminLogin.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        String admin = matcher.group(1);

        mockMvc.perform(post("/categories").header(HttpHeaders.AUTHORIZATION, bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Poetry\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/categories").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Poetry\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void catalogBrowsingNeedsNoToken() throws Exception {
        mockMvc.perform(get("/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/books")).andExpect(status().isOk());
        mockMvc.perform(get("/publishers")).andExpect(status().isOk());
    }

    @Test
    void updatingProfileChangesNameEmailAndPassword() throws Exception {
        String token = registerAndGetToken("before@example.com");

        mockMvc.perform(patch("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\",\"email\":\"After@Example.com\",\"password\":\"N3wS3cur3P@ss!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.email").value("after@example.com"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"after@example.com\",\"password\":\"N3wS3cur3P@ss!\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"after@example.com\",\"password\":\"%s\"}".formatted(PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void takingAnotherAccountsEmailIsConflict() throws Exception {
        registerAndGetToken("taken@example.com");
        String token = registerAndGetToken("mine@example.com");

        mockMvc.perform(patch("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"taken@example.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void emptyPatchLeavesProfileUnchanged() throws Exception {
        String token = registerAndGetToken("same@example.com");

        mockMvc.perform(patch("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Arjun Patel"))
                .andExpect(jsonPath("$.email").value("same@example.com"));
    }

    @Test
    void invalidProfileUpdateIsRejectedWithoutEchoingThePassword() throws Exception {
        String token = registerAndGetToken("invalid@example.com");

        mockMvc.perform(patch("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"short\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("short\""))));
    }
}
