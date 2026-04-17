package com.noserbulgaria.micromarket.security.auth.refresh;

import com.noserbulgaria.micromarket.security.user.User;

public record RotationResult(User user, String newRawToken) {
}
