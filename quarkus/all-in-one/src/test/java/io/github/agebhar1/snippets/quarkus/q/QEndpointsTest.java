package io.github.agebhar1.snippets.quarkus.q;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.enableLoggingOfRequestAndResponseIfValidationFails;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.config.HeaderConfig.headerConfig;
import static io.restassured.http.ContentType.JSON;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonStringEquals;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.iterableWithSize;

@QuarkusTest
@TestProfile(QEndpointsTest.class)
public class QEndpointsTest implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "q.info.static[0]", "key0=value",
                "q.info.static[1]", "key1=value"
        );
    }

    @BeforeAll
    static void beforeAll() {
        enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Nested
    @DisplayName("Info Endpoint")
    class Info {

        @Test
        @DisplayName("GET /q/info - Git, Java, OS, Build and custom allowed environment (env)")
        void testInfoEndpoint() {

            var expected = """
                    {
                         "git": {
                             "branch": "${json-unit.any-string}",
                             "commit": {
                                 "id": "${json-unit.any-string}",
                                 "time": "${json-unit.any-string}"
                             }
                         },
                         "java": {
                             "version": "${json-unit.any-string}",
                             "vendor": "${json-unit.any-string}",
                             "vendorVersion": "${json-unit.any-string}"
                         },
                         "os": {
                             "name": "${json-unit.any-string}",
                             "version": "${json-unit.any-string}",
                             "arch": "${json-unit.any-string}"
                         },
                         "static": {
                            "key0": "value",
                            "key1": "value"
                         },
                         "build": {
                             "name": "snippets-quarkus-allinone",
                             "group": "io.github.agebhar1",
                             "artifact": "snippets-quarkus-allinone",
                             "version": "${json-unit.any-string}",
                             "time": "${json-unit.any-string}",
                             "quarkusVersion": "${json-unit.any-string}",
                             "enabled": "${json-unit.any-string}"
                         }
                    }
                    """;

            when().get("/q/info")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(jsonStringEquals(expected));
        }

    }

    @Nested
    @DisplayName("Logging Manager Endpoint")
    class LoggingManager {

        @Test
        @DisplayName("GET /q/logging-manager - Information on Logger(s)")
        void testLoggingManagerEndpointAllLoggers() {
            when().get("/q/logging-manager")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("$", allOf(
                            iterableWithSize(greaterThan(0)),
                            hasItem(hasEntry("name", "io.quarkus.application")))
                    );
        }

        @Test
        @DisplayName("GET /q/logging-manager?loggerName= - Information on Logger(s) filtered by 'loggerName'")
        void testLoggingManagerEndpointFilteredLoggers() {
            when().get("/q/logging-manager?loggerName=io.github.agebhar1")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("$", hasEntry("name", "io.github.agebhar1"));
        }

        @Test
        @DisplayName("POST /q/logging-manager - Update log level")
        void testLoggingManagerEndpointUpdateLogLevel() {

            var keepHeaders = headerConfig().overwriteHeadersWithName("");
            var config = RestAssured.config().headerConfig(keepHeaders);

            var response = given()
                    .config(config)
                    .contentType("application/x-www-form-urlencoded") // requires 'keepHeader' configuration, otherwise encoding is added
                    .formParam("loggerName", "io.github.agebhar1")
                    .formParam("loggerLevel", "DEBUG")
                    .when()
                    .post("/q/logging-manager")
                    .then();

            response.statusCode(201);

            when().get("/q/logging-manager?loggerName=io.github.agebhar1")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("$", allOf(
                            hasEntry("name", "io.github.agebhar1"),
                            hasEntry("effectiveLevel", "DEBUG")
                    ));
        }

    }

}
