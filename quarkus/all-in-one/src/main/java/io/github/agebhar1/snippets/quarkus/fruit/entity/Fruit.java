package io.github.agebhar1.snippets.quarkus.fruit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.UuidGenerator;

import java.util.Objects;
import java.util.UUID;

import static org.hibernate.annotations.UuidGenerator.Style.VERSION_7;

@Entity
@SuppressWarnings("NullAway.Init")
public class Fruit {

    @Id
    @UuidGenerator(style = VERSION_7)
    private UUID id;

    @Version
    private long version = 1;

    @NotBlank
    @Column(columnDefinition = "TEXT", unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Metadata metadata;

    public Fruit() {
        /* required by JPA */
    }

    public Fruit(UUID uuid, long version, String name, String description, Metadata metadata) {
        this.id = uuid;
        this.version = version;
        this.name = name;
        this.description = description;
        this.metadata = metadata;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getVersion() {
        return version;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Fruit fruit)) return false;
        return Objects.equals(id, fruit.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Fruit{" +
                "id=" + id +
                ", version=" + version +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", metadata=" + metadata +
                '}';
    }

}