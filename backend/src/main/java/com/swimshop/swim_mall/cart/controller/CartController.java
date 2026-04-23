package com.swimshop.swim_mall.cart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.cart.dto.CartItemRequestDto;
import com.swimshop.swim_mall.cart.dto.CartItemUpdateDto;
import com.swimshop.swim_mall.cart.dto.CartResponseDto;
import com.swimshop.swim_mall.cart.service.CartService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RequestMapping("/api/cart")
@RequiredArgsConstructor
@RestController
public class CartController {

    private final CartService cartService;

    /**
     * 장바구니 조회
     */
    @GetMapping
    public ResponseEntity<CartResponseDto> getCart(HttpSession session) {
        CartResponseDto cart = cartService.getCart(session);
        return ResponseEntity.ok(cart);
    }

    /**
     * 장바구니에 아이템 추가
     */
    @PostMapping("/items")
    public ResponseEntity<String> addItem(
            HttpSession session,
            @RequestBody CartItemRequestDto requestDto
    ) {
        cartService.addItem(session, requestDto);
        return ResponseEntity.ok("장바구니에 추가되었습니다.");
    }

    /**
     * 장바구니 아이템 수정 (수량, 옵션 동시 변경 가능)
     */
    @PutMapping("/items/{cartItemNo}")
    public ResponseEntity<String> updateItem(
            HttpSession session,
            @PathVariable Long cartItemNo,
            @RequestBody CartItemUpdateDto updateDto
    ) {
        cartService.updateItem(session, cartItemNo, updateDto);
        return ResponseEntity.ok("장바구니 아이템이 수정되었습니다.");
    }

    /**
     * 장바구니 아이템 삭제
     */
    @DeleteMapping("/items/{cartItemNo}")
    public ResponseEntity<String> removeItem(
            HttpSession session,
            @PathVariable Long cartItemNo
    ) {
        cartService.removeItem(session, cartItemNo);
        return ResponseEntity.ok("장바구니에서 삭제되었습니다.");
    }
}
