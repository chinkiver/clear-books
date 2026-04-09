package com.accounting.service;

import com.accounting.dto.*;
import com.accounting.entity.User;
import com.accounting.exception.BusinessException;
import com.accounting.repository.UserRepository;
import com.accounting.security.JwtUtil;
import com.accounting.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtUtil.generateToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return buildTokenResponse(token, userDetails);
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .email(request.getEmail())
                .build();

        userRepository.save(user);

        // Auto login after registration
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtUtil.generateToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return buildTokenResponse(token, userDetails);
    }

    public TokenResponse refresh(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new BusinessException("Invalid token");
        }

        String token = bearerToken.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException("Invalid or expired token");
        }

        String username = jwtUtil.getUsernameFromToken(token);
        String newToken = jwtUtil.generateToken(username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found"));

        return buildTokenResponse(newToken, UserDetailsImpl.build(user));
    }

    private TokenResponse buildTokenResponse(String token, UserDetailsImpl userDetails) {
        return TokenResponse.builder()
                .token(token)
                .type("Bearer")
                .expiresIn(jwtUtil.getExpirationTime() / 1000)
                .user(TokenResponse.UserInfo.builder()
                        .id(userDetails.getId())
                        .username(userDetails.getUsername())
                        .nickname(userDetails.getNickname())
                        .email(userDetails.getEmail())
                        .build())
                .build();
    }
}
