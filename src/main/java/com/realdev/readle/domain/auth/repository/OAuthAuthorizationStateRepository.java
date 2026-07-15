package com.realdev.readle.domain.auth.repository;

import com.realdev.readle.domain.auth.entity.OAuthAuthorizationState;
import com.realdev.readle.domain.member.entity.OAuthProvider;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

public interface OAuthAuthorizationStateRepository
    extends JpaRepository<OAuthAuthorizationState, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<OAuthAuthorizationState> findByStateHashAndOauthProvider(
      @Param("stateHash") String stateHash, @Param("oauthProvider") OAuthProvider oauthProvider);
}
