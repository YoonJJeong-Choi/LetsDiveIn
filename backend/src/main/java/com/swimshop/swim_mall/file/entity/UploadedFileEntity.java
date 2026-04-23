package com.swimshop.swim_mall.file.entity;

import com.swimshop.swim_mall.common.enums.AccountRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "uploaded_file")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UploadedFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileId;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 255)
    private String originalName;

    @Column(nullable = false, length = 255, unique = true)
    private String storedName;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false, length = 1000)
    private String storagePath;

    @Column(nullable = false)
    private Boolean isPrivate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountRole uploadedByRole;

    @Column(nullable = false)
    private Long uploadedBySubjectId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
