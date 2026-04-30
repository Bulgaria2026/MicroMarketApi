package com.noserbulgaria.micromarket.auth.user;

import com.noserbulgaria.micromarket.customer.Customer;
import com.noserbulgaria.micromarket.customer.CustomerRepository;
import com.noserbulgaria.micromarket.customer.Profile;
import com.noserbulgaria.micromarket.customer.ProfileRepository;
import com.noserbulgaria.micromarket.exception.StripeApiException;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountControllerTest {

  private static final String ADMIN_EMAIL = "admin@micromarket.dev";
  private static final String USER_EMAIL = "user-controller-user@micromarket.dev";
  private static final String EXISTING_EMAIL = "user-controller-existing@micromarket.dev";
  private static final String UPDATED_EMAIL = "user-controller-updated@micromarket.dev";
  private static final String PASSWORD = "user12345";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CustomerRepository customerRepository;

  @Autowired
  private ProfileRepository profileRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @MockitoBean
  private StripePaymentProvider stripePaymentProvider;

  private User userToUpdate;

  @BeforeEach
  void setUp() {
    userToUpdate = createUserWithProfile(USER_EMAIL, Role.USER);
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchAccount_withValidRole_updatesRole() throws Exception {
    mockMvc.perform(patch("/account/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"role": "ADMINISTRATOR"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ADMINISTRATOR"))
        .andExpect(jsonPath("$.email").value(emailFor(userToUpdate)))
        .andExpect(jsonPath("$.status").value(AccountStatus.ACTIVE.name()));

    User updatedUser = userRepository.findById(userToUpdate.getId()).orElseThrow();
    Assertions.assertEquals(Role.ADMINISTRATOR, updatedUser.getRole());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchAccount_withValidEmail_updatesEmail() throws Exception {
    String newEmail = UPDATED_EMAIL;

    mockMvc.perform(patch("/account/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "%s"}
                """.formatted(newEmail)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(newEmail))
        .andExpect(jsonPath("$.role").value(userToUpdate.getRole().name()))
        .andExpect(jsonPath("$.status").value(AccountStatus.ACTIVE.name()));

    Profile updatedProfile = profileRepository.findByUserId(userToUpdate.getId()).orElseThrow();
    Assertions.assertEquals(newEmail, updatedProfile.getCustomer().getEmail());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchAccount_withValidStatus_updatesStatus() throws Exception {
    mockMvc.perform(patch("/account/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status": "INACTIVE"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(AccountStatus.INACTIVE.name()))
        .andExpect(jsonPath("$.email").value(emailFor(userToUpdate)))
        .andExpect(jsonPath("$.role").value(userToUpdate.getRole().name()));

    User updatedUser = userRepository.findById(userToUpdate.getId()).orElseThrow();
    Assertions.assertEquals(AccountStatus.INACTIVE, updatedUser.getStatus());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchAccount_withValidEmail_andStripeCustomerId_propagatesToStripe() throws Exception {
    Customer customer = customerRepository.findByEmail(USER_EMAIL).orElseThrow();
    customer.setStripeCustomerId("cus_test_propagate");
    customerRepository.saveAndFlush(customer);

    mockMvc.perform(patch("/account/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "%s"}
                """.formatted(UPDATED_EMAIL)))
        .andExpect(status().isOk());

    verify(stripePaymentProvider, times(1))
        .updateCustomerEmail("cus_test_propagate", UPDATED_EMAIL);
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchAccount_withValidEmail_andNoStripeCustomerId_skipsStripeCall() throws Exception {
    mockMvc.perform(patch("/account/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "%s"}
                """.formatted(UPDATED_EMAIL)))
        .andExpect(status().isOk());

    verify(stripePaymentProvider, never()).updateCustomerEmail(any(), any());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchAccount_whenEmailUnchanged_doesNotCallStripe() throws Exception {
    Customer customer = customerRepository.findByEmail(USER_EMAIL).orElseThrow();
    customer.setStripeCustomerId("cus_unchanged");
    customerRepository.saveAndFlush(customer);

    mockMvc.perform(patch("/account/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "%s"}
                """.formatted(USER_EMAIL)))
        .andExpect(status().isOk());

    verify(stripePaymentProvider, never()).updateCustomerEmail(any(), any());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchAccount_normalizesNewEmailToLowercaseBeforePropagatingToStripe() throws Exception {
    Customer customer = customerRepository.findByEmail(USER_EMAIL).orElseThrow();
    customer.setStripeCustomerId("cus_test_normalize");
    customerRepository.saveAndFlush(customer);

    mockMvc.perform(patch("/account/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "MiXeD.CaSe@Example.COM"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("mixed.case@example.com"));

    verify(stripePaymentProvider, times(1))
        .updateCustomerEmail("cus_test_normalize", "mixed.case@example.com");
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchAccount_withDuplicateEmail_returnsConflict() throws Exception {
    createUserWithProfile(EXISTING_EMAIL, Role.USER);

    mockMvc.perform(patch("/account/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "%s"}
                """.formatted(EXISTING_EMAIL)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Conflict"))
        .andExpect(jsonPath("$.detail").value("Account with email '%s' already exists".formatted(EXISTING_EMAIL)));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchAccount_withInvalidRole_returnsBadRequest() throws Exception {
    mockMvc.perform(patch("/account/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"role": "INVALID"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.detail").value("Request body is malformed or contains invalid values."))
        .andExpect(jsonPath("$.instance").value("/account/" + userToUpdate.getId()));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchAccount_withInvalidStatus_returnsBadRequest() throws Exception {
    mockMvc.perform(patch("/account/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status": "WRONG"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.detail").value("Request body is malformed or contains invalid values."))
        .andExpect(jsonPath("$.instance").value("/account/" + userToUpdate.getId()));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchAccount_withInvalidEmail_returnsBadRequest() throws Exception {
    mockMvc.perform(patch("/account/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "not-an-email"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Request Validation Failed"))
        .andExpect(jsonPath("$.errors[0]").value("email: A valid email must be provided"));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchAccount_withoutUpdates_returnsBadRequest() throws Exception {
    mockMvc.perform(patch("/account/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Request Validation Failed"))
        .andExpect(jsonPath("$.errors[0]").value("updates: At least one field must be provided"));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchAccount_forMissingUser_returnsNotFound() throws Exception {
    UUID missingUserId = UUID.randomUUID();

    mockMvc.perform(patch("/account/{id}", missingUserId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"role": "ADMINISTRATOR"}
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Not Found"))
        .andExpect(jsonPath("$.detail").value("Account with id '%s' not found".formatted(missingUserId)));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchAccount_whenStripeEmailDirty_propagatesEvenIfEmailUnchanged() throws Exception {
    Customer customer = customerRepository.findByEmail(USER_EMAIL).orElseThrow();
    customer.setStripeCustomerId("cus_pending_sync");
    customer.setStripeEmailDirty(true);
    customerRepository.saveAndFlush(customer);

    mockMvc.perform(patch("/account/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"role": "ADMINISTRATOR"}
                """))
        .andExpect(status().isOk());

    verify(stripePaymentProvider, times(1)).updateCustomerEmail("cus_pending_sync", USER_EMAIL);
    Customer reloaded = customerRepository.findById(customer.getId()).orElseThrow();
    Assertions.assertFalse(reloaded.isStripeEmailDirty());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchAccount_whenStripeFails_keepsDirtyFlagForRetry() throws Exception {
    Customer customer = customerRepository.findByEmail(USER_EMAIL).orElseThrow();
    customer.setStripeCustomerId("cus_retry");
    customerRepository.saveAndFlush(customer);

    doThrow(new StripeApiException("simulated outage"))
        .when(stripePaymentProvider).updateCustomerEmail(any(), any());

    mockMvc.perform(patch("/account/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "%s"}
                """.formatted(UPDATED_EMAIL)))
        .andExpect(status().isBadGateway());

    Customer afterFail = customerRepository.findById(customer.getId()).orElseThrow();
    Assertions.assertEquals(UPDATED_EMAIL, afterFail.getEmail());
    Assertions.assertTrue(afterFail.isStripeEmailDirty());

    reset(stripePaymentProvider);

    mockMvc.perform(patch("/account/{id}", userToUpdate.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"role": "ADMINISTRATOR"}
                """))
        .andExpect(status().isOk());

    verify(stripePaymentProvider, times(1)).updateCustomerEmail("cus_retry", UPDATED_EMAIL);
    Customer afterHeal = customerRepository.findById(customer.getId()).orElseThrow();
    Assertions.assertFalse(afterHeal.isStripeEmailDirty());
  }

  private User createUserWithProfile(String email, Role role) {
    User user = new User();
    user.setPassword(Objects.requireNonNull(passwordEncoder.encode(PASSWORD)));
    user.setRole(role);
    user.setStatus(AccountStatus.ACTIVE);
    user = userRepository.saveAndFlush(user);

    Customer customer = new Customer();
    customer.setEmail(email);
    Profile profile = new Profile();
    profile.setCustomer(customer);
    profile.setUser(user);
    profile.setPoints(0);
    customer.setProfile(profile);
    customerRepository.saveAndFlush(customer);
    return user;
  }

  private String emailFor(User user) {
    return profileRepository.findByUserId(user.getId()).orElseThrow().getCustomer().getEmail();
  }
}
