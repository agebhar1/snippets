#include <errno.h>
#include <pthread.h>
#include <signal.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t cond = PTHREAD_COND_INITIALIZER;

typedef enum { STATE_READY, STATE_PROCESSING, STATE_EXIT } state_t;

static constexpr char state_t_str[][11] = {"READY", "PROCESSING", "EXIT"};

static state_t state = STATE_READY;
static uint64_t data = 0;

static volatile sig_atomic_t running = 1;

static void sig_handler([[maybe_unused]] const int sig) {
#ifndef NDEBUG
    printf("[      ] received signal: %s\n", strsignal(sig));
#endif
    running = 0;
}

static void *threadFunc([[maybe_unused]] void *arg) {
    while (true) {
        // wait for processing or exit notification
        pthread_mutex_lock(&mutex);
        while (state != STATE_PROCESSING && state != STATE_EXIT) {
#ifndef NDEBUG
            printf("[thread] call pthread_cond_wait(...)\n");
#endif
            if (pthread_cond_wait(&cond, &mutex) != 0) {
                perror("pthread_cond_wait");
            }
        }

        if (state == STATE_EXIT) {
            printf("[thread] shutdown\n");
            break;
        }

        if (pthread_mutex_unlock(&mutex) != 0) {
            perror("pthread_mutex_unlock");
        }

        // process worker data
        printf("[thread] state: %s, data: %ld\n", state_t_str[state], data);
        sleep(1);

        if (pthread_mutex_lock(&mutex) != 0) {
            perror("pthread_mutex_lock");
        }

        state = STATE_READY;
        printf("[thread] state: %s\n", state_t_str[state]);

        if (pthread_cond_signal(&cond) != 0) {
            perror("pthread_cond_signal");
        }
        if (pthread_mutex_unlock(&mutex) != 0) {
            perror("pthread_mutex_unlock");
        }
    }
    if (pthread_mutex_unlock(&mutex) != 0) {
        perror("pthread_mutex_unlock");
    }
    return nullptr;
}

int main([[maybe_unused]] const int argc, [[maybe_unused]] char *argv[]) {
    signal(SIGINT, sig_handler);

    pthread_t thread;
    if (pthread_create(&thread, nullptr, threadFunc, nullptr) != 0) {
        perror("pthread_create");
        exit(EXIT_FAILURE);
    }

    uint64_t iteration = 0;
    while (running) {
        iteration++;

        struct timespec abs_timeout;
        clock_gettime(CLOCK_REALTIME, &abs_timeout);

        abs_timeout.tv_nsec += 125'000'000;
        abs_timeout.tv_sec += abs_timeout.tv_nsec / 1'000'000'000;
        abs_timeout.tv_nsec = abs_timeout.tv_nsec % 1'000'000'000;

        const int rv_timedlock = pthread_mutex_timedlock(&mutex, &abs_timeout);
        if (rv_timedlock == ETIMEDOUT) {
#ifndef NDEBUG
            fprintf(stderr, "[ main ] pthread_mutex_timedlock: ETIMEDOUT\n");
#endif
            continue;
        }
        while (state != STATE_READY) {
#ifndef NDEBUG
            printf("[ main ] call pthread_cond_timedwait(...)\n");
#endif
            const int rv_timedwait = pthread_cond_timedwait(&cond, &mutex, &abs_timeout);
            if (rv_timedwait == ETIMEDOUT) {
#ifndef NDEBUG
                fprintf(stderr, "[ main ] pthread_cond_timedwait: ETIMEDOUT\n");
#endif
                goto timeout;
            }
        }
        printf("[ main ] state: %s, processed data: %ld\n", state_t_str[state], data);

        // prepare worker data
        data = iteration;

        // 'notify' worker
        state = STATE_PROCESSING;
        printf("[ main ] state: %s, data: %ld\n", state_t_str[state], data);
        if (pthread_cond_signal(&cond) != 0) {
            perror("pthread_cond_signal");
        }
    timeout:
        if (pthread_mutex_unlock(&mutex) != 0) {
            perror("pthread_mutex_unlock");
        }
    }

    if (pthread_mutex_lock(&mutex) != 0) {
        perror("pthread_mutex_lock");
    };
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
}
