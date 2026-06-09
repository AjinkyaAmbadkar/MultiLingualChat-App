package com.multilingual.chat.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.multilingual.chat.app.entity.RefreshToken;
import com.multilingual.chat.app.entity.User;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Looks up a refresh token by its string value.
     * Used during token refresh: client sends the token string → we validate it
     * here.
     *
     * Returns Optional.empty() if the token doesn't exist (invalid / already
     * deleted).
     * Spring Data generates the query: SELECT * FROM refresh_tokens WHERE token = ?
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Deletes all refresh tokens belonging to a user.
     * Called on:
     * - Password change (invalidate all sessions)
     * - Account deletion
     * - "Log out from all devices" feature
     *
     * Spring Data generates: DELETE FROM refresh_tokens WHERE user_id = ?
     */
    void deleteAllByUser(User user);
}
