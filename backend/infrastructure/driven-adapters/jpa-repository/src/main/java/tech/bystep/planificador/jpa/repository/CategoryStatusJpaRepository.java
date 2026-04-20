package tech.bystep.planificador.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tech.bystep.planificador.jpa.entity.CategoryStatusEntity;

import java.util.List;
import java.util.UUID;

public interface CategoryStatusJpaRepository extends JpaRepository<CategoryStatusEntity, UUID> {

    List<CategoryStatusEntity> findByCategoryIdOrderByDisplayOrderAsc(String categoryId);

    @Query("SELECT DISTINCT cs.categoryId FROM CategoryStatusEntity cs ORDER BY cs.categoryId")
    List<String> findDistinctCategoryIds();
}
