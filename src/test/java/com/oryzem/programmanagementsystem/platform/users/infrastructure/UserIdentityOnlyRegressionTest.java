package com.oryzem.programmanagementsystem.platform.users.infrastructure;

import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class)
class UserIdentityOnlyRegressionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void appUserSchemaShouldNotExposeLegacyAccessColumns() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Set<String> columns = jdbcTemplate.queryForList(
                        """
                                select column_name
                                from information_schema.columns
                                where table_name = 'app_user'
                                """,
                        String.class)
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Assertions.assertThat(columns)
                .doesNotContain("role", "tenant_id", "tenant_type");
    }

    @Test
    void managedUserAndUserEntityShouldStayIdentityOnly() {
        Set<String> managedUserComponents = Arrays.stream(ManagedUser.class.getRecordComponents())
                .map(component -> component.getName().toLowerCase())
                .collect(Collectors.toSet());
        Set<String> userEntityFields = Arrays.stream(UserEntity.class.getDeclaredFields())
                .map(field -> field.getName().toLowerCase())
                .collect(Collectors.toSet());

        Assertions.assertThat(managedUserComponents)
                .doesNotContain("role", "tenantid", "tenanttype");
        Assertions.assertThat(userEntityFields)
                .doesNotContain("role", "tenantid", "tenanttype");
    }
}
