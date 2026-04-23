package com.swimshop.swim_mall.color.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.color.entity.ColorEntity;
import com.swimshop.swim_mall.color.entity.ColorSynonymEntity;
import com.swimshop.swim_mall.color.repository.ColorRepository;
import com.swimshop.swim_mall.color.repository.ColorSynonymRepository;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ColorService {

    private final ColorRepository colorRepository;
    private final ColorSynonymRepository colorSynonymRepository;

    @Transactional
    public ColorEntity createColor(String code, String label, String hex, Integer sortOrder, Boolean isActive) {
        String normalizedCode = code.trim().toUpperCase();
        String normalizedLabel = label.trim();
        if (normalizedCode.isEmpty() || normalizedLabel.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "code/label은 필수입니다.");
        }
        if (colorRepository.existsById(normalizedCode)) {
            // 이미 존재하면 업데이트로 취급
            ColorEntity existing = colorRepository.findById(normalizedCode).get();
            ColorEntity updated = ColorEntity.builder()
                    .code(existing.getCode())
                    .label(normalizedLabel)
                    .hex(hex != null ? hex.trim() : existing.getHex())
                    .sortOrder(sortOrder != null ? sortOrder : existing.getSortOrder())
                    .isActive(isActive != null ? isActive : existing.getIsActive())
                    .build();
            return colorRepository.save(updated);
        }
        ColorEntity color = ColorEntity.builder()
                .code(normalizedCode)
                .label(normalizedLabel)
                .hex(hex != null ? hex.trim() : null)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .isActive(isActive != null ? isActive : true)
                .build();
        return colorRepository.save(color);
    }

    @Transactional
    public void updateColorStatus(String code, Boolean isActive) {
        String normalizedCode = code == null ? null : code.trim().toUpperCase();
        ColorEntity color = colorRepository.findById(normalizedCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "존재하지 않는 컬러 코드입니다."));
        ColorEntity updated = ColorEntity.builder()
                .code(color.getCode())
                .label(color.getLabel())
                .sortOrder(color.getSortOrder())
                .isActive(isActive != null ? isActive : color.getIsActive())
                .build();
        colorRepository.save(updated);
    }

    @Transactional
    public ColorSynonymEntity addSynonym(String code, String synonym) {
        String normalizedCode = code == null ? null : code.trim().toUpperCase();
        ColorEntity color = colorRepository.findById(normalizedCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "존재하지 않는 컬러 코드입니다."));
        String value = synonym.trim().toLowerCase();
        if (value.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "synonym은 필수입니다.");
        }
        // 중복 허용 안 함
        if (colorSynonymRepository.findBySynonymIgnoreCase(value).isPresent()) {
            return colorSynonymRepository.findBySynonymIgnoreCase(value).get();
        }
        ColorSynonymEntity entity = ColorSynonymEntity.builder()
                .color(color)
                .synonym(value)
                .build();
        return colorSynonymRepository.save(entity);
    }

    @Transactional
    public void removeSynonym(Long synonymId) {
        colorSynonymRepository.deleteById(synonymId);
    }

    /**
     * 입력 문자열을 표준 컬러 코드로 정규화합니다.
     * 1) label 일치
     * 2) synonym 일치
     * 못 찾으면 빈 Optional
     */
    public Optional<ColorEntity> normalizeColor(String input) {
        if (input == null) return Optional.empty();
        String normalized = input.trim();
        if (normalized.isEmpty()) return Optional.empty();
        return colorRepository.findByLabelIgnoreCase(normalized)
                .or(() -> colorSynonymRepository.findBySynonymIgnoreCase(normalized)
                        .map(ColorSynonymEntity::getColor));
    }

    /**
     * 표준 컬러 코드에 해당하는 비교 문자열 세트를 반환합니다.
     * - label + 모든 synonym (lower/trim 처리)
     */
    public Set<String> getComparableStringsForColorCode(String code) {
        if (code == null) return Set.of();
        Set<String> values = new HashSet<>();
        colorRepository.findById(code).ifPresent(color -> {
            values.add(color.getLabel().trim().toLowerCase());
            List<String> syns = colorSynonymRepository.findByColor_Code(code).stream()
                    .map(ColorSynonymEntity::getSynonym)
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .map(s -> s.trim().toLowerCase())
                    .collect(Collectors.toList());
            values.addAll(syns);
        });
        return values;
    }
}
