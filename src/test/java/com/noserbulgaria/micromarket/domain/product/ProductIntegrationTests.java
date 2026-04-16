package com.noserbulgaria.micromarket.domain.product;

import com.jayway.jsonpath.JsonPath;
import com.noserbulgaria.micromarket.domain.order.OrderRepository;
import com.noserbulgaria.micromarket.security.user.Role;
import com.noserbulgaria.micromarket.security.user.User;
import com.noserbulgaria.micromarket.security.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductIntegrationTests {

  private static final String ADMIN_EMAIL = "admin@micromarket.dev";
  private static final String USER_EMAIL = "user@micromarket.dev";
  private static final String PASSWORD = "user123";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    orderRepository.deleteAll();
    productRepository.deleteAll();
    userRepository.deleteAll();

    userRepository.save(createUser(ADMIN_EMAIL, Role.ADMINISTRATOR));
    userRepository.save(createUser(USER_EMAIL, Role.USER));
  }

  //region Public API Tests
  @Test
  void getAll_publicAccessible_returnsPagedProducts() throws Exception {
    productRepository.saveAndFlush(product("Cola", true, 10L));
    productRepository.saveAndFlush(product("Chips", false, 3L));

    mockMvc.perform(get("/product"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Cola"))
        .andExpect(jsonPath("$.content[0].amount").isNumber())
        .andExpect(jsonPath("$.page.totalElements").value(1));
  }

  @Test
  void getById_withoutAuthentication_returnsProductWithHistory() throws Exception {
    Product product = productRepository.saveAndFlush(product("Water", true, 5L));
    product.setAmount(8L);
    product.setDescription("Updated water");
    productRepository.saveAndFlush(product);

    mockMvc.perform(get("/product/{id}", product.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(product.getId().toString()))
        .andExpect(jsonPath("$.name").value("Water"))
        .andExpect(jsonPath("$.amount").value(8))
        .andExpect(jsonPath("$.history.length()").value(2))
        .andExpect(jsonPath("$.history[0].revisionNumber").isNumber())
        .andExpect(jsonPath("$.history[0].revisionTimestamp").exists());
  }

  @Test
  void getById_disabledProductWithoutAuthentication_returns401() throws Exception {
    Product product = productRepository.saveAndFlush(product("Hidden Water", false, 5L));

    mockMvc.perform(get("/product/{id}", product.getId()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value("Authentication is required to access disabled product 'Hidden Water'"))
        .andExpect(jsonPath("$.instance").value("/product/" + product.getId()));
  }

  @Test
  void getById_disabledProductWithUserToken_returns403() throws Exception {
    String userToken = accessTokenFor(USER_EMAIL, PASSWORD);
    Product product = productRepository.saveAndFlush(product("Hidden Water", false, 5L));

    mockMvc.perform(get("/product/{id}", product.getId())
            .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
        .andExpect(status().isForbidden());
  }

  @Test
  void getById_disabledProductWithAdminToken_returns200() throws Exception {
    String adminToken = accessTokenFor(ADMIN_EMAIL, PASSWORD);
    Product product = productRepository.saveAndFlush(product("Hidden Water", false, 5L));
    product.setAmount(7L);
    product.setDescription("Updated hidden water");
    productRepository.saveAndFlush(product);

    mockMvc.perform(get("/product/{id}", product.getId())
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(product.getId().toString()))
        .andExpect(jsonPath("$.enabled").value(false))
        .andExpect(jsonPath("$.history.length()").value(2));
  }

  @Test
  void getById_enabledProductWithAdminToken_returnsProductWithHistory() throws Exception {
    String adminToken = accessTokenFor(ADMIN_EMAIL, PASSWORD);
    Product product = productRepository.saveAndFlush(product("Admin Water", true, 5L));
    product.setAmount(6L);
    productRepository.saveAndFlush(product);

    mockMvc.perform(get("/product/{id}", product.getId())
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(product.getId().toString()))
        .andExpect(jsonPath("$.enabled").value(true))
        .andExpect(jsonPath("$.history.length()").value(2))
        .andExpect(jsonPath("$.history[0].revisionNumber").isNumber())
        .andExpect(jsonPath("$.history[0].revisionTimestamp").exists());
  }

  @Test
  void getById_nonExistentProduct_returns404() throws Exception {
    UUID productId = UUID.randomUUID();

    mockMvc.perform(get("/product/{id}", productId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Not Found"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("Product with id '%s' not found".formatted(productId)))
        .andExpect(jsonPath("$.instance").value("/product/" + productId));
  }

  @Test
  void getById_invalidUuid_returns400() throws Exception {
    mockMvc.perform(get("/product/{id}", "not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Request Binding Failed"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.instance").value("/product/not-a-uuid"));
  }

  @Test
  void searchByName_publicAccessible_returnsEnabledProduct() throws Exception {
    productRepository.saveAndFlush(product("Sprite", true, 11L));
    productRepository.saveAndFlush(product("Sprite Zero", false, 4L));

    mockMvc.perform(get("/product/search/by-name").param("name", "sprite"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Sprite"))
        .andExpect(jsonPath("$.amount").value(11));
  }

  @Test
  void searchByName_disabledProductWithoutAuthentication_returns401() throws Exception {
    productRepository.saveAndFlush(product("Ghost Soda", false, 9L));

    mockMvc.perform(get("/product/search/by-name").param("name", "ghost soda"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void searchByName_disabledProductWithUserToken_returns403() throws Exception {
    String userToken = accessTokenFor(USER_EMAIL, PASSWORD);
    productRepository.saveAndFlush(product("Ghost Soda", false, 9L));

    mockMvc.perform(get("/product/search/by-name")
            .param("name", "ghost soda")
            .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
        .andExpect(status().isForbidden());
  }

  @Test
  void searchByName_disabledProductWithAdminToken_returns200() throws Exception {
    String adminToken = accessTokenFor(ADMIN_EMAIL, PASSWORD);
    productRepository.saveAndFlush(product("Ghost Soda", false, 9L));

    mockMvc.perform(get("/product/search/by-name")
            .param("name", "ghost soda")
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Ghost Soda"))
        .andExpect(jsonPath("$.enabled").value(false));
  }

  @Test
  void getAllDisabled_withAdminToken_returnsOnlyDisabledProducts() throws Exception {
    String adminToken = accessTokenFor(ADMIN_EMAIL, PASSWORD);
    productRepository.saveAndFlush(product("Juice", true, 4L));
    productRepository.saveAndFlush(product("Hidden", false, 9L));

    mockMvc.perform(get("/product/disabled")
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Hidden"))
        .andExpect(jsonPath("$[0].enabled").value(false));
  }

  @Test
  void getAllDisabled_withoutAuthentication_returns401() throws Exception {
    mockMvc.perform(get("/product/disabled"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getAllDisabled_withUserToken_returns403() throws Exception {
    String userToken = accessTokenFor(USER_EMAIL, PASSWORD);

    mockMvc.perform(get("/product/disabled")
            .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.title").value("Access Denied"))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.detail").value("Access to '/product/disabled' is forbidden."))
        .andExpect(jsonPath("$.instance").value("/product/disabled"));
  }

  @Test
  void searchByName_nonExistentProduct_returns404() throws Exception {
    mockMvc.perform(get("/product/search/by-name").param("name", "missing"))
        .andExpect(status().isNotFound());
  }

  @Test
  void searchByName_missingParam_returns400() throws Exception {
    mockMvc.perform(get("/product/search/by-name"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Request Binding Failed"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.instance").value("/product/search/by-name"));
  }
  //endregion

  //region Admin API Tests
  @Test
  void create_withAdminToken_returns201() throws Exception {
    String adminToken = accessTokenFor(ADMIN_EMAIL, PASSWORD);

    mockMvc.perform(post("/product")
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validWriteBody("Fanta", "Orange soda", 6L)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Fanta"))
        .andExpect(jsonPath("$.amount").value(6));
  }

  @Test
  void create_withoutAuthentication_returns401() throws Exception {
    mockMvc.perform(post("/product")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validWriteBody("Fanta", "Orange soda", 6L)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void create_withUserToken_returns403() throws Exception {
    String userToken = accessTokenFor(USER_EMAIL, PASSWORD);

    mockMvc.perform(post("/product")
            .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validWriteBody("Fanta", "Orange soda", 6L)))
        .andExpect(status().isForbidden());
  }

  @Test
  void create_invalidBody_returns400() throws Exception {
    String adminToken = accessTokenFor(ADMIN_EMAIL, PASSWORD);

    mockMvc.perform(post("/product")
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"","description":"","price":-1,"discount":101,"enabled":true,"amount":-5}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Request Validation Failed"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("Request body validation failed."))
        .andExpect(jsonPath("$.instance").value("/product"))
        .andExpect(jsonPath("$.errors").isArray());
  }

  @Test
  void update_withAdminToken_returns200() throws Exception {
    String adminToken = accessTokenFor(ADMIN_EMAIL, PASSWORD);
    Product product = productRepository.saveAndFlush(product("Tea", true, 2L));

    mockMvc.perform(put("/product/{id}", product.getId())
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validWriteBody("Iced Tea", "Cold tea", 15L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Iced Tea"))
        .andExpect(jsonPath("$.amount").value(15));
  }

  @Test
  void update_withoutAuthentication_returns401() throws Exception {
    Product product = productRepository.saveAndFlush(product("Tea", true, 2L));

    mockMvc.perform(put("/product/{id}", product.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validWriteBody("Iced Tea", "Cold tea", 15L)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void update_withUserToken_returns403() throws Exception {
    String userToken = accessTokenFor(USER_EMAIL, PASSWORD);
    Product product = productRepository.saveAndFlush(product("Tea", true, 2L));

    mockMvc.perform(put("/product/{id}", product.getId())
            .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validWriteBody("Iced Tea", "Cold tea", 15L)))
        .andExpect(status().isForbidden());
  }

  @Test
  void update_nonExistentProduct_returns404() throws Exception {
    String adminToken = accessTokenFor(ADMIN_EMAIL, PASSWORD);

    mockMvc.perform(put("/product/{id}", UUID.randomUUID())
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validWriteBody("Iced Tea", "Cold tea", 15L)))
        .andExpect(status().isNotFound());
  }

  @Test
  void update_invalidUuid_returns400() throws Exception {
    String adminToken = accessTokenFor(ADMIN_EMAIL, PASSWORD);

    mockMvc.perform(put("/product/{id}", "not-a-uuid")
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validWriteBody("Iced Tea", "Cold tea", 15L)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void update_invalidBody_returns400() throws Exception {
    String adminToken = accessTokenFor(ADMIN_EMAIL, PASSWORD);
    Product product = productRepository.saveAndFlush(product("Tea", true, 2L));

    mockMvc.perform(put("/product/{id}", product.getId())
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"","description":"","price":0,"discount":-1,"enabled":true,"amount":-1}
                """))
        .andExpect(status().isBadRequest());
  }

  //endregion

  private User createUser(String email, Role role) {
    User user = new User();
    user.setEmail(email);
    user.setPassword(Objects.requireNonNull(passwordEncoder.encode(PASSWORD)));
    user.setRole(role);
    return user;
  }

  private Product product(String name, boolean enabled, long amount) {
    Product product = new Product();
    product.setName(name);
    product.setDescription(name + " description");
    product.setPrice(BigDecimal.valueOf(2));
    product.setDiscount(0);
    product.setEnabled(enabled);
    product.setAmount(amount);
    return product;
  }

  private String validWriteBody(String name, String description, long amount) {
    return """
        {"name":"%s","description":"%s","price":2.5,"discount":0.0,"enabled":false,"amount":%d}
        """.formatted(name, description, amount);
  }

  private String accessTokenFor(String email, String password) throws Exception {
    MvcResult loginResult = mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "%s", "password": "%s"}
                """.formatted(email, password)))
        .andExpect(status().isOk())
        .andReturn();

    return JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");
  }

  private String bearer(String accessToken) {
    return "Bearer " + accessToken;
  }
}
