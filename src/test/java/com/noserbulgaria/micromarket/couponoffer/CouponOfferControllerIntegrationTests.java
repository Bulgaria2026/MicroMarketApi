package com.noserbulgaria.micromarket.couponoffer;

import com.noserbulgaria.micromarket.auth.user.AccountStatus;
import com.noserbulgaria.micromarket.auth.user.Role;
import com.noserbulgaria.micromarket.auth.user.User;
import com.noserbulgaria.micromarket.auth.user.UserRepository;
import com.noserbulgaria.micromarket.coupon.Coupon;
import com.noserbulgaria.micromarket.coupon.CouponRepository;
import com.noserbulgaria.micromarket.customer.PointChangeReason;
import com.noserbulgaria.micromarket.customer.Profile;
import com.noserbulgaria.micromarket.customer.ProfileRepository;
import com.noserbulgaria.micromarket.payment.stripe.StripeManagedCoupon;
import com.noserbulgaria.micromarket.payment.stripe.StripeManagedCouponRequest;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import jakarta.persistence.EntityManager;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CouponOfferControllerIntegrationTests {

  private static final String ADMIN_EMAIL = "admin@micromarket.dev";
  private static final String USER_EMAIL = "user@micromarket.dev";
  private static final String PASSWORD = "user12345";

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private CouponOfferRepository couponOfferRepository;
  @Autowired
  private CouponRepository couponRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private ProfileRepository profileRepository;
  @Autowired
  private PasswordEncoder passwordEncoder;
  @Autowired
  private EntityManager entityManager;

  @MockitoBean
  private StripePaymentProvider stripePaymentProvider;

  private final AtomicInteger stripeCounter = new AtomicInteger();

  @BeforeEach
  void setUp() {
    couponRepository.deleteAll();
    couponOfferRepository.deleteAll();
    ensureUserExists(ADMIN_EMAIL, Role.ADMINISTRATOR);
    User user = ensureUserExists(USER_EMAIL, Role.USER);
    ensureProfile(user, 120, null);

    when(stripePaymentProvider.createManagedCoupon(any()))
        .thenAnswer(invocation -> {
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
    when(stripePaymentProvider.createCustomer(USER_EMAIL)).thenReturn("cus_generated");
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void createCouponOffer_returnsCreatedOffer() throws Exception {
    mockMvc.perform(post("/coupon-offer")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Coffee discount",
                  "description": "Spend points for coffee",
                  "pointCost": 50,
                  "amountOff": 7.50,
                  "maxPurchases": 20,
                  "active": true
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Coffee discount"))
        .andExpect(jsonPath("$.description").value("Spend points for coffee"))
        .andExpect(jsonPath("$.pointCost").value(50))
        .andExpect(jsonPath("$.amountOff").value(7.5))
        .andExpect(jsonPath("$.maxPurchases").value(20))
        .andExpect(jsonPath("$.purchaseCount").value(0))
        .andExpect(jsonPath("$.active").value(true));

    assertThat(couponOfferRepository.findAll()).hasSize(1);
  }

  @Test
  void catalog_returnsOnlyCurrentlyPurchasableOffers() throws Exception {
    couponOfferRepository.saveAndFlush(CouponOffer.builder()
        .name("Visible")
        .description("visible")
        .pointCost(10)
        .amountOff(new BigDecimal("5.00"))
        .active(true)
        .build());
    couponOfferRepository.saveAndFlush(CouponOffer.builder()
        .name("Inactive")
        .description("inactive")
        .pointCost(10)
        .amountOff(new BigDecimal("5.00"))
        .active(false)
        .build());

    mockMvc.perform(get("/coupon-offer/catalog"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Visible"));
  }

  @Test
  void purchase_withoutAuthentication_returnsUnauthorized() throws Exception {
    CouponOffer offer = couponOfferRepository.saveAndFlush(CouponOffer.builder()
        .name("Visible")
        .pointCost(10)
        .amountOff(new BigDecimal("5.00"))
        .active(true)
        .build());

    mockMvc.perform(post("/coupon-offer/{id}/purchase", offer.getId()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithUserDetails(value = USER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void purchase_deductsPointsAndIssuesSingleUseCoupon() throws Exception {
    User user = userRepository.findByEmail(USER_EMAIL).orElseThrow();
    Profile profile = profileRepository.findByUserId(user.getId()).orElseThrow();
    profile.setStripeCustomerId(null);
    profileRepository.saveAndFlush(profile);

    CouponOffer offer = couponOfferRepository.saveAndFlush(CouponOffer.builder()
        .name("Issued offer")
        .description("offer")
        .pointCost(40)
        .amountOff(new BigDecimal("6.00"))
        .active(true)
        .maxPurchases(3)
        .build());

    mockMvc.perform(post("/coupon-offer/{id}/purchase", offer.getId()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.couponOfferId").value(offer.getId().toString()))
        .andExpect(jsonPath("$.userId").value(user.getId().toString()))
        .andExpect(jsonPath("$.amountOff").value(6.0))
        .andExpect(jsonPath("$.maxRedemptions").value(1))
        .andExpect(jsonPath("$.active").value(true));

    entityManager.clear();
    Profile updatedProfile = profileRepository.findByUserId(user.getId()).orElseThrow();
    assertThat(updatedProfile.getPoints()).isEqualTo(80);
    assertThat(updatedProfile.getLastChangeReason()).isEqualTo(PointChangeReason.COUPON_PURCHASED);
    assertThat(updatedProfile.getStripeCustomerId()).isEqualTo("cus_generated");

    Coupon issuedCoupon = couponRepository.findAll().getFirst();
    assertThat(issuedCoupon.getCouponOffer()).isNotNull();
    assertThat(issuedCoupon.getCouponOffer().getId()).isEqualTo(offer.getId());
    assertThat(issuedCoupon.getMaxRedemptions()).isEqualTo(1);
    assertThat(issuedCoupon.getStripeCouponId()).startsWith("coupon_stripe_");

    CouponOffer updatedOffer = couponOfferRepository.findById(offer.getId()).orElseThrow();
    assertThat(updatedOffer.getPurchaseCount()).isEqualTo(1);

    ArgumentCaptor<StripeManagedCouponRequest> captor = ArgumentCaptor.forClass(StripeManagedCouponRequest.class);
    verify(stripePaymentProvider).createManagedCoupon(captor.capture());
    assertThat(captor.getValue().amountOff()).isEqualTo(600L);
    assertThat(captor.getValue().name()).isEqualTo("Issued offer");
    assertThat(captor.getValue().maxRedemptions()).isEqualTo(1);
    assertThat(captor.getValue().stripeCustomerId()).isEqualTo("cus_generated");
  }

  @Test
  @WithUserDetails(value = USER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void purchase_sameOfferTwice_createsDistinctStripeCouponsAndLocalCoupons() throws Exception {
    User user = userRepository.findByEmail(USER_EMAIL).orElseThrow();
    CouponOffer offer = couponOfferRepository.saveAndFlush(CouponOffer.builder()
        .name("Repeatable")
        .pointCost(10)
        .amountOff(new BigDecimal("3.00"))
        .active(true)
        .maxPurchases(5)
        .build());

    mockMvc.perform(post("/coupon-offer/{id}/purchase", offer.getId()))
        .andExpect(status().isCreated());
    mockMvc.perform(post("/coupon-offer/{id}/purchase", offer.getId()))
        .andExpect(status().isCreated());

    entityManager.clear();
    var coupons = couponRepository.findAll();
    assertThat(coupons).hasSize(2);
    assertThat(coupons)
        .allSatisfy(coupon -> {
          assertThat(coupon.getUser()).isNotNull();
          assertThat(coupon.getUser().getId()).isEqualTo(user.getId());
          assertThat(coupon.getStripeCouponId()).startsWith("coupon_stripe_");
        });
    assertThat(coupons.get(0).getStripeCouponId()).isNotEqualTo(coupons.get(1).getStripeCouponId());
    assertThat(coupons.get(0).getStripePromotionCodeId()).isNotEqualTo(coupons.get(1).getStripePromotionCodeId());
  }

  @Test
  @WithUserDetails(value = USER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void purchase_withInsufficientPoints_returnsBadRequest() throws Exception {
    User user = userRepository.findByEmail(USER_EMAIL).orElseThrow();
    Profile profile = profileRepository.findByUserId(user.getId()).orElseThrow();
    profile.setPoints(5);
    profileRepository.saveAndFlush(profile);

    CouponOffer offer = couponOfferRepository.saveAndFlush(CouponOffer.builder()
        .name("Too expensive")
        .pointCost(20)
        .amountOff(new BigDecimal("6.00"))
        .active(true)
        .build());

    mockMvc.perform(post("/coupon-offer/{id}/purchase", offer.getId()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Insufficient points to purchase coupon offer"));

    assertThat(couponRepository.findAll()).isEmpty();
  }

  @Test
  @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
  void updateCouponOffer_whenCapReachedCanBeDeactivated() throws Exception {
    CouponOffer offer = couponOfferRepository.saveAndFlush(CouponOffer.builder()
        .name("Editable")
        .description("desc")
        .pointCost(10)
        .amountOff(new BigDecimal("4.00"))
        .active(true)
        .maxPurchases(2)
        .purchaseCount(2)
        .build());

    mockMvc.perform(put("/coupon-offer/{id}", offer.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Editable",
                  "description": "updated",
                  "pointCost": 10,
                  "amountOff": 4.00,
                  "maxPurchases": 2,
                  "active": false
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("updated"))
        .andExpect(jsonPath("$.active").value(false));
  }

  private User ensureUserExists(String email, Role role) {
    return userRepository.findByEmail(email)
        .orElseGet(() -> {
          User user = new User();
          user.setEmail(email);
          user.setPassword(Objects.requireNonNull(passwordEncoder.encode(PASSWORD)));
          user.setRole(role);
          user.setStatus(AccountStatus.ACTIVE);
          return userRepository.saveAndFlush(user);
        });
  }

  private void ensureProfile(User user, int points, String stripeCustomerId) {
    Profile profile = profileRepository.findByUserId(user.getId())
        .orElseGet(() -> {
          Profile created = new Profile();
          created.setUser(user);
          return created;
        });
    profile.setPoints(points);
    profile.setStripeCustomerId(stripeCustomerId);
    profileRepository.saveAndFlush(profile);
  }
}
