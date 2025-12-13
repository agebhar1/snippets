package io.github.agebhar1.snippets.quarkus.fruit.entity;

import jakarta.validation.constraints.NotBlank;

import java.util.Objects;

public record Fruit(Long id, @NotBlank String name, String description) {

    public Fruit withId(Long id) {
        return new Fruit(id, name, description);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Fruit fruit = (Fruit) o;
        return Objects.equals(id, fruit.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
