package tech.bystep.planificador.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.bystep.planificador.api.dto.response.ApiResponse;
import tech.bystep.planificador.model.CategoryStatus;
import tech.bystep.planificador.usecase.CategoryStatusUseCase;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/category-statuses")
@RequiredArgsConstructor
public class CategoryStatusController {

    private final CategoryStatusUseCase categoryStatusUseCase;

    /** Returns ordered status list for a given category key. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryStatus>>> getByCategory(
            @RequestParam String category) {
        return ResponseEntity.ok(ApiResponse.ok(categoryStatusUseCase.findByCategoryId(category)));
    }

    /** Returns all available categories: { key: label }. */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.ok(categoryStatusUseCase.findAllCategories()));
    }
}
