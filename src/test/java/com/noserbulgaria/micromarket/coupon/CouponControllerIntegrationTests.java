package com.noserbulgaria.micromarket.coupon;

import com.noserbulgaria.micromarket.auth.user.AccountStatus;
import com.noserbulgaria.micromarket.auth.user.Role;
import com.noserbulgaria.micromarket.auth.user.User;
import com.noserbulgaria.micromarket.auth.user.UserRepository;
import com.noserbulgaria.micromarket.couponoffer.CouponOffer;
import com.noserbulgaria.micromarket.couponoffer.CouponOfferRepository;
import com.noserbulgaria.micromarket.customer.Profile;
import com.noserbulgaria.micromarket.customer.ProfileRepository;
import com.noserbulgaria.micromarket.payment.stripe.StripeManagedCoupon;
import com.noserbulgaria.micromarket.payment.stripe.StripeManagedCouponRequest;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CouponControllerIntegrationTests {

  private static final String ADMIN_EMAIL = "admin@micromarket.dev";
  private static final String USER_EMAIL = "user@micromarket.dev";
  private static final String PASSWORD = "user12345";

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private CouponRepository couponRepository;
  @Autowired
  private CouponOfferRepository couponOfferRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private ProfileRepository profileRepository;
  @Autowired
  private PasswordEncoder passwordEncoder;

  @MockitoBean
  private StripePaymentProvider stripePaymentProvider;

  private final AtomicInteger stripeCounter = new AtomicInteger();

  @BeforeEach
  void setUp() {
    couponRepository.deleteAll();
    couponOfferRepository.deleteAll();
    ensureUserExists(ADMIN_EMAIL, Role.ADMINISTRATOR);
    ensureUserExists(USER_EMAIL, Role.USER);

    when(stripePaymentProvider.createManagedCoupon(any())).thenAnswer(invocation -> {
      StripeManagedCouponRequest request = invocation.getArgument(0);
      int sequence = stripeCounter.incrementAndGet();
      return new StripeManagedCoupon(
          "coupon_stripe_" + sequence,
          "promo_stripe_" + sequence,
          request.code(),
          0,
          request.active()
      );
    });
    when(stripePaymentProvider.retrievePromotionCode(any())).thenAnswer(invocation -> {
      String stripePromotionCodeId = invocation.getArgument(0);
      Coupon coupon = couponRepository.findByStripePromotionCodeId(stripePromotionCodeId)
          .orElseThrow();
      return new StripeManagedCoupon(
          coupon.getStripeCouponId(),
          coupon.getStripePromotionCodeId(),
          coupon.getCode(),
          coupon.getTimesRedeemed(),
          coupon.isActive()
      );
    });
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void createCoupon_withCustomCode_returnsCreatedCoupon() throws Exception {
    mockMvc.perform(post("/coupon")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "welcome-5",
                  "name": "Welcome coupon",
                  "pointCost": 10,
                  "amountOff": 5.00,
                  "maxRedemptions": 10,
                  "active": true
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("WELCOME-5"))
        .andExpect(jsonPath("$.name").value("Welcome coupon"))
        .andExpect(jsonPath("$.pointCost").value(10))
        .andExpect(jsonPath("$.amountOff").value(5.0))
        .andExpect(jsonPath("$.maxRedemptions").value(10))
        .andExpect(jsonPath("$.timesRedeemed").value(0))
        .andExpect(jsonPath("$.active").value(true));

    Coupon coupon = couponRepository.findAll().getFirst();
    assertThat(coupon.getCode()).isEqualTo("WELCOME-5");
    assertThat(coupon.getStripeCouponId()).startsWith("coupon_stripe_");
    assertThat(coupon.getStripePromotionCodeId()).startsWith("promo_stripe_");
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void createCoupon_withoutCode_generatesAdminFriendlyCode() throws Exception {
    mockMvc.perform(post("/coupon")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "pointCost": 0,
                  "amountOff": 7.50
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesRegex("^MM-[A-Z0-9]{8}$")))
        .andExpect(jsonPath("$.amountOff").value(7.5));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void createCoupon_forUserWithoutProfile_createsStripeCustomerEagerly() throws Exception {
    User user = createUser("coupon-target@micromarket.dev", false, null);
    when(stripePaymentProvider.createCustomer("coupon-target@micromarket.dev")).thenReturn("cus_generated");

    mockMvc.perform(post("/coupon")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userId": "%s",
                  "code": "USER-ONLY",
                  "pointCost": 0,
                  "amountOff": 4.00
                }
                """.formatted(user.getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(user.getId().toString()))
        .andExpect(jsonPath("$.code").value("USER-ONLY"));

    Profile profile = profileRepository.findByUserId(user.getId()).orElseThrow();
    assertThat(profile.getStripeCustomerId()).isEqualTo("cus_generated");

    ArgumentCaptor<StripeManagedCouponRequest> captor = ArgumentCaptor.forClass(StripeManagedCouponRequest.class);
    verify(stripePaymentProvider).createManagedCoupon(captor.capture());
    assertThat(captor.getValue().stripeCustomerId()).isEqualTo("cus_generated");
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getCouponById_returnsCoupon() throws Exception {
    Coupon coupon = couponRepository.saveAndFlush(coupon("READ-ONE", null, "coupon_old", "promo_old"));

    mockMvc.perform(get("/coupon/{id}", coupon.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(coupon.getId().toString()))
        .andExpect(jsonPath("$.code").value("READ-ONE"))
        .andExpect(jsonPath("$.amountOff").value(5.0));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void listCoupons_withUserIdFilter_returnsMatchingCoupons() throws Exception {
    User user = createUser("coupon-filter@micromarket.dev", true, "cus_filter");
    couponRepository.saveAndFlush(coupon("FILTER-ME", user, "coupon_a", "promo_a"));
    couponRepository.saveAndFlush(coupon("OTHER", null, "coupon_b", "promo_b"));

    mockMvc.perform(get("/coupon").param("userId", user.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].code").value("FILTER-ME"))
        .andExpect(jsonPath("$.page.totalElements").value(1));
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchCoupon_nameOnly_updatesLocalNameAndDoesNotTouchStripe() throws Exception {
    Coupon coupon = couponRepository.saveAndFlush(coupon("LOCAL-ONLY", null, "coupon_old", "promo_old"));

    mockMvc.perform(patch("/coupon/{id}", coupon.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Renamed locally"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Renamed locally"))
        .andExpect(jsonPath("$.code").value("LOCAL-ONLY"));

    Coupon updated = couponRepository.findById(coupon.getId()).orElseThrow();
    assertThat(updated.getName()).isEqualTo("Renamed locally");
    assertThat(updated.getStripeCouponId()).isEqualTo("coupon_old");
    assertThat(updated.getStripePromotionCodeId()).isEqualTo("promo_old");
    verify(stripePaymentProvider, never()).updateCouponName(any(), any());
    verify(stripePaymentProvider, never()).createManagedCoupon(any());
    verify(stripePaymentProvider, never()).deleteCoupon(any());
    verify(stripePaymentProvider, never()).updatePromotionCodeActive(any(), anyBoolean());
    verify(stripePaymentProvider, never()).retrievePromotionCode(any());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchCoupon_activeFalse_deletesStripeCouponAndMarksLocalInactive() throws Exception {
    Coupon coupon = couponRepository.saveAndFlush(coupon("RETIRE-ME", null, "coupon_old", "promo_old"));

    mockMvc.perform(patch("/coupon/{id}", coupon.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "active": false
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));

    Coupon updated = couponRepository.findById(coupon.getId()).orElseThrow();
    assertThat(updated.isActive()).isFalse();
    assertThat(updated.getTimesRedeemed()).isZero();
    verify(stripePaymentProvider).deleteCoupon("coupon_old");
    verify(stripePaymentProvider, never()).updatePromotionCodeActive(any(), anyBoolean());
    verify(stripePaymentProvider, never()).createManagedCoupon(any());
    verify(stripePaymentProvider, never()).retrievePromotionCode(any());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchCoupon_activeTrueForInactiveCoupon_returnsBadRequest() throws Exception {
    Coupon coupon = coupon("REACTIVATE-ME", null, "coupon_old", "promo_old");
    coupon.setActive(false);
    couponRepository.saveAndFlush(coupon);

    mockMvc.perform(patch("/coupon/{id}", coupon.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "active": true
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Inactive coupons cannot be reactivated"));

    Coupon updated = couponRepository.findById(coupon.getId()).orElseThrow();
    assertThat(updated.isActive()).isFalse();
    verify(stripePaymentProvider, never()).deleteCoupon(any());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchCoupon_withImmutableField_returnsBadRequest() throws Exception {
    Coupon coupon = couponRepository.saveAndFlush(coupon("IMMUTABLE", null, "coupon_old", "promo_old"));

    mockMvc.perform(patch("/coupon/{id}", coupon.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "amountOff": 8.00
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Unsupported coupon patch field(s): amountOff"));

    Coupon updated = couponRepository.findById(coupon.getId()).orElseThrow();
    assertThat(updated.getAmountOff()).isEqualByComparingTo("5.00");
    verify(stripePaymentProvider, never()).deleteCoupon(any());
    verify(stripePaymentProvider, never()).createManagedCoupon(any());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchCoupon_emptyBody_returnsBadRequest() throws Exception {
    Coupon coupon = couponRepository.saveAndFlush(coupon("EMPTY", null, "coupon_old", "promo_old"));

    mockMvc.perform(patch("/coupon/{id}", coupon.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Patch must include name or active"));

    verify(stripePaymentProvider, never()).deleteCoupon(any());
    verify(stripePaymentProvider, never()).createManagedCoupon(any());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void patchCoupon_purchasedCoupon_returnsBadRequest() throws Exception {
    CouponOffer offer = couponOfferRepository.saveAndFlush(CouponOffer.builder()
        .name("Offer")
        .pointCost(10)
        .amountOff(new BigDecimal("5.00"))
        .active(true)
        .build());
    Coupon coupon = coupon("PURCHASED", null, "coupon_old", "promo_old");
    coupon.setCouponOffer(offer);
    couponRepository.saveAndFlush(coupon);

    mockMvc.perform(patch("/coupon/{id}", coupon.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Nope"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Purchased coupons cannot be updated manually"));

    assertThat(couponRepository.findById(coupon.getId()).orElseThrow().getName()).isEqualTo("Local only");
    verify(stripePaymentProvider, never()).deleteCoupon(any());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void createCoupon_withDuplicateCode_returnsConflict() throws Exception {
    couponRepository.saveAndFlush(coupon("DUPLICATE", null, "coupon_old", "promo_old"));

    mockMvc.perform(post("/coupon")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "duplicate",
                  "pointCost": 0,
                  "amountOff": 5.00
                }
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Coupon with code 'DUPLICATE' already exists"));

    verify(stripePaymentProvider, never()).createManagedCoupon(any());
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void createCoupon_forUnknownUser_returnsNotFound() throws Exception {
    mockMvc.perform(post("/coupon")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userId": "%s",
                  "code": "UNKNOWN-USER",
                  "pointCost": 0,
                  "amountOff": 5.00
                }
                """.formatted(UUID.randomUUID())))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails(value = USER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void createCoupon_asNonAdmin_returnsForbidden() throws Exception {
    mockMvc.perform(post("/coupon")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "NOPE",
                  "pointCost": 0,
                  "amountOff": 5.00
                }
                """))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails(value = USER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void getOwnCoupons_returnsOnlyCurrentUsersCoupons() throws Exception {
    User user = userRepository.findByEmail(USER_EMAIL).orElseThrow();
    User otherUser = createUser("other-user@micromarket.dev", true, "cus_other");
    couponRepository.saveAndFlush(coupon("MINE", user, "coupon_mine", "promo_mine"));
    couponRepository.saveAndFlush(coupon("NOT-MINE", otherUser, "coupon_other", "promo_other"));

    mockMvc.perform(get("/coupon/own"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].code").value("MINE"))
        .andExpect(jsonPath("$.content[0].userId").value(user.getId().toString()));
  }

  private Coupon coupon(String code, User user, String stripeCouponId, String stripePromotionCodeId) {
    return Coupon.builder()
        .user(user)
        .code(code)
        .name("Local only")
        .pointCost(10)
        .amountOff(new BigDecimal("5.00"))
        .maxRedemptions(3)
        .timesRedeemed(0)
        .active(true)
        .stripeCouponId(stripeCouponId)
        .stripePromotionCodeId(stripePromotionCodeId)
        .build();
  }

  private User createUser(String email, boolean withProfile, String stripeCustomerId) {
    User user = userRepository.findByEmail(email)
        .orElseGet(() -> {
          User created = new User();
          created.setEmail(email);
          created.setPassword(Objects.requireNonNull(passwordEncoder.encode(PASSWORD)));
          created.setRole(Role.USER);
          created.setStatus(AccountStatus.ACTIVE);
          return userRepository.saveAndFlush(created);
        });

    if (withProfile) {
      Profile profile = profileRepository.findByUserId(user.getId())
          .orElseGet(() -> {
            Profile created = new Profile();
            created.setUser(user);
            created.setPoints(0);
            return profileRepository.saveAndFlush(created);
          });
      profile.setStripeCustomerId(stripeCustomerId);
      profileRepository.saveAndFlush(profile);
    }
    return user;
  }

  private void ensureUserExists(String email, Role role) {
    if (userRepository.findByEmail(email).isPresent()) {
      return;
    }
    User user = new User();
    user.setEmail(email);
    user.setPassword(Objects.requireNonNull(passwordEncoder.encode(PASSWORD)));
    user.setRole(role);
    user.setStatus(AccountStatus.ACTIVE);
    userRepository.saveAndFlush(user);
  }
}
