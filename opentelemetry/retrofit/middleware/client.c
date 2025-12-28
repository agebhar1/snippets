#include <getopt.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <tgmath.h>
#include <time.h>
#include <unistd.h>

#include "middleware.h"

typedef struct {
    char *r;
    char **w;
    struct timespec duration;
    unsigned int len;
} Configuration;

Configuration get_config(const int argc, char *argv[]) {
    Configuration config = {
        .r = nullptr,
        .w = nullptr,
        .duration = {
            .tv_sec = 0,
            .tv_nsec = 0
        },
        .len = 0
    };

    int opt;
    while ((opt = getopt(argc, argv, "r:w:d:")) != -1) {
        switch (opt) {
            case 'r':
                if (config.r == nullptr) {
                    config.r = strdup(optarg);
                } else {
                    fprintf(stderr, "WARNING: option 'r' already set by '%s', '%s' ignored\n", config.r, optarg);
                }
                break;
            case 'w':
                config.w = realloc(config.w, (config.len + 1) * sizeof(char *));
                config.w[config.len] = strdup(optarg);
                config.len++;
                break;
            case 'd':
                double duration = atof(optarg);
                if (duration < 0.0) {
                    fprintf(stderr, "WARNING: option 'd'(uration) must be >= 0.0\n");
                    break;
                }
                config.duration.tv_sec = (time_t) duration;
                config.duration.tv_nsec = (long) ((duration - floor(duration)) * 1e9);
                break;
            default:
                fprintf(stderr, "Usage: %s [-r queue] [-w queue]+ [-s duration]\n", argv[0]);
                exit(EXIT_FAILURE);
        }
    }


    if (config.r != nullptr || config.w != nullptr) {
        printf("%s", config.r == nullptr ? "(stdin)" : config.r);
        if (config.w != nullptr) {
            printf(" -> ");
            for (unsigned int i = 0; i < (config.len - 1); i++) {
                printf("%s | ", config.w[i]);
            }
            printf("%s", config.w[config.len - 1]);
        }
        printf("\n\n");
    }

    return config;
}

void free_config(Configuration *config) {
    if (config->r != nullptr) {
        free(config->r);
        config->r = nullptr;
    }
    if (config->w != nullptr) {
        for (unsigned short i = 0; i < config->len; i++) {
            free(config->w[i]);
        }
        free(config->w);
        config->w = nullptr;
    }
}

int main(int argc, char *argv[]) {
    Configuration config = get_config(argc, argv);
    if (config.r == nullptr && config.w == nullptr) {
        goto done;
    }

    if (middleware_connect() == -1) {
        printf("failed to connect to middleware\n");
        goto done;
    }

    while (1) {
        // read
        if (config.r == nullptr) {
            const ssize_t size = read(STDIN_FILENO, middleware_buffer, sizeof(middleware_buffer));
            if (size == -1 || size == 0) {
                break;
            }
            middleware_buffer_size = size - 1;
        } else {
            if (middleware_read(config.r) == -1) {
                printf("failed to read from middleware\n");
                break;
            }
            printf("read from queue '%s' => '%s'\n", config.r, (const char *) middleware_buffer);
        }

        // business process
        if (config.r != nullptr) {
            printf("[ processing ... (%ld.%09lds) ]\n", config.duration.tv_sec, config.duration.tv_nsec);
            nanosleep(&config.duration, nullptr);
        }

        // write
        for (unsigned short i = 0; i < config.len; i++) {
            if (middleware_write(config.w[i], middleware_buffer, middleware_buffer_size) == -1) {
                printf("failed to write to middleware\n");
                break;
            }
        }
    }

    middleware_disconnect();

done:
    free_config(&config);

    return EXIT_SUCCESS;
}
