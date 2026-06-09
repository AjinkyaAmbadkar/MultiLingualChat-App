package com.multilingual.chat.app.entity;

/**
 * Identifies how a user registered / authenticated.
 *
 * LOCAL — registered with email + password via our own sign-up form.
 * GOOGLE — authenticated via Google OAuth2 (no password stored on our side).
 *
 * Stored as a VARCHAR string in the DB via @Enumerated(EnumType.STRING)
 * so the column is human-readable ("LOCAL" / "GOOGLE") instead of a fragile
 * ordinal integer.
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE
}
