package com.swimshop.swim_mall.customer.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객-태그 매핑 엔티티
 * 고객과 태그의 다대다 관계를 표현
 */
@Entity
@Table(
    name = "customer_tag_mapping",
    uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "tag_id"})
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomerTagMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    private Long mappingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private CustomerTagEntity tag;

    /**
     * 매핑 생성
     */
    public static CustomerTagMappingEntity create(CustomerEntity customer, CustomerTagEntity tag) {
        return CustomerTagMappingEntity.builder()
                .customer(customer)
                .tag(tag)
                .build();
    }
}
