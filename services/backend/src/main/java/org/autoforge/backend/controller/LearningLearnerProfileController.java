package org.autoforge.backend.controller;

import org.autoforge.backend.dto.LearningLearnerProfileRequest;
import org.autoforge.backend.dto.LearningLearnerProfileResponse;
import org.autoforge.backend.service.LearningLearnerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/learning/learner-profile")
public class LearningLearnerProfileController {

  private final LearningLearnerProfileService learningLearnerProfileService;

  @GetMapping
  public LearningLearnerProfileResponse getProfile(Authentication authentication) {
    return learningLearnerProfileService.getProfile(currentSubject(authentication));
  }

  @PutMapping
  public LearningLearnerProfileResponse updateProfile(@RequestBody LearningLearnerProfileRequest request, Authentication authentication) {
    return learningLearnerProfileService.updateProfile(currentSubject(authentication), request);
  }

  private String currentSubject(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken token)) {
      throw new IllegalStateException("Expected JWT authentication");
    }

    Jwt jwt = token.getToken();
    return jwt.getSubject();
  }
}
