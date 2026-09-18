package ru.homyakin.seeker.infrastructure.migration;

import liquibase.changelog.ChangeLogParameters;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EquipmentCatalogLifecycleChangelogTest {
    @Test
    void mainChangelogContainsParsableCatalogLifecycleMigration() throws Exception {
        final var path = "migrations/main-changelog.xml";
        try (final var accessor = new ClassLoaderResourceAccessor()) {
            final var parser = ChangeLogParserFactory.getInstance().getParser(path, accessor);
            final var changeLog = parser.parse(path, new ChangeLogParameters(), accessor);

            Assertions.assertTrue(changeLog.getChangeSets().stream().anyMatch(changeSet ->
                changeSet.getId().equals("add-equipment-catalog-lifecycle")
            ));
        }
    }
}
