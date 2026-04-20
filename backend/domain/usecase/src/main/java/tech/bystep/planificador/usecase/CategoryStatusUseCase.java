package tech.bystep.planificador.usecase;

import lombok.RequiredArgsConstructor;
import tech.bystep.planificador.model.CategoryStatus;
import tech.bystep.planificador.model.gateways.CategoryStatusGateway;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class CategoryStatusUseCase {

    private final CategoryStatusGateway categoryStatusGateway;

    public List<CategoryStatus> findByCategoryId(String categoryId) {
        return categoryStatusGateway.findByCategoryId(categoryId);
    }

    public Map<String, String> findAllCategories() {
        return categoryStatusGateway.findAllCategories();
    }
}
