package com.swimshop.swim_mall.size.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.size.entity.SizeEntity;
import com.swimshop.swim_mall.size.entity.SizeSynonymEntity;
import com.swimshop.swim_mall.size.repository.SizeRepository;
import com.swimshop.swim_mall.size.repository.SizeSynonymRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SizeService {

    private final SizeRepository sizeRepository;
    private final SizeSynonymRepository sizeSynonymRepository;

    @Transactional
    public SizeEntity createSize(String code, String label, Integer sortOrder, Boolean isActive) {
        String normalizedCode = code.trim().toUpperCase();
        String normalizedLabel = label.trim();
        if (normalizedCode.isEmpty() || normalizedLabel.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "code/label은 필수입니다.");
        }
        if (sizeRepository.existsById(normalizedCode)) {
            SizeEntity existing = sizeRepository.findById(normalizedCode).get();
            SizeEntity updated = SizeEntity.builder()
                    .code(existing.getCode())
                    .label(normalizedLabel)
                    .sortOrder(sortOrder != null ? sortOrder : existing.getSortOrder())
                    .isActive(isActive != null ? isActive : existing.getIsActive())
                    .build();
            return sizeRepository.save(updated);
        }
        SizeEntity e = SizeEntity.builder()
                .code(normalizedCode)
                .label(normalizedLabel)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .isActive(isActive != null ? isActive : true)
                .build();
        return sizeRepository.save(e);
    }

    @Transactional
    public void updateSizeStatus(String code, Boolean isActive) {
        String normalized = code == null ? null : code.trim().toUpperCase();
        SizeEntity e = sizeRepository.findById(normalized)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "존재하지 않는 사이즈 코드입니다."));
        SizeEntity updated = SizeEntity.builder()
                .code(e.getCode())
                .label(e.getLabel())
                .sortOrder(e.getSortOrder())
                .isActive(isActive != null ? isActive : e.getIsActive())
                .build();
        sizeRepository.save(updated);
    }

    @Transactional
    public SizeSynonymEntity addSynonym(String code, String synonym) {
        String normalized = code == null ? null : code.trim().toUpperCase();
        SizeEntity e = sizeRepository.findById(normalized)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "존재하지 않는 사이즈 코드입니다."));
        String value = synonym.trim().toLowerCase();
        if (value.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "synonym은 필수입니다.");
        }
        return sizeSynonymRepository.findBySynonymIgnoreCase(value)
                .orElseGet(() -> sizeSynonymRepository.save(
                        SizeSynonymEntity.builder().size(e).synonym(value).build()
                ));
    }

    @Transactional
    public void removeSynonym(Long synonymId) {
        sizeSynonymRepository.deleteById(synonymId);
    }

    public Optional<SizeEntity> normalizeSize(String input) {
        if (input == null) return Optional.empty();
        String normalized = input.trim();
        if (normalized.isEmpty()) return Optional.empty();
        return sizeRepository.findByLabelIgnoreCase(normalized)
                .or(() -> sizeSynonymRepository.findBySynonymIgnoreCase(normalized)
                        .map(SizeSynonymEntity::getSize));
    }
}
