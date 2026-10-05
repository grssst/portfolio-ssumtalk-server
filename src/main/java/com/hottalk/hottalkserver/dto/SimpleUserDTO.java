package com.hottalk.hottalkserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SimpleUserDTO {
    private Long id;
    private String nickname;
    private String profileImageUrl;
    private String profileImageUrl2;
    private String profileImageUrl3;
    private Integer candy;
}
