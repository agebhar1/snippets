#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <tgmath.h>
#include <time.h>
#include <unistd.h>
#ifdef DIGEST
#include <openssl/sha.h>
#endif
#include <sys/socket.h>
#include <sys/un.h>

typedef enum { STATE_READY, STATE_PROCESSING, STATE_EXIT } state_t;

constexpr unsigned char data[1024 * 1024] = {};
unsigned char data_copy[sizeof(data)] = {};

static volatile state_t state = STATE_READY;
static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t cond = PTHREAD_COND_INITIALIZER;

typedef struct {
    unsigned int events;
    size_t size;
    struct timeval timeout_select;
    struct timespec timeout_timed;
} Configuration;

Configuration get_config(const int argc, char *argv[]) {
    Configuration config = {
        .events = 1'000,
        .size = 72,
        .timeout_select = {
            .tv_sec = 0,
            .tv_usec = 100'000
        },
        .timeout_timed = {
            .tv_sec = 0,
            .tv_nsec = 12'500'000
        }
    };

    int opt;
    bool error = false;
    while ((opt = getopt(argc, argv, "e:r:s:t:u:")) != -1) {
        switch (opt) {
            case 'e':
                const int events = atoi(optarg);
                if (events < 1) {
                    fprintf(stderr, "ERROR: option 'e' (events) must be >= 1\n");
                    error = true;
                    break;
                }
                config.events = events;
                break;
            case 's':
                const int size = atoi(optarg);
                if (size < 1) {
                    fprintf(stderr, "ERROR: option 's' (size) must be >= 1\n");
                    error = true;
                    break;
                }
                config.size = (size_t) size;
                break;
            case 't':
                const double timeout_select = atof(optarg);
                if (timeout_select < 0.0) {
                    fprintf(stderr, "ERROR: option 't' (timeout select) must be >= 0.0\n");
                    error = true;
                    break;
                }
                config.timeout_select.tv_sec = (time_t) timeout_select;
                config.timeout_select.tv_usec = (long) ((timeout_select - floor(timeout_select)) * 1e6);
                break;
            case 'u':
                const double timeout_timed = atof(optarg);
                if (timeout_timed < 0.0) {
                    fprintf(stderr, "ERROR: option 'u' (timeout timed) must be >= 0.0\n");
                    error = true;
                    break;
                }
                config.timeout_timed.tv_sec = (time_t) timeout_timed;
                config.timeout_timed.tv_nsec = (long) ((timeout_timed - floor(timeout_timed)) * 1e9);
                break;
            default:
                fprintf(
                    stderr,
                    "Usage: %s [-e #events (default 1000)] [-t timeout select (default 0.1)] [-u timeout timed (default 0.0125)]\n",
                    argv[0]);
                error = true;
        }
    }
    if (error) {
        exit(EXIT_FAILURE);
    }

    return config;
}

static void *threadFunc(void *arg) {
    const Configuration config = *(Configuration *) arg;

    const int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (fd == -1) {
        perror("socket");
        exit(EXIT_FAILURE);
    }

    struct sockaddr_un addr = {.sun_family = AF_LOCAL};
    strcpy(addr.sun_path, "main.sock");

    if (connect(fd, (struct sockaddr *) &addr, sizeof(addr)) == -1) {
        close(fd);
        perror("connect");
        exit(EXIT_FAILURE);
    }

    const unsigned char buffer[config.size] = {};
#ifdef DIGEST
    unsigned char md[32] = {};
#endif

    unsigned int i = 0;
    bool done = false;
    while (!done) {
        if (pthread_mutex_lock(&mutex) != 0) {
            perror("pthread_mutex_lock");
        }
#ifdef DELAY
        fprintf(stderr, "[thread][%d] locked mutex\n", i);
#endif
        while (state != STATE_PROCESSING && state != STATE_EXIT) {
            if (pthread_cond_wait(&cond, &mutex) != 0) {
                perror("pthread_cond_wait");
            }
        }

        if (state == STATE_EXIT) {
            done = true;
        } else {
            if (pthread_mutex_unlock(&mutex) != 0) {
                perror("pthread_mutex_unlock");
            }

            bool error = false;
            const unsigned char *buffer_ptr = buffer;
            size_t remaining = config.size;
#ifdef DIGEST
            SHA256(data_copy, sizeof(data_copy), md);
#endif

            while (!error && remaining > 0) {
                const ssize_t sent = send(fd, buffer_ptr, remaining, MSG_NOSIGNAL | MSG_DONTWAIT);
                if (sent != -1) {
                    remaining -= sent;
                    buffer_ptr += sent;
                } else {
#ifdef DEBUG
                    fprintf(stderr, "%d event(s), sent %ld bytes [%s]\n", i, config.size * i, strerror(errno));
#endif
                    error = errno != EAGAIN && errno != EWOULDBLOCK;
                    if (!error) {
                        fd_set writefds;
                        FD_ZERO(&writefds);
                        FD_SET(fd, &writefds);
                        struct timeval timeout = config.timeout_select;
                        // https://man7.org/linux/man-pages/man2/select.2.html
                        (void) select(fd + 1, nullptr, &writefds, nullptr, &timeout);
                    }
                }
#ifdef DEBUG
                fprintf(stderr, "%d event(s), sent %ld bytes, remaining %ld bytes\n", i, config.size * i, remaining);
#endif
            }

#ifdef DELAY
            struct timespec duration = {.tv_sec = 1, .tv_nsec = 0};
            nanosleep(&duration, nullptr);
#endif
            if (pthread_mutex_lock(&mutex) != 0) {
                perror("pthread_mutex_lock");
            }

            state = STATE_READY;

            if (pthread_cond_signal(&cond) != 0) {
                perror("pthread_cond_signal");
            }
#ifdef DEBUG
            if (i % 1024 == 0) {
                printf("%d event(s), sent %ld bytes\n", i, config.size * i);
                fflush(stdout);
            }
#endif
        }

        if (pthread_mutex_unlock(&mutex) != 0) {
            perror("pthread_mutex_unlock");
        }
#ifdef DELAY
        fprintf(stderr, "[thread][%d] unlocked mutex\n", i);
#endif
        i++;
    }
    // i -= 1; // done
    // printf("processed: %d event(s), sent %ld bytes \n", i, config.size * i);

    close(fd);
    return nullptr;
}

