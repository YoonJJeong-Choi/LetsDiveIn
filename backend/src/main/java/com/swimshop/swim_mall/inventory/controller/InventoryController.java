package com.swimshop.swim_mall.inventory.controller;

import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.inventory.dto.InventoryResponseDto;
import com.swimshop.swim_mall.inventory.dto.InventoryUpdateRequestDto;
import com.swimshop.swim_mall.inventory.service.InventoryService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/partner/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * 파트너의 전체 재고 목록 조회
     * GET /api/partner/inventory
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryResponseDto>>> getMyInventories(HttpSession session) {
        List<InventoryResponseDto> inventories = inventoryService.getMyInventories(session);
        return ResponseEntity.ok(ApiResponse.success(inventories));
    }

    /**
     * 특정 옵션의 재고 조회
     * GET /api/partner/inventory/{optionNo}
     */
    @GetMapping("/{optionNo}")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> getInventoryByOptionNo(
            HttpSession session,
            @PathVariable Long optionNo
    ) {
        InventoryResponseDto inventory = inventoryService.getInventoryByOptionNo(session, optionNo);
        return ResponseEntity.ok(ApiResponse.success(inventory));
    }

    /**
     * 재고 생성
     * POST /api/partner/inventory/{optionNo}
     */
    @PostMapping("/{optionNo}")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> createInventory(
            HttpSession session,
            @PathVariable Long optionNo,
            @Valid @RequestBody InventoryUpdateRequestDto requestDto
    ) {
        InventoryResponseDto inventory = inventoryService.createInventory(session, optionNo, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(inventory));
    }

    /**
     * 재고 수정
     * PUT /api/partner/inventory/{optionNo}
     */
    @PutMapping("/{optionNo}")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> updateInventory(
            HttpSession session,
            @PathVariable Long optionNo,
            @Valid @RequestBody InventoryUpdateRequestDto requestDto
    ) {
        InventoryResponseDto inventory = inventoryService.updateInventory(session, optionNo, requestDto);
        return ResponseEntity.ok(ApiResponse.success(inventory));
    }

    /**
     * 특정 상품의 재고 조회 (옵션이 없는 상품의 경우)
     * GET /api/partner/inventory/product/{productNo}
     */
    @GetMapping("/product/{productNo}")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> getInventoryByProductNo(
            HttpSession session,
            @PathVariable Long productNo
    ) {
        InventoryResponseDto inventory = inventoryService.getInventoryByProductNo(session, productNo);
        return ResponseEntity.ok(ApiResponse.success(inventory));
    }

    /**
     * 재고 생성 (옵션이 없는 상품의 경우)
     * POST /api/partner/inventory/product/{productNo}
     */
    @PostMapping("/product/{productNo}")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> createInventoryForProduct(
            HttpSession session,
            @PathVariable Long productNo,
            @Valid @RequestBody InventoryUpdateRequestDto requestDto
    ) {
        InventoryResponseDto inventory = inventoryService.createInventoryForProduct(session, productNo, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(inventory));
    }

    /**
     * 재고 수정 (옵션이 없는 상품의 경우)
     * PUT /api/partner/inventory/product/{productNo}
     */
    @PutMapping("/product/{productNo}")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> updateInventoryForProduct(
            HttpSession session,
            @PathVariable Long productNo,
            @Valid @RequestBody InventoryUpdateRequestDto requestDto
    ) {
        InventoryResponseDto inventory = inventoryService.updateInventoryForProduct(session, productNo, requestDto);
        return ResponseEntity.ok(ApiResponse.success(inventory));
    }
}
