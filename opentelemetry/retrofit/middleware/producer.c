#include <getopt.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "middleware.h"

typedef struct {
    char **w;
    unsigned int events;
    unsigned int len;
} Configuration;

Configuration get_config(const int argc, char *argv[]) {
    Configuration config = {
        .events = 1'000,
        .w = nullptr,
        .len = 0
    };

    int opt;
    bool error = false;
    while ((opt = getopt(argc, argv, "e:w:")) != -1) {
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
            case 'w':
                config.w = realloc(config.w, (config.len + 1) * sizeof(char *));
                config.w[config.len] = strdup(optarg);
                config.len++;
                break;
            default:
                fprintf(stderr, "Usage: %s [-w queue]+\n", argv[0]);
                exit(EXIT_FAILURE);
        }
    }


    if (config.w != nullptr) {
        if (config.w != nullptr) {
            printf(" -> ");
            for (unsigned int i = 0; i < (config.len - 1); i++) {
                printf("%s | ", config.w[i]);
            }
            printf("%s", config.w[config.len - 1]);
        }
        printf("\n\n");
    }
    if (error) {
        exit(EXIT_FAILURE);
    }

    return config;
}

void free_config(Configuration *config) {
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
    if (config.w == nullptr) {
        goto done;
    }

    if (middleware_connect() == -1) {
        printf("failed to connect to middleware\n");
        goto done;
    }

    for (uint16_t i = 0; i < config.events; i++) {
        memset(middleware_buffer, 0, middleware_buffer_size);
        sprintf((char *) middleware_buffer, "%d", i);

        // write
        for (unsigned short j = 0; j < config.len; j++) {
            middleware_buffer_size = strlen((const char *) middleware_buffer);
            if (middleware_write(config.w[j], middleware_buffer, middleware_buffer_size) == -1) {
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
