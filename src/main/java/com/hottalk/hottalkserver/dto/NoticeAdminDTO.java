package com.hottalk.hottalkserver.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoticeAdminDTO {
    private String title;
    private String content;
    // createdAt은 서버에서 생성
}
