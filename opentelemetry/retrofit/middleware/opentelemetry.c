#include <errno.h>
#include <openssl/sha.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <tgmath.h>
#include <time.h>
#include <unistd.h>

#include "opentelemetry.h"

typedef enum { STATE_READY, STATE_PROCESSING, STATE_EXIT } state_t;

static int opentelemetry_fd = -1;

static struct timespec opentelemetry_gateway_timeout = {.tv_sec = 0, .tv_nsec = 25'000'000};

static pthread_t opentelemetry_gateway_thread;
static pthread_mutex_t opentelemetry_state_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t opentelemetry_state_cond = PTHREAD_COND_INITIALIZER;
static pthread_mutex_t opentelemetry_event_mutex = PTHREAD_MUTEX_INITIALIZER;

static unsigned char opentelemetry_buffer[1024 * 1024] = {};
static size_t opentelemetry_buffer_size = 0;
static state_t opentelemetry_state;
static uint64_t opentelemetry_dropped_events = 0;
static OpenTelemetryEvent opentelemetry_event;

static inline void mutex_incr(pthread_mutex_t *mutex, uint64_t *value) {
    if (pthread_mutex_lock(mutex) != 0) {
        perror("pthread_mutex_lock");
    }
    (*value)++;
    if (pthread_mutex_unlock(mutex) != 0) {
        perror("pthread_mutex_unlock");
    }
}

static void *opentelemetry_gateway_thread_fn([[maybe_unused]] void *arg) {
    while (true) {
        if (pthread_mutex_lock(&opentelemetry_state_mutex) != 0) {
            perror("pthread_mutex_lock");
        }

        while (opentelemetry_state != STATE_PROCESSING && opentelemetry_state != STATE_EXIT) {
            if (pthread_cond_wait(&opentelemetry_state_cond, &opentelemetry_state_mutex) != 0) {
                perror("pthread_cond_wait");
            }
        }

        if (opentelemetry_state == STATE_EXIT) {
            break;
        }

        if (pthread_mutex_unlock(&opentelemetry_state_mutex) != 0) {
            perror("pthread_mutex_unlock");
        }

        SHA256(opentelemetry_buffer, opentelemetry_buffer_size, opentelemetry_event.key);

        if (pthread_mutex_lock(&opentelemetry_event_mutex) != 0) {
            perror("pthread_mutex_lock");
        }
        opentelemetry_event.dropped_events = opentelemetry_dropped_events;
        if (pthread_mutex_unlock(&opentelemetry_event_mutex) != 0) {
            perror("pthread_mutex_unlock");
        }

        const ssize_t sent = send(opentelemetry_fd, &opentelemetry_event, sizeof(opentelemetry_event), MSG_NOSIGNAL);
        if (sent == -1) {
            perror("send");
        }

        if (pthread_mutex_lock(&opentelemetry_state_mutex) != 0) {
            perror("pthread_mutex_lock");
        }

        opentelemetry_state = STATE_READY;

        if (pthread_cond_signal(&opentelemetry_state_cond) != 0) {
            perror("pthread_cond_signal");
        }

        if (pthread_mutex_unlock(&opentelemetry_state_mutex) != 0) {
            perror("pthread_mutex_unlock");
        }
    }
    if (pthread_mutex_unlock(&opentelemetry_state_mutex) != 0) {
        perror("pthread_mutex_unlock");
    }
    return nullptr;
}

