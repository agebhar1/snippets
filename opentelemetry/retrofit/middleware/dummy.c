#include <stddef.h>

#include "opentelemetry.h"

int opentelemetry_connect(void) { return 0; }

int opentelemetry_disconnect(void) { return 0; }

int opentelemetry_read(
    __attribute__((unused)) const char *queue,
    __attribute__((unused)) const unsigned char *buffer,
    __attribute__((unused)) size_t size) { return 0; }

int opentelemetry_write(
    __attribute__((unused)) const char *queue,
    __attribute__((unused)) const unsigned char *buffer,
    __attribute__((unused)) size_t size) { return 0; }
