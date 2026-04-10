#include <errno.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t cond = PTHREAD_COND_INITIALIZER;

typedef enum { STATE_READY, STATE_PROCESSING, STATE_EXIT } state_t;

constexpr char state_t_str[][11] = {"READY", "PROCESSING", "EXIT"};

state_t state = STATE_READY;

static void *threadFunc([[maybe_unused]] void *arg) {
    while (1) {
        (void) pthread_mutex_lock(&mutex);
        while (state == STATE_READY) {
            // printf("[thread] pthread_cond_wait\n");
            (void) pthread_cond_wait(&cond, &mutex);
        }
        (void) pthread_mutex_unlock(&mutex);

        sleep(1);

        (void) pthread_mutex_lock(&mutex);
        state = STATE_READY;
        printf("[thread] state: %s\n", state_t_str[state]);
        (void) pthread_cond_signal(&cond);
        (void) pthread_mutex_unlock(&mutex);
    }
    return nullptr;
}

int main([[maybe_unused]] const int argc, [[maybe_unused]] char *argv[]) {
    pthread_t thread;
    if (pthread_create(&thread, nullptr, threadFunc, nullptr) != 0) {
        perror("pthread_create");
        exit(EXIT_FAILURE);
    }

    while (1) {
        struct timespec abs_timeout;
        clock_gettime(CLOCK_REALTIME, &abs_timeout);

        // abs_timeout.tv_sec += 1;
        abs_timeout.tv_nsec += 125'000'000;
        abs_timeout.tv_sec += abs_timeout.tv_nsec / 1'000'000'000;
        abs_timeout.tv_nsec = abs_timeout.tv_nsec % 1'000'000'000;

        const int rv_timedlock = pthread_mutex_timedlock(&mutex, &abs_timeout);
        if (rv_timedlock == ETIMEDOUT) {
            fprintf(stderr, "[ main ] pthread_mutex_timedlock: ETIMEDOUT\n");
            continue;
        }
        while (state == STATE_PROCESSING) {
            // printf("[ main ] pthread_cond_timedwait\n");
            const int rv_timedwait = pthread_cond_timedwait(&cond, &mutex, &abs_timeout);
            if (rv_timedwait == ETIMEDOUT) {
                fprintf(stderr, "[ main ] pthread_cond_timedwait: ETIMEDOUT\n");
                goto timeout;
            }
        }
        state = STATE_PROCESSING;
        printf("[ main ] state: %s\n", state_t_str[state]);
    timeout:
        (void) pthread_cond_signal(&cond);
        (void) pthread_mutex_unlock(&mutex);
    }
}
