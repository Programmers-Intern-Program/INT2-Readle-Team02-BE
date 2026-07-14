package com.realdev.readle.global.security;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
public record SecurityProperties(
    String jwtIssuer,
    String jwtSecret,
    String jwtAudience,
    int accessTokenMinutes,
    int refreshTokenDays,
    String stateEncryptionKey,
    int stateMinutes,
    String backendOrigin,
    List<String> allowedReturnPaths,
    OAuthProviders oauth) {

  public SecurityProperties {
    requireAtLeast(jwtSecret, 32, "JWT secret");
    requireAesKey(stateEncryptionKey);
    requireExactly(accessTokenMinutes, 30, "Access-token TTL");
    requireExactly(refreshTokenDays, 14, "Refresh-token TTL");
    requireExactly(stateMinutes, 10, "OAuth state TTL");
  }

  private static void requireAtLeast(String value, int minimumBytes, String name) {
    if (value == null || value.getBytes(StandardCharsets.UTF_8).length < minimumBytes) {
      throw new IllegalArgumentException(name + " must be at least " + minimumBytes + " bytes");
    }
  }

  private static void requireAesKey(String value) {
    int length = value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
    if (length != 16 && length != 24 && length != 32) {
      throw new IllegalArgumentException(
          "OAuth state encryption key must be a 16, 24, or 32 byte AES key");
    }
  }

  private static void requireExactly(int value, int expected, String name) {
    if (value != expected) {
      throw new IllegalArgumentException(name + " must be " + expected);
    }
  }

  public record OAuthProviders(OAuthProviderSettings google, OAuthProviderSettings kakao) {}

  public record OAuthProviderSettings(
      String clientId,
      String clientSecret,
      String authorizationUrl,
      String tokenUrl,
      String userInfoUrl) {
    public OAuthProviderSettings() {
      this("", "", "", "", "");
    }
  }
}
