package com.swimshop.swim_mall.review.entity;

import com.swimshop.swim_mall.file.entity.UploadedFileEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_image")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReviewImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_no", nullable = false)
    private ReviewEntity review;

    @Column(nullable = false, length = 1000)
    private String imageUrl;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false, unique = true)
    private UploadedFileEntity file; // 업로드 파일 메타
}
