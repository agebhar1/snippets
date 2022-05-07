package io.github.agebhar1.snippets

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.Month.MARCH
import java.time.ZoneOffset.UTC
import javax.validation.ClockProvider
import javax.validation.Validator
import javax.validation.constraints.Future
import javax.validation.constraints.Past
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junitpioneer.jupiter.DefaultLocale
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

@ExtendWith(SpringExtension::class)
class JavaBeanValidationCustomClockProviderTest(@Autowired private val validator: Validator) {

  internal data class Data(@get:Past val past: LocalDate, @get:Future val future: LocalDate)

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

  @TestConfiguration(proxyBeanMethods = false)
  class Configuration {

    @Bean
    fun customizedValidator() =
      object : LocalValidatorFactoryBean() {

        // from org.springframework.validation.beanvalidation.LocalValidatorFactoryBean:
        //
        // Bean Validation 2.0: currently not implemented here since it would imply
        // a hard dependency on the new javax.validation.ClockProvider interface.
        // To be resolved once Spring Framework requires Bean Validation 2.0+.
        // Obtain the native ValidatorFactory through unwrap(ValidatorFactory.class)
        // instead which will fully support a getClockProvider() call as well.
        override fun getClockProvider(): ClockProvider = TODO("not used")

        override fun postProcessConfiguration(configuration: javax.validation.Configuration<*>) {
          configuration.clockProvider { Clock.fixed(Instant.parse("2022-03-12T19:00:00Z"), UTC) }
        }
      }
  }
}
