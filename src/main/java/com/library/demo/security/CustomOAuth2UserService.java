package com.library.demo.security;

import com.library.demo.model.User;
import com.library.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // GitHub users can have their email address set to private, so it won't be returned in attributes.
        // If so, we manually fetch it from the GitHub user emails API.
        if ("github".equals(registrationId) && email == null) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(userRequest.getAccessToken().getTokenValue());
                HttpEntity<String> entity = new HttpEntity<>("", headers);
                
                ResponseEntity<List> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    entity,
                    List.class
                );
                
                List<Map<String, Object>> emails = response.getBody();
                if (emails != null && !emails.isEmpty()) {
                    for (Map<String, Object> emailObj : emails) {
                        if (Boolean.TRUE.equals(emailObj.get("primary"))) {
                            email = (String) emailObj.get("email");
                            break;
                        }
                    }
                    if (email == null) {
                        email = (String) emails.get(0).get("email");
                    }
                }
            } catch (Exception e) {
                // Fallback / log error
            }
        }

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        // Create mutable attributes map to update values if needed
        Map<String, Object> mutableAttributes = new HashMap<>(attributes);
        mutableAttributes.put("email", email);

        if (name == null || name.trim().isEmpty()) {
            if ("github".equals(registrationId)) {
                name = (String) attributes.get("login");
            }
            if (name == null || name.trim().isEmpty()) {
                name = email.split("@")[0];
            }
            mutableAttributes.put("name", name);
        }

        // Check if user exists in the database
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Register a new user
            user = new User();
            user.setEmail(email);
            // Fallback to email username if display name is null or blank
            user.setName(name);
            // Generate a secure random password for OAuth2 users to satisfy validations
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setRole(User.Role.USER); // Default role
            user = userRepository.save(user);
        }

        // Map authorities based on the database user role
        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        // We return DefaultOAuth2User with "email" as the nameAttributeKey
        // so authentication.getName() correctly returns the email address
        return new DefaultOAuth2User(authorities, mutableAttributes, "email");
    }
}

