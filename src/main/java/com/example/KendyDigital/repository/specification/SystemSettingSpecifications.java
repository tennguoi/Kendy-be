package com.example.KendyDigital.repository.specification;

import com.example.KendyDigital.model.setting.SystemSetting;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class SystemSettingSpecifications {

    private SystemSettingSpecifications() {
    }

    public static Specification<SystemSetting> search(String query) {
        return (root, cq, cb) -> {
            String pattern = SearchSpecificationUtils.likePattern(query);
            if (pattern != null) {
                return SearchSpecificationUtils.likeLower(cb, root.get("key"), pattern);
            }
            return cb.conjunction();
        };
    }
}
