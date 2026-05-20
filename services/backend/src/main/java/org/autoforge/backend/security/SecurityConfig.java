package org.autoforge.backend.security;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Base64;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
      .csrf(AbstractHttpConfigurer::disable)
      .cors(Customizer.withDefaults())
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(authorize -> authorize
        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/api/v1/health").permitAll()
        .requestMatchers("/api/v1/prompt-drafts/**", "/api/v1/jobs/**").hasAnyRole("developer", "teacher", "learning_teacher")
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .anyRequest().authenticated())
      .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
      .build();
  }

  @Bean
  JwtDecoder jwtDecoder() throws Exception {
    return NimbusJwtDecoder.withPublicKey(readPublicKey()).build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(properties.allowedOrigins());
    configuration.setAllowedMethods(Lists.allowedMethods());
    configuration.setAllowedHeaders(Lists.allowedHeaders());
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setPrincipalClaimName("preferred_username");
    converter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtGrantedAuthoritiesConverter());
    return converter;
  }

  private RSAPublicKey readPublicKey() throws Exception {
    try (InputStream inputStream = new DefaultResourceLoader().getResource("classpath:keycloak-public.pem").getInputStream()) {
      String pem = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replaceAll("\\s", "");
      byte[] decoded = Base64.getDecoder().decode(pem);
      X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
      return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }
  }

  @Bean
  SecurityProperties securityProperties(org.springframework.core.env.Environment environment) {
    String rawOrigins = environment.getProperty("autoforge.security.cors.allowed-origins", "");
    return SecurityProperties.fromCsv(rawOrigins);
  }

  private static final class KeycloakJwtGrantedAuthoritiesConverter
    implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
      Set<GrantedAuthority> authorities = new LinkedHashSet<>();
      authorities.addAll(scopeAuthorities(jwt));
      authorities.addAll(groupAuthorities(jwt));
      authorities.addAll(realmRoleAuthorities(jwt));
      authorities.addAll(resourceRoleAuthorities(jwt));
      return authorities;
    }

    private Collection<GrantedAuthority> scopeAuthorities(Jwt jwt) {
      Object scopeClaim = jwt.getClaims().get("scope");
      if (!(scopeClaim instanceof String scopes)) {
        return Set.of();
      }

      return java.util.Arrays.stream(scopes.split(" "))
        .filter(scope -> !scope.isBlank())
        .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Collection<GrantedAuthority> realmRoleAuthorities(Jwt jwt) {
      Object realmAccess = jwt.getClaims().get("realm_access");
      if (!(realmAccess instanceof Map<?, ?> access)) {
        return Set.of();
      }

      Object roles = access.get("roles");
      if (!(roles instanceof Collection<?> realmRoles)) {
        return Set.of();
      }

      return realmRoles.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Collection<GrantedAuthority> groupAuthorities(Jwt jwt) {
      Object groupsClaim = jwt.getClaims().get("groups");
      if (!(groupsClaim instanceof Collection<?> groups)) {
        return Set.of();
      }

      return groups.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(SecurityConfig::normalizeGroupName)
        .filter(group -> !group.isBlank())
        .map(group -> new SimpleGrantedAuthority("ROLE_" + group))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Collection<GrantedAuthority> resourceRoleAuthorities(Jwt jwt) {
      Object resourceAccess = jwt.getClaims().get("resource_access");
      if (!(resourceAccess instanceof Map<?, ?> access)) {
        return Set.of();
      }

      Set<GrantedAuthority> authorities = new HashSet<>();
      for (Object entryValue : access.values()) {
        if (!(entryValue instanceof Map<?, ?> clientAccess)) {
          continue;
        }

        Object roles = clientAccess.get("roles");
        if (!(roles instanceof Collection<?> clientRoles)) {
          continue;
        }

        clientRoles.stream()
          .filter(String.class::isInstance)
          .map(String.class::cast)
          .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
          .forEach(authorities::add);
      }
      return authorities;
    }
  }

  private static String normalizeGroupName(String group) {
    if (group == null) {
      return "";
    }

    String normalized = group.trim();
    if (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }
    if (normalized.contains("/")) {
      normalized = normalized.substring(normalized.lastIndexOf('/') + 1);
    }
    return normalized;
  }
}