int opentelemetry_connect() {
    const char *otel_sdk_disabled = nullptr;
    if ((otel_sdk_disabled = getenv("OTEL_SDK_DISABLED")) != NULL && strcmp(otel_sdk_disabled, "true") == 0) {
        return 0;
    }

    const char *gateway_timeout = getenv("GATEWAY_TIMEOUT");
    if (gateway_timeout != nullptr) {
        const double value = atof(gateway_timeout);
        if (value < 0.0) {
            fprintf(stderr, "ERROR: GATEWAY_TIMEOUT must be >= 0.0\n");
        } else {
            opentelemetry_gateway_timeout.tv_sec = (time_t) value;
            opentelemetry_gateway_timeout.tv_nsec = (long) ((value - floor(value)) * 1e9);
        }
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

    opentelemetry_state = STATE_READY;
    if (pthread_create(&opentelemetry_gateway_thread, nullptr, opentelemetry_gateway_thread_fn, nullptr) != 0) {
        perror("pthread_create");
        exit(EXIT_FAILURE);
    }

    return 0;
}

int opentelemetry_disconnect() {
    if (pthread_mutex_lock(&opentelemetry_state_mutex) != 0) {
        perror("pthread_mutex_lock");
    }

    while (opentelemetry_state != STATE_READY) {
        if (pthread_cond_wait(&opentelemetry_state_cond, &opentelemetry_state_mutex) != 0) {
            perror("pthread_cond_wait");
        }
    }

    opentelemetry_state = STATE_EXIT;

    if (pthread_cond_signal(&opentelemetry_state_cond) != 0) {
        perror("pthread_cond_signal");
    }
    if (pthread_mutex_unlock(&opentelemetry_state_mutex) != 0) {
        perror("pthread_mutex_unlock");
    }
    if (pthread_join(opentelemetry_gateway_thread, nullptr) != 0) {
        perror("pthread_join");
    }

    if (opentelemetry_fd == -1) {
        return 0;
    }
    const int r = close(opentelemetry_fd);
    opentelemetry_fd = -1;

    return r;
}

static int opentelemetry_execute(const ClientOpCode opcode, const char *queue, const unsigned char *buffer,
                                 const size_t size) {
    if (opentelemetry_fd == -1) {
        return -1;
    }

    struct timespec abs_timeout;
    clock_gettime(CLOCK_REALTIME, &abs_timeout);

    abs_timeout.tv_sec += opentelemetry_gateway_timeout.tv_sec;
    abs_timeout.tv_nsec += opentelemetry_gateway_timeout.tv_nsec;
    abs_timeout.tv_sec += abs_timeout.tv_nsec / 1'000'000'000;
    abs_timeout.tv_nsec = abs_timeout.tv_nsec % 1'000'000'000;

    const int rv_mutex = pthread_mutex_timedlock(&opentelemetry_state_mutex, &abs_timeout);
    if (rv_mutex != 0) {
        perror("pthread_mutex_timedlock");
        return -1;
    }
    if (rv_mutex == ETIMEDOUT) {
        mutex_incr(&opentelemetry_event_mutex, &opentelemetry_dropped_events);
        return ETIMEDOUT;
    }

    while (opentelemetry_state != STATE_READY) {
        const int rv_cond = pthread_cond_timedwait(&opentelemetry_state_cond, &opentelemetry_state_mutex, &abs_timeout);
        if (rv_cond == ETIMEDOUT) {
            mutex_incr(&opentelemetry_event_mutex, &opentelemetry_dropped_events);
            if (pthread_mutex_unlock(&opentelemetry_state_mutex) != 0) {
                perror("pthread_mutex_unlock");
            }
            return ETIMEDOUT;
        }
    }

    opentelemetry_event.opcode = opcode;

    strncpy(opentelemetry_event.queue, queue, sizeof(opentelemetry_event.queue) - 1);
    opentelemetry_event.queue[sizeof(opentelemetry_event.queue) - 1] = '\0';

    clock_gettime(CLOCK_REALTIME, &opentelemetry_event.ts);

    memcpy(opentelemetry_buffer, buffer, size);
    opentelemetry_buffer_size = size;
    opentelemetry_state = STATE_PROCESSING;

    if (pthread_cond_signal(&opentelemetry_state_cond) != 0) {
        perror("pthread_cond_signal");
    }

    if (pthread_mutex_unlock(&opentelemetry_state_mutex) != 0) {
        perror("pthread_mutex_unlock");
    }

    return 0;
}

int opentelemetry_read(const char *queue, const unsigned char *buffer, const size_t size) {
    return opentelemetry_execute(CLIENT_READ, queue, buffer, size);
}

int opentelemetry_write(const char *queue, const unsigned char *buffer, const size_t size) {
    return opentelemetry_execute(CLIENT_WRITE, queue, buffer, size);
}
