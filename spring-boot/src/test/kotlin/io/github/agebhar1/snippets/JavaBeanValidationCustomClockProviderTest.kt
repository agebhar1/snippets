package io.github.agebhar1.snippets

import jakarta.validation.Validator
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Past
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junitpioneer.jupiter.DefaultLocale
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration
import org.springframework.boot.autoconfigure.validation.ValidationConfigurationCustomizer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.Month.MARCH
import java.time.ZoneOffset.UTC

@ExtendWith(SpringExtension::class)
@Import(ValidationAutoConfiguration::class)
class JavaBeanValidationCustomClockProviderTest(@Autowired private val validator: Validator) {
    @Test
    fun `entity should be validated`() {
        val entity = Data(past = LocalDate.of(2022, MARCH, 11), future = LocalDate.of(2022, MARCH, 13))

        val violations = validator.validate(entity)

        assertThat(violations).isEmpty()
    }

    @Test
    @DefaultLocale(language = "en")
    fun `entity should not be validated`() {
        val entity = Data(past = LocalDate.of(2022, MARCH, 12), future = LocalDate.of(2022, MARCH, 12))

        val violations = validator.validate(entity)

        assertThat(violations)
            .hasSize(2)
            .extracting("message")
            .containsExactlyInAnyOrder("must be a past date", "must be a future date")
    }

    internal data class Data(@get:Past val past: LocalDate, @get:Future val future: LocalDate)

    @TestConfiguration(proxyBeanMethods = false)
    class Configuration {
        @Bean
        fun validationCustomizer() = ValidationConfigurationCustomizer {
            it.clockProvider { Clock.fixed(Instant.parse("2022-03-12T19:00:00Z"), UTC) }
        }
    }
}
