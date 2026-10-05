package com.hottalk.hottalkserver.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_terms")
public class UserTerms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_user_id", nullable = false, unique = true)
    private String externalUserId;  // ✅ 관계 설정 없이

    @Column(nullable = false)
    private boolean termsOfService;

    @Column(nullable = false)
    private boolean locationService;

    @Column(nullable = false)
    private boolean privacyPolicy;

    @Column(nullable = false)
    private boolean privacyUse;
}