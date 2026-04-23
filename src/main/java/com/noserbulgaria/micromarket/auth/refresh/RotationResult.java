package com.noserbulgaria.micromarket.auth.refresh;

import com.noserbulgaria.micromarket.auth.user.User;

public record RotationResult(User user, String newRawToken) {
}
