package com.realdev.readle.global.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class SecurityPropertiesTest {

  @Test
  void rejectsMissingOrShortSecurityKeys() {
    assertThatThrownBy(() -> properties("", validStateKey()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> properties(validJwtSecret(), "short"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNonContractTokenAndStateTtls() {
    assertThatThrownBy(() -> properties(validJwtSecret(), validStateKey(), 29, 14, 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> properties(validJwtSecret(), validStateKey(), 30, 13, 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> properties(validJwtSecret(), validStateKey(), 30, 14, 9))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private SecurityProperties properties(String jwtSecret, String stateEncryptionKey) {
    return properties(jwtSecret, stateEncryptionKey, 30, 14, 10);
  }

  private SecurityProperties properties(
      String jwtSecret,
      String stateEncryptionKey,
      int accessTokenMinutes,
      int refreshTokenDays,
      int stateMinutes) {
    return new SecurityProperties(
        "issuer",
        jwtSecret,
        "audience",
        accessTokenMinutes,
        refreshTokenDays,
        stateEncryptionKey,
        stateMinutes,
        "http://localhost:8080",
        List.of("/"),
        new SecurityProperties.OAuthProviders(
            new SecurityProperties.OAuthProviderSettings(),
            new SecurityProperties.OAuthProviderSettings()));
  }

  private String validJwtSecret() {
    return "01234567890123456789012345678901";
  }

  private String validStateKey() {
    return "abcdefghijklmnopqrstuvwxyz123456";
  }
}
