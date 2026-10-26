package com.example.inventory_api_springboot.service;

import com.example.inventory_api_springboot.dto.request.LoginRequestDTO;
import com.example.inventory_api_springboot.dto.request.RegisterAdminRequestDTO;
import com.example.inventory_api_springboot.dto.request.RegisterCustomerRequestDTO;
import com.example.inventory_api_springboot.entity.User;
import com.example.inventory_api_springboot.entity.UserBalance;
import com.example.inventory_api_springboot.entity.UserDetail;
import com.example.inventory_api_springboot.entity.UserSession;
import com.example.inventory_api_springboot.enums.UserRoleEnum;
import com.example.inventory_api_springboot.repository.UserRepository;
import com.example.inventory_api_springboot.repository.UserSessionRepository;
import com.example.inventory_api_springboot.util.NetworkUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserAgentAnalyzer userAgentAnalyzer;
    private final AuthenticationManager authenticationManager;

    public ResponseEntity<String> register(RegisterCustomerRequestDTO registerRequestData) {
        // Hash password using Argon2
        String hashedPassword = passwordEncoder.encode(registerRequestData.password());

        User user = new User().builder()
            .email(registerRequestData.email())
            .password(hashedPassword)
            .role(UserRoleEnum.CUSTOMER)
            .build();

        UserDetail userDetail = new UserDetail().builder()
            .firstName(registerRequestData.firstName())
            .lastName(registerRequestData.lastName())
            .phoneNumber(registerRequestData.phoneNumber())
            .address(registerRequestData.address())
            .build();

        UserBalance userBalance = new UserBalance().builder()
            .user(user)
            .balance(BigDecimal.ZERO)
            .reservedBalance(BigDecimal.ZERO)
            .build();

        user.setUserDetail(userDetail);
        user.setUserBalance(userBalance);
        userDetail.setUser(user);

        userRepository.save(user);
        return ResponseEntity.ok("Register success");
    }

    public ResponseEntity<String> registerAdmin(RegisterAdminRequestDTO registerAdminRequestData) {
        User user = new User().builder()
            .email(registerAdminRequestData.email())
            .password(passwordEncoder.encode(registerAdminRequestData.password()))
            .role(registerAdminRequestData.role())
            .build();

        userRepository.save(user);
        return ResponseEntity.ok("Admin registered successfully");
    }

    public ResponseEntity login(HttpServletRequest request, LoginRequestDTO loginRequestData) {
        Authentication authentication =
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequestData.email(),
                            loginRequestData.password()
                    )
            );

        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.fromString(authentication.getName());

        String accessToken = jwtService.generateToken(userId, sessionId, Duration.ofMinutes(15));
        String refreshToken = jwtService.generateToken(userId, sessionId, Duration.ofDays(30));

        // Extract UserAgent Header
        UserAgent userAgent = userAgentAnalyzer.parse(request.getHeader("User-Agent"));
        // getReferenceById doesn't fetch from DB
        User user = userRepository.getReferenceById(userId);

        UserSession userSessions = new UserSession().builder()
                .user(user)
                .clientSessionId(sessionId)
                .clientName(userAgent.getValue(UserAgent.AGENT_NAME))
                .osName(userAgent.getValue(UserAgent.OPERATING_SYSTEM_NAME))
                .ipAddress(NetworkUtil.getIp(request))
                .token(refreshToken).build();

        userSessionRepository.save(userSessions);

        // Set Response Secure Cookie
        ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", accessToken)
                .secure(false)
                .httpOnly(true)
//                .path("/")
                .maxAge(Duration.ofMinutes(15))
                .sameSite("lax")
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", refreshToken)
                .secure(false)
                .httpOnly(true)
//                .path("/")
                .maxAge(Duration.ofDays(30))
                .sameSite("lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body("Login Successfull");
    }

    public ResponseEntity<String> refresh(UUID userId, UUID sessionId) throws BadRequestException {
        UserSession userSession = userSessionRepository.findByClientSessionIdAndDeletedAtIsNull(sessionId)
                .orElseThrow(() -> new BadRequestException("No session found"));

        String accessToken = jwtService.generateToken(userId, sessionId, Duration.ofMinutes(15));
        // Set Response Secure Cookie
        ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", accessToken)
                .secure(false)
                .httpOnly(true)
//                .path("/")
                .maxAge(Duration.ofMinutes(15))
                .sameSite("lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .body("User session refreshed successfully");
    }

    public ResponseEntity<String> logout(UUID userId, UUID sessionId) throws BadRequestException {
        UserSession userSessions = userSessionRepository.findByClientSessionIdAndDeletedAtIsNull(sessionId)
                .orElseThrow(() -> new BadRequestException("No session found"));

        userSessionRepository.delete(userSessions);
        return ResponseEntity.ok("User logged out successfully");
    }

    public ResponseEntity<String> logoutAll(UUID userId) throws BadRequestException {
        userSessionRepository.logoutAll(userId, LocalDateTime.now());
        return ResponseEntity.ok("User logged out successfully");
    }

    public boolean roleCheck(UUID userId, UserRoleEnum requiredUserRoles) {
        return userRepository.existsByUserIdAndRolesNative(userId, requiredUserRoles);
    }
}
