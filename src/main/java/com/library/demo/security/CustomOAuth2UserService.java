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

import java.util.List;
import java.util.Map;
import java.util.UUID;

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

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        // Check if user exists in the database
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Register a new user
            user = new User();
            user.setEmail(email);
            // Fallback to email username if display name is null or blank
            user.setName(name != null && !name.trim().isEmpty() ? name : email.split("@")[0]);
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
        return new DefaultOAuth2User(authorities, attributes, "email");
    }
}
