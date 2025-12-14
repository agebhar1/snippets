package io.github.agebhar1.snippets.quarkus.fruit;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonStringEquals;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestProfile(FruitResourceTest.Profile.class)
class FruitResourceTest {

    static final ByteArrayOutputStream stdOut = new ByteArrayOutputStream();

    public static class Profile implements QuarkusTestProfile {

        static {
            Locale.setDefault(Locale.GERMAN);
            debug("===");
            System.setOut(new PrintStream(System.out) {
                @Override
                public void write(byte[] buf, int off, int len) {
                    stdOut.write(buf, off, len);
                    debug("-->");
                    super.write(buf, off, len);
                }
            });
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.log.category.\"io.github.agebhar1.snippets.quarkus.fruit\".level", "debug",
                    "quarkus.log.json.pretty-print", "false",
                    "quarkus.log.json.log-format", "ecs",
                    "quarkus.log.json.console.enabled", "true"
            );
        }
    }

    @Test
    @Order(1)
    @DisplayName("GET /fruits")
    void testFruitsGET() {

        var expected = """
                [
                    {
                        "id": 1,
                        "name": "Apple",
                        "description": "Winter fruit"
                    },
                    {
                        "id": 2,
                        "name": "Pineapple",
                        "description": "Tropical fruit"
                    }
                ]
                """;

        given()
                .when()
                .get("/fruits")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(jsonStringEquals(expected));
        debug("<--");
    }

    @Test
    @Order(2)
    @DisplayName("GET /fruits/{id}")
    void testFruitGETbyId() {

        var expected = """
                {
                    "id": 1,
                    "name": "Apple",
                    "description": "Winter fruit"
                }
                """;

        given()
                .when()
                .get("/fruits/1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(jsonStringEquals(expected));
        debug("<--");
    }

    @Test
    @Order(3)
    @DisplayName("GET /fruits/{id} - Not Found with RFC 9457 response")
    void testFruitGETbyIdNotFound() {

        // https://datatracker.ietf.org/doc/html/rfc9457
        var expected = """
                {
                    "status": 404,
                    "title": "Not Found",
                    "detail": "HTTP 404 Not Found",
                    "instance": "/fruits/0"
                }
                """;

        given()
                .when()
                .get("/fruits/0")
                .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body(jsonStringEquals(expected));
        debug("<--");
    }

    @Test
    @Order(4)
    @DisplayName("POST /fruits/")
    void testFruitPOST() {

        var response = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Orange",
                            "description": "Winter fruit"
                        }
                        """)
                .redirects().follow(false)
                .post("/fruits");

        response.then()
                .statusCode(303)
                .header("Location", endsWith("/fruits/3"));
        debug("<--");
    }

    @Test
    @Order(5)
    @DisplayName("POST /fruits/ - Bad Request")
    void testFruitPOSTBadRequest() {

        var expected = """
                {
                    "status": 400,
                    "title": "Bad Request",
                    "instance": "/fruits",
                    "violations": [
                        {
                            "field": "name",
                            "in": "body",
                            "message": "darf nicht leer sein"
                        }
                    ]
                }
                """;

        var response = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                             "name": ""
                        }
                        """)
                .redirects().follow(false)
                .post("/fruits");

        response.then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body(jsonStringEquals(expected));
        debug("<--");
    }

    @Test
    @Order(6)
    @DisplayName("DELETE /fruits/{id}")
    void testFruitDELETE() {
        given()
                .when()
                .delete("/fruits/1")
                .then()
                .statusCode(204)
                .body(equalTo(""));
        debug("<--");
    }

    @Test
    @Order(7)
    @DisplayName("DELETE /fruits/{id} - Not Found with RFC 9457 response")
    void testFruitDELETENotFound() {

        // https://datatracker.ietf.org/doc/html/rfc9457
        var expected = """
                {
                    "status": 404,
                    "title": "Not Found",
                    "detail": "HTTP 404 Not Found",
                    "instance": "/fruits/0"
                }
                """;

        given()
                .when()
                .delete("/fruits/0")
                .then()
                .statusCode(404)
                .body(jsonStringEquals(expected));
        debug("<--");
    }

    static void debug(String prefix) {
        System.err.println(prefix + " stdOut@" + System.identityHashCode(stdOut) + " " + Thread.currentThread());
        System.err.println(prefix + " stdOut.size: " + stdOut.size());
    }

    /*
    === stdOut@1925785585 Thread[#1,main,5,main]
    === stdOut.size: 0
    === stdOut@416579056 Thread[#1,main,5,main]
    === stdOut.size: 0
    [INFO] Running io.github.agebhar1.snippets.quarkus.fruit.FruitResourceTest
    --> stdOut@1925785585 Thread[#1,main,5,main]
    --> stdOut.size: 127
    2025-12-14 19:17:25,925 INFO  [org.hibernate.validator.internal.util.Version] (main) HV000001: Hibernate Validator 9.1.0.Final
    --> stdOut@1925785585 Thread[#1,main,5,main]
    --> stdOut.size: 471
    {"@timestamp":"2025-12-14T19:17:26.328+01:00","log.logger":"io.quarkus","log.level":"INFO","process.thread.name":"main","process.thread.id":1,"host.name":"babbage","field-name-1":"12345","message":"snippets-quarkus 0.0.1-SNAPSHOT on JVM (powered by Quarkus 3.30.3) started in 2.066s. Listening on: http://localhost:8081","ecs.version":"9.0.0"}
    --> stdOut@1925785585 Thread[#1,main,5,main]
    --> stdOut.size: 718
    {"@timestamp":"2025-12-14T19:17:26.335+01:00","log.logger":"io.quarkus","log.level":"INFO","process.thread.name":"main","process.thread.id":1,"host.name":"babbage","field-name-1":"12345","message":"Profile test activated. ","ecs.version":"9.0.0"}
    --> stdOut@1925785585 Thread[#1,main,5,main]
    --> stdOut.size: 1110
    {"@timestamp":"2025-12-14T19:17:26.335+01:00","log.logger":"io.quarkus","log.level":"INFO","process.thread.name":"main","process.thread.id":1,"host.name":"babbage","field-name-1":"12345","message":"Installed features: [cdi, hibernate-validator, logging-json, logging-manager, rest, rest-jackson, resteasy-problem, smallrye-context-propagation, smallrye-health, vertx]","ecs.version":"9.0.0"}
    --> stdOut@1925785585 Thread[#61,executor-thread-1,5,main]
    --> stdOut.size: 1494
    {"@timestamp":"2025-12-14T19:17:26.956+01:00","log.logger":"io.github.agebhar1.snippets.quarkus.fruit.control.InMemoryFruitRepository","log.level":"DEBUG","process.thread.name":"executor-thread-1","process.thread.id":61,"host.name":"babbage","arguments":{"entity":{"id":null,"name":"Apple","description":"Winter fruit"}},"field-name-1":"12345","message":"save","ecs.version":"9.0.0"}
    --> stdOut@1925785585 Thread[#61,executor-thread-1,5,main]
    --> stdOut.size: 1884
    {"@timestamp":"2025-12-14T19:17:26.975+01:00","log.logger":"io.github.agebhar1.snippets.quarkus.fruit.control.InMemoryFruitRepository","log.level":"DEBUG","process.thread.name":"executor-thread-1","process.thread.id":61,"host.name":"babbage","arguments":{"entity":{"id":null,"name":"Pineapple","description":"Tropical fruit"}},"field-name-1":"12345","message":"save","ecs.version":"9.0.0"}
    --> stdOut@1925785585 Thread[#61,executor-thread-1,5,main]
    --> stdOut.size: 2194
    {"@timestamp":"2025-12-14T19:17:26.976+01:00","log.logger":"io.github.agebhar1.snippets.quarkus.fruit.control.InMemoryFruitRepository","log.level":"DEBUG","process.thread.name":"executor-thread-1","process.thread.id":61,"host.name":"babbage","field-name-1":"12345","message":"findAll()","ecs.version":"9.0.0"}
    <-- stdOut@416579056 Thread[#1,main,5,main]
    <-- stdOut.size: 0
    */

}