package com.e_commerce.kento_shopping.controller.admin;

import com.e_commerce.kento_shopping.dto.request.admin.FlashSaleRequest;
import com.e_commerce.kento_shopping.dto.response.FlashSaleResponse;
import com.e_commerce.kento_shopping.enums.FlashSaleStatus;
import com.e_commerce.kento_shopping.service.FlashSaleAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/flash-sales")
@RequiredArgsConstructor
public class AdminFlashSaleController {
    private final FlashSaleAdminService flashSaleAdminService;

    @GetMapping
    @PreAuthorize("hasAuthority('FLASHSALE_READ_ALL')")
    public ResponseEntity<Page<FlashSaleResponse>> getAll(
            @RequestParam(required = false) FlashSaleStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size){
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "startAt"));
        return ResponseEntity.ok(flashSaleAdminService.getAll(status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FLASHSALE_READ_ALL')")
    public ResponseEntity<FlashSaleResponse> getById(@PathVariable Long id){
        return ResponseEntity.ok(flashSaleAdminService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FLASHSALE_CREATE')")
    public ResponseEntity<FlashSaleResponse> create(@Valid @RequestBody FlashSaleRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(flashSaleAdminService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FLASHSALE_UPDATE')")
    public ResponseEntity<FlashSaleResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody FlashSaleRequest request){
        return ResponseEntity.ok(flashSaleAdminService.update(id, request));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('FLASHSALE_UPDATE')")
    public ResponseEntity<FlashSaleResponse> cancel(@PathVariable Long id){
        return ResponseEntity.ok(flashSaleAdminService.cancel(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FLASHSALE_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        flashSaleAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
