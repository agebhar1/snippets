#include <errno.h>
#include <fcntl.h>
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

typedef struct {
    unsigned int events;
    int flags;
    size_t size;
    struct timespec timeout;
} Configuration;

Configuration get_config(const int argc, char *argv[]) {
    Configuration config = {
        .events = 1'000,
        .flags = MSG_NOSIGNAL | MSG_DONTWAIT,
        .size = 72,
        .timeout = {
            .tv_sec = 0,
            .tv_nsec = 100'000'000
        }
    };

    int opt;
    bool error = false;
    while ((opt = getopt(argc, argv, "d:e:r:s:w")) != -1) {
        switch (opt) {
            case 'd':
                const double duration = atof(optarg);
                if (duration < 0.0) {
                    fprintf(stderr, "ERROR: option 'd' (duration) must be >= 0.0\n");
                    error = true;
                    break;
                }
                config.timeout.tv_sec = (time_t) duration;
                config.timeout.tv_nsec = (long) ((duration - floor(duration)) * 1e9);
                break;
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
            case 'w':
                config.flags ^= MSG_DONTWAIT;
                break;
            default:
                fprintf(
                    stderr,
                    "Usage: %s [-e #events (default 1000)] [-d duration (default 0.1)] [-w]\n",
                    argv[0]);
                error = true;
        }
    }
    if (error) {
        exit(EXIT_FAILURE);
    }

    return config;
}

int main(int argc, char *argv[]) {
    const Configuration config = get_config(argc, argv);

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

    unsigned int i = 0;
    unsigned char buffer[config.size] = {};
#ifdef DIGEST
    constexpr unsigned char data[1024 * 1024] = {};
#endif

    struct timespec start;
    clock_gettime(CLOCK_REALTIME, &start);

    bool error = false;
    while (!error && i < config.events) {
        const unsigned char *buffer_ptr = buffer;
        unsigned long remaining = sizeof(buffer);

#ifdef DIGEST
        SHA256(data, sizeof(data), buffer);
#endif
        while (!error && remaining > 0) {
            const ssize_t sent = send(fd, buffer_ptr, remaining, config.flags);
            if (sent != -1) {
                remaining -= sent;
                buffer_ptr += sent;
            } else {
#ifdef DEBUG
                fprintf(stderr, "%d event(s), sent %ld bytes [%s]\n", i, sizeof(buffer) * i, strerror(errno));
#endif
                error = errno != EAGAIN && errno != EWOULDBLOCK;
                if (!error) {
                    struct timespec duration = config.timeout;
                    nanosleep(&duration, nullptr);
                }
            }
#ifdef DEBUG
            fprintf(stderr, "%d event(s), sent %ld bytes, remaining %ld bytes\n", i, sizeof(buffer) * i, remaining);
#endif
        }
        i++;
#ifdef DEBUG
        if (i % 1024 == 0) {
            printf("%d event(s), sent %ld bytes\n", i, sizeof(buffer) * i);
            fflush(stdout);
        }
#endif
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

    const bool is_msg_dontwait = (config.flags & MSG_DONTWAIT) == MSG_DONTWAIT;
    printf(
        "{\"scenario\":\"send %s\",\"events\":%d,\"size\":%lu,\"duration\":%ld.%09ld,\"elapsed\":%ld.%09ld}\n",
        is_msg_dontwait ? "(non-blocking), nanosleep" : "(blocking)", config.events, config.size, config.timeout.tv_sec,
        config.timeout.tv_nsec, elapsed.tv_sec, elapsed.tv_nsec);

    close(fd);
    return EXIT_SUCCESS;
}