int main(const int argc, char *argv[]) {
    Configuration config = get_config(argc, argv);

    pthread_t thread;
    if (pthread_create(&thread, nullptr, threadFunc, &config) != 0) {
        perror("pthread_create");
        exit(EXIT_FAILURE);
    }

    unsigned int i = 0;
    unsigned int dropped_events = 0;

    struct timespec start;
    clock_gettime(CLOCK_REALTIME, &start);

    while (i < config.events) {
        struct timespec abs_timeout;
        clock_gettime(CLOCK_REALTIME, &abs_timeout);

        abs_timeout.tv_sec += config.timeout_timed.tv_sec;
        abs_timeout.tv_nsec += config.timeout_timed.tv_nsec;
        abs_timeout.tv_sec += abs_timeout.tv_nsec / 1'000'000'000;
        abs_timeout.tv_nsec = abs_timeout.tv_nsec % 1'000'000'000;

        const int rv_lock = pthread_mutex_timedlock(&mutex, &abs_timeout);
        if (rv_lock == ETIMEDOUT) {
#ifdef DELAY
            fprintf(stderr, "[ main ][%d] pthread_mutex_timedlock: ETIMEDOUT\n", i);
#endif
            dropped_events++;
            i++;
            continue;
        }
        if (rv_lock != 0) {
            perror("pthread_mutex_lock");
        }
#ifdef DELAY
        fprintf(stderr, "[ main ][%d] locked mutex\n", i);
#endif

        while (state != STATE_READY) {
            const int rv_cond = pthread_cond_timedwait(&cond, &mutex, &abs_timeout);
            if (rv_cond == ETIMEDOUT) {
#ifdef DELAY
                fprintf(stderr, "[ main ][%d] pthread_cond_timedwait: ETIMEDOUT\n", i);
#endif
                dropped_events++;
                goto timeout;
            }
            if (rv_cond != 0) {
                perror("pthread_cond_timedwait");
            }
        }

        memcpy(data_copy, data, sizeof(data));
        state = STATE_PROCESSING;

    timeout:
        if (pthread_cond_signal(&cond) != 0) {
            perror("pthread_cond_signal");
        }
        if (pthread_mutex_unlock(&mutex) != 0) {
            perror("pthread_mutex_unlock");
        }
#ifdef DELAY
        fprintf(stderr, "[ main ][%d] unlocked mutex\n", i);
#endif
        i++;
    }

    if (pthread_mutex_lock(&mutex) != 0) {
        perror("pthread_mutex_lock");
    }

    while (state != STATE_READY) {
        if (pthread_cond_wait(&cond, &mutex) != 0) {
            perror("pthread_cond_wait");
        }
    }

    state = STATE_EXIT;

    if (pthread_cond_signal(&cond) != 0) {
        perror("pthread_cond_signal");
    }
    if (pthread_mutex_unlock(&mutex) != 0) {
        perror("pthread_mutex_unlock");
    }
    if (pthread_join(thread, nullptr) != 0) {
        perror("pthread_join");
    }

    struct timespec end;
    clock_gettime(CLOCK_REALTIME, &end);

    struct timespec elapsed;
    elapsed.tv_sec = end.tv_sec - start.tv_sec;
    elapsed.tv_nsec = end.tv_nsec - start.tv_nsec;
    if (elapsed.tv_nsec < 0) {
        elapsed.tv_sec--;
        elapsed.tv_nsec += 1000000000;
    }

    printf(
        "{\"scenario\":\"thread send (non-blocking), select\",\"events\":%d,\"size\":%lu,\"timeout_select\":%ld.%06ld,\"timeout_timed\":%ld.%09ld,\"dropped\":%d,\"elapsed\":%ld.%09ld}\n",
        config.events, config.size, config.timeout_select.tv_sec, config.timeout_select.tv_usec,
        config.timeout_timed.tv_sec, config.timeout_timed.tv_nsec, dropped_events, elapsed.tv_sec, elapsed.tv_nsec);

    return EXIT_SUCCESS;
}
