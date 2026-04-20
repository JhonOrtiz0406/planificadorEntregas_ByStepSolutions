package tech.bystep.planificador.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tech.bystep.planificador.jpa.entity.CategoryStatusEntity;
import tech.bystep.planificador.jpa.repository.CategoryStatusJpaRepository;
import tech.bystep.planificador.model.CategoryStatus;
import tech.bystep.planificador.model.gateways.CategoryStatusGateway;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CategoryStatusAdapter implements CategoryStatusGateway {

    private final CategoryStatusJpaRepository repository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<CategoryStatus> findByCategoryId(String categoryId) {
        return repository.findByCategoryIdOrderByDisplayOrderAsc(categoryId)
                .stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public Map<String, String> findAllCategories() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, label FROM org_categories ORDER BY id");
        Map<String, String> result = new LinkedHashMap<>();
        rows.forEach(r -> result.put((String) r.get("id"), (String) r.get("label")));
        return result;
    }

    private CategoryStatus toModel(CategoryStatusEntity e) {
        return CategoryStatus.builder()
                .id(e.getId()).categoryId(e.getCategoryId()).statusKey(e.getStatusKey())
                .label(e.getLabel()).displayOrder(e.getDisplayOrder()).isFinal(e.isFinal())
                .build();
    }
}
