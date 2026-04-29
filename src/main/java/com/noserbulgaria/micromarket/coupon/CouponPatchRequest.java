package com.noserbulgaria.micromarket.coupon;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class CouponPatchRequest {

  @Nullable
  @Size(max = 40, message = "Name must be at most 40 characters")
  private String name;

  private @Nullable Boolean active;

  private final Set<String> unsupportedFields = new LinkedHashSet<>();

  public @Nullable String getName() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  public @Nullable Boolean getActive() {
    return active;
  }

  public void setActive(@Nullable Boolean active) {
    this.active = active;
  }

  @JsonAnySetter
  public void setUnsupportedField(String name, Object value) {
    unsupportedFields.add(name);
  }

  Set<String> unsupportedFields() {
    return Collections.unmodifiableSet(unsupportedFields);
  }

  boolean isEmpty() {
    return name == null && active == null && unsupportedFields.isEmpty();
  }
}
