#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <time.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>

#include <openssl/md5.h>
#include <openssl/sha.h>

#include "middleware.h"
#include "opentelemetry.h"

static int opentelemetry_fd = -1;

int opentelemetry_connect() {
    const char *otel_sdk_disabled = nullptr;
    if ((otel_sdk_disabled = getenv("OTEL_SDK_DISABLED")) != NULL && strcmp(otel_sdk_disabled, "true") == 0) {
        return 0;
    }

    char *otel_service_name = nullptr;
    if ((otel_service_name = getenv("OTEL_SERVICE_NAME")) != NULL) {
        otel_service_name = strdup(otel_service_name);
    }
    if (otel_service_name == nullptr || strlen(otel_service_name) == 0) {
        if (otel_service_name != nullptr) { free(otel_service_name); }
        return 0;
    }

    opentelemetry_fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (opentelemetry_fd == -1) {
        free(otel_service_name);
        return -1;
    }

    struct sockaddr_un addr = {.sun_family = AF_LOCAL};
    strcpy(addr.sun_path, "/tmp/opentelemetry.sock");

    if (connect(opentelemetry_fd, (struct sockaddr *) &addr, sizeof(addr)) == -1) {
        close(opentelemetry_fd);
        free(otel_service_name);
        return -1;
    }

    const ssize_t size = strlen(otel_service_name);
    if (send(opentelemetry_fd, &size, sizeof(size), MSG_NOSIGNAL) == sizeof(size)) {
        (void) send(opentelemetry_fd, otel_service_name, size, MSG_NOSIGNAL);
    }

    free(otel_service_name);

    return 0;
}

int opentelemetry_disconnect() {
    if (opentelemetry_fd == -1) {
        return 0;
    }
    const int r = close(opentelemetry_fd);
    opentelemetry_fd = -1;
    return r;
}

int opentelemetry_read(const char *queue, const unsigned char *buffer, size_t size) {
    if (opentelemetry_fd == -1) {
        return 0;
    }

    OpenTelemetryEvent event = {.opcode = CLIENT_READ, .key = {0}};

    strncpy(event.queue, queue, sizeof(event.queue) - 1);
    event.queue[sizeof(event.queue) - 1] = '\0';

    clock_gettime(CLOCK_REALTIME, &event.ts);

    SHA256(buffer, size, event.key);
    if (send(opentelemetry_fd, &event, sizeof(event), MSG_NOSIGNAL) == -1) {
        middleware_disconnect();
        return -1;
    }

    return 0;
}

int opentelemetry_write(const char *queue, const unsigned char *buffer, size_t size) {
    if (opentelemetry_fd == -1) {
        return 0;
    }

    OpenTelemetryEvent event = {.opcode = CLIENT_WRITE, .key = {0}};

    strncpy(event.queue, queue, sizeof(event.queue) - 1);
    event.queue[sizeof(event.queue) - 1] = '\0';

    clock_gettime(CLOCK_REALTIME, &event.ts);

    SHA256(buffer, size, event.key);

    if (send(opentelemetry_fd, &event, sizeof(event), MSG_NOSIGNAL) == -1) {
        middleware_disconnect();
        return -1;
    }

    return 0;
}
