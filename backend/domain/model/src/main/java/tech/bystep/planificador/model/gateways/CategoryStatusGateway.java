package tech.bystep.planificador.model.gateways;

import tech.bystep.planificador.model.CategoryStatus;

import java.util.List;
import java.util.Map;

public interface CategoryStatusGateway {

    List<CategoryStatus> findByCategoryId(String categoryId);

    /** Returns all distinct categories: key → display label. */
    Map<String, String> findAllCategories();
}
