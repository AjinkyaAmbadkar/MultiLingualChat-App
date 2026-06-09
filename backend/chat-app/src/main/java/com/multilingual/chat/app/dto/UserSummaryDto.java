package com.multilingual.chat.app.dto;

import com.multilingual.chat.app.entity.User;

/**
 * Safe public representation of a user — never exposes passwordHash or googleId.
 * Used by GET /api/users and GET /api/users/me so the frontend can render
 * user lists and profile info without sensitive fields leaking into the response.
 */
public class UserSummaryDto {

    private Long id;
    private String name;
    private String email;
    private String pictureUrl;
    private String preferredLanguage;

    public UserSummaryDto() {}

    public static UserSummaryDto from(User user) {
        UserSummaryDto dto = new UserSummaryDto();
        dto.id                = user.getId();
        dto.name              = user.getname();
        dto.email             = user.getemail();
        dto.pictureUrl        = user.getPictureUrl();
        dto.preferredLanguage = user.getPreferredLanguage();
        return dto;
    }

    public Long getId()                  { return id; }
    public String getName()              { return name; }
    public String getEmail()             { return email; }
    public String getPictureUrl()        { return pictureUrl; }
    public String getPreferredLanguage() { return preferredLanguage; }
}
