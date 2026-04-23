package com.swimshop.swim_mall.color.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "color_synonym",
       uniqueConstraints = @UniqueConstraint(name = "uk_color_synonym_value", columnNames = {"synonym"}))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ColorSynonymEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "color_code", referencedColumnName = "code", nullable = false)
    private ColorEntity color;

    @Column(length = 100, nullable = false)
    private String synonym; // 소문자 권장 (정규화 비교용)
}
