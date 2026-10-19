package com.tttn.webthitracnghiem.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryQueryTest {

    @Test
    void globalUserSearchUsesOneUtf8CollationForAllLikeComparisons() throws Exception {
        Method search = UserRepository.class.getMethod("search", String.class,
                org.springframework.data.domain.Pageable.class);
        Query query = search.getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value())
                .contains("CONVERT(u.id USING utf8mb4) COLLATE utf8mb4_unicode_ci")
                .contains("CONVERT(u.full_name USING utf8mb4) COLLATE utf8mb4_unicode_ci")
                .contains("CONVERT(:name USING utf8mb4)")
                .contains("COLLATE utf8mb4_unicode_ci");
        assertThat(query.countQuery())
                .contains("CONVERT(u.full_name USING utf8mb4) COLLATE utf8mb4_unicode_ci");
    }
}
