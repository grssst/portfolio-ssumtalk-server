package com.hottalk.hottalkserver.dto;

import lombok.Data;

@Data
public class UserAdminDTO {
    private Long id;
    private String externalUserId;
    private String nickname;
    private String gender;
    private Integer age;
    private String profileImageUrl;
    private Integer candy;
    // 기타 필요한 필드...
}
