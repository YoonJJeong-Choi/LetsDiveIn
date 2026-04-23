package com.swimshop.swim_mall.color.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "color")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ColorEntity {

    @Id
    @Column(length = 50, nullable = false, updatable = false)
    private String code; // 표준 코드 (예: BLACK)

    @Column(length = 100, nullable = false)
    private String label; // 표시 라벨 (예: 블랙)

    @Column(length = 7, nullable = true)
    private String hex; // HEX 색상 코드 (예: #000000)

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
