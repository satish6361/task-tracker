package com.assignment.task_tracker.security;

import com.assignment.task_tracker.dto.LoginRequestDto;
import com.assignment.task_tracker.dto.SignUpRequestDto;
import com.assignment.task_tracker.dto.UserDto;
import com.assignment.task_tracker.entity.RefreshToken;
import com.assignment.task_tracker.entity.User;
import com.assignment.task_tracker.entity.enums.Role;
import com.assignment.task_tracker.exception.ConflictException;
import com.assignment.task_tracker.repository.UserRepository;
import com.assignment.task_tracker.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public UserDto signup(SignUpRequestDto signUpRequestDto) {
        User user = userRepository.findByEmail(signUpRequestDto.getEmail()).orElse(null);

        if (user != null) {
            throw new ConflictException(
                    "User already exists with this email"
            );
        }

        user = modelMapper.map(signUpRequestDto, User.class);
        user.setRoles(EnumSet.of(Role.MEMBER));
        user.setPassword(passwordEncoder.encode(signUpRequestDto.getPassword()));

        User newUser = userRepository.save(user);

        return modelMapper.map(newUser, UserDto.class);
    }

    @Transactional
    public String[] login(LoginRequestDto loginRequestDto) {
        log.info("Trying to login with email: {}", loginRequestDto.getEmail());
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginRequestDto.getEmail(), loginRequestDto.getPassword()
        ));

        User user = (User) authentication.getPrincipal();
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new String[]{
                jwtService.generateAccessToken(user),
                refreshToken.getToken()
        };
    }

    public String[] refreshToken(String incomingToken) {
        log.info("Refreshing user's access token");
        RefreshToken old = refreshTokenService.validate(incomingToken);
        refreshTokenService.revoke(old);

        User user = old.getUser();
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        return new String[]{
                jwtService.generateAccessToken(user),
                newRefreshToken.getToken()
        };
    }
}
