package io.github.agebhar1.snippets

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication class DemoTestcontainersApplication

fun main(args: Array<String>) {
  runApplication<DemoTestcontainersApplication>(*args)
}
