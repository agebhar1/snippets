#ifndef OPENTELEMETRY_H
#define OPENTELEMETRY_H

#include <stdint.h>
#include <time.h>

typedef enum { CLIENT_READ, CLIENT_WRITE } ClientOpCode;

typedef struct {
    ClientOpCode opcode; // size 4, offset 0
    char queue[8 + 1]; // size 9 (+3 padding), offset 4
    struct timespec ts; // size 16, offset 16
    uint8_t key[32]; // size 32, offset 32
} OpenTelemetryEvent;

int opentelemetry_connect(void);

int opentelemetry_disconnect(void);

int opentelemetry_read(const char *queue, const unsigned char *buffer, size_t size);

int opentelemetry_write(const char *queue, const unsigned char *buffer, size_t size);

#endif //OPENTELEMETRY_H
