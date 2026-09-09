package com.e_commerce.kento_shopping.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Roles answer "which app am I in" — they drive routing and which shell the
 * client renders. Permissions answer "should this button exist". The client
 * uses both to hide things; the server remains the one that decides, so a 403
 * is an expected outcome whenever permissions change mid-session.
 */
@Getter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private UserSummaryResponse user;
    private List<String> roles;
    private List<String> permissions;
}
