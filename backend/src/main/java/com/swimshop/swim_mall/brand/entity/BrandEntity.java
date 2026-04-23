package com.swimshop.swim_mall.brand.entity;

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
@Table(name = "brand")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BrandEntity {

    @Id
    @Column(length = 100, nullable = false, updatable = false)
    private String code; // 표준 코드(슬러그/고유키) 예: SPEEDO

    @Column(length = 200, nullable = false)
    private String canonicalName; // 공식명

    @Column(length = 200, nullable = false)
    private String displayName; // 노출명

    @Column(length = 200, nullable = true)
    private String slug; // URL용 슬러그

    @Column(length = 300, nullable = true)
    private String logoUrl;

    @Column(length = 100, nullable = true)
    private String country;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
