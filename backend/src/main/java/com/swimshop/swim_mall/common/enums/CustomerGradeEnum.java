package com.swimshop.swim_mall.common.enums;

/**
 * 고객 등급 Enum
 * 등급은 고정되어 있으며, 생성/삭제가 불가능하고 수정만 가능합니다.
 */
public enum CustomerGradeEnum {

    BEGINNER("BEGINNER", "초보자", 1),
    SWIMMER("SWIMMER", "수영인", 2),
    PRO("PRO", "프로", 3),
    MASTER("MASTER", "마스터", 4),
    LEGEND("LEGEND", "레전드", 5);

    private final String code;
    private final String label;
    private final Integer level;

    CustomerGradeEnum(String code, String label, Integer level) {
        this.code = code;
        this.label = label;
        this.level = level;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public Integer getLevel() {
        return level;
    }

    /**
     * 코드로 Enum 찾기
     */
    public static CustomerGradeEnum fromCode(String code) {
        for (CustomerGradeEnum grade : values()) {
            if (grade.code.equals(code)) {
                return grade;
            }
        }
        throw new IllegalArgumentException("Unknown grade code: " + code);
    }

    /**
     * 레벨로 Enum 찾기
     */
    public static CustomerGradeEnum fromLevel(Integer level) {
        for (CustomerGradeEnum grade : values()) {
            if (grade.level.equals(level)) {
                return grade;
            }
        }
        throw new IllegalArgumentException("Unknown grade level: " + level);
    }
}
