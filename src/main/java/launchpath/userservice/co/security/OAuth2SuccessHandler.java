package launchpath.userservice.co.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import launchpath.userservice.co.entities.User;
import launchpath.userservice.co.enums.AuthProvider;
import launchpath.userservice.co.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private UserService userService;
    private final JwtUtil jwtUtil;

    @Value("${app.oauth2.redirect-url:http://localhost:8080/auth/callback}")
    private String frontendCallbackUrl;

    @Autowired
    public OAuth2SuccessHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Autowired
    @Lazy
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            String picture = oAuth2User.getAttribute("picture");
            String googleId = oAuth2User.getAttribute("sub");

            if (email == null || email.isEmpty()) {
                handleFailure(response, "Email not provided by Google");
                return;
            }

            User user = userService.registerOrLoginOAuthUser(
                    email, name, picture, AuthProvider.GOOGLE, googleId
            );

            String accessToken = jwtUtil.generateAccessToken(
                    user.getId(), user.getEmail(), user.getRole().name()
            );
            String refreshToken = jwtUtil.generateRefreshToken(user.getId());

            // Build redirect URL — do NOT double-encode
            String redirectUrl = UriComponentsBuilder
                    .fromUriString(frontendCallbackUrl)
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build()
                    .toUriString();

            log.info("Redirecting OAuth2 success to frontend");

            // Clear authentication attributes to avoid session issues
            super.clearAuthenticationAttributes(request);

            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 authentication failed: {}", e.getMessage());
            handleFailure(response, e.getMessage());
        }
    }

    private void handleFailure(HttpServletResponse response, String message) throws IOException {
        String errorUrl = UriComponentsBuilder
                .fromUriString(frontendCallbackUrl.replace("/auth/callback", "/login"))
                .queryParam("error", message)
                .build()
                .toUriString();
        response.sendRedirect(errorUrl);
    }}