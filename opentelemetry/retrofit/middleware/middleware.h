#ifndef MIDDLEWARE_H
#define MIDDLEWARE_H

extern unsigned char middleware_buffer[1024 * 1024];
extern size_t middleware_buffer_size;

typedef enum { READ, WRITE } OpCode;

typedef struct {
    OpCode opcode;
    size_t size;
    char queue[8+1];
} MessageHdr;

int middleware_connect();

int middleware_disconnect();

int middleware_read(const char *queue);

int middleware_write(const char *queue, const unsigned char *data, size_t size);

#endif //MIDDLEWARE_H
