package com.swimshop.swim_mall.size.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "size_synonym",
       uniqueConstraints = @UniqueConstraint(name = "uk_size_synonym_value", columnNames = {"synonym"}))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SizeSynonymEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "size_code", referencedColumnName = "code", nullable = false)
    private SizeEntity size;

    @Column(length = 100, nullable = false)
    private String synonym; // 소문자 권장
}
