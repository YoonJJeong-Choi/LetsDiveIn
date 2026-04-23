package com.swimshop.swim_mall.size.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "size")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SizeEntity {

    @Id
    @Column(length = 50, nullable = false, updatable = false)
    private String code; // 표준 코드 (예: S, M, L, FREE)

    @Column(length = 100, nullable = false)
    private String label; // 표시 라벨 (예: 스몰)

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
