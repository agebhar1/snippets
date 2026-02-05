package io.github.agebhar1.snippets.quarkus.info;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

public class StaticInfoContributorTest {

    @ParameterizedTest(name = "given: {0}, expected {1} -> {2}")
    @CsvSource(delimiter = '|', value = {"""
            key=value   | key | value
            key= value  | key | value
            key =value  | key | value
            key = value | key | value
            key=value=1 | key | value=1
            key         | key | ''
            =           | ''  | ''
            """})
    @DisplayName("should split to key/value")
    void shouldSplit(String entry, String key, String value) {
        assertThat(new StaticInfoContributor(Optional.of(List.of(entry))))
                .extracting(StaticInfoContributor::data, MAP)
                .containsAllEntriesOf(Map.of(key, value));
    }

}
