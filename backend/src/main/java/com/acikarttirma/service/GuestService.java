package com.acikarttirma.service;

import com.acikarttirma.dto.GuestResponse;
import com.acikarttirma.exception.ApiException;
import com.acikarttirma.model.User;
import com.acikarttirma.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestService {

    private final UserRepository userRepository;

    public GuestService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public GuestResponse login(String username) {
        String name = username.trim();
        if (userRepository.existsByUsername(name)) {
            throw usernameTaken();
        }
        try {
            User user = userRepository.saveAndFlush(new User(name));
            return new GuestResponse(user.getId(), user.getUsername(), user.getPlayerToken());
        } catch (DataIntegrityViolationException e) {
            throw usernameTaken(); // iki kişi aynı anda aynı adı almaya çalıştı
        }
    }

    /** X-Player-Token başlığından kullanıcıyı bulur. */
    @Transactional(readOnly = true)
    public User authenticate(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "TOKEN_REQUIRED", "X-Player-Token başlığı gerekli");
        }
        return userRepository.findByPlayerToken(token.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "Geçersiz oyuncu anahtarı"));
    }

    private ApiException usernameTaken() {
        return new ApiException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "Bu kullanıcı adı alınmış");
    }
}