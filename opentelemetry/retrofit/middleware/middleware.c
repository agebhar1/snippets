#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>

#include "middleware.h"
#include "opentelemetry.h"

unsigned char middleware_buffer[1024 * 1024] = {0};
size_t middleware_buffer_size = 0;

static int middleware_fd = -1;

int middleware_connect() {
    middleware_fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (middleware_fd == -1) {
        return -1;
    }

    struct sockaddr_un addr = {.sun_family = AF_LOCAL};
    strcpy(addr.sun_path, "/tmp/middleware.sock");

    if (connect(middleware_fd, (struct sockaddr *) &addr, sizeof(addr)) == -1) {
        close(middleware_fd);
        return -1;
    }

    const pid_t pid = getpid();
    if (write(middleware_fd, &pid, sizeof(pid)) != sizeof(pid)) {
        close(middleware_fd);
        return -1;
    }

    (void) opentelemetry_connect();

    return 0;
}

int middleware_disconnect() {
    (void) opentelemetry_disconnect();

    if (middleware_fd == -1) {
        return -1;
    }
    const int r = close(middleware_fd);
    middleware_fd = -1;
    return r;
}

int middleware_read(const char *queue) {
    MessageHdr hdr = {.opcode = READ, .size = 0};
    strncpy(hdr.queue, queue, sizeof(hdr.queue) - 1);
    hdr.queue[sizeof(hdr.queue) - 1] = '\0';

    if (write(middleware_fd, &hdr, sizeof(hdr)) != sizeof(hdr)) {
        return -1;
    }

    if (read(middleware_fd, &middleware_buffer_size, sizeof(middleware_buffer_size)) != sizeof(
            middleware_buffer_size)) {
        return -1;
    }

    memset(middleware_buffer, 0, sizeof(middleware_buffer));
    if (middleware_buffer_size > 0) {
        if (read(middleware_fd, &middleware_buffer, middleware_buffer_size) != (ssize_t) middleware_buffer_size) {
            return -1;
        }
    }

    (void) opentelemetry_read(queue, middleware_buffer, middleware_buffer_size);

    return 0;
}

int middleware_write(const char *queue, const unsigned char *data, size_t size) {
    MessageHdr hdr = {.opcode = WRITE, .size = size};
    strncpy(hdr.queue, queue, sizeof(hdr.queue) - 1);
    hdr.queue[sizeof(hdr.queue) - 1] = '\0';

    if (write(middleware_fd, &hdr, sizeof(hdr)) != sizeof(hdr)) {
        return -1;
    }

    if (hdr.size > 0) {
        if (write(middleware_fd, data, size) != (ssize_t) size) {
            return -1;
        }
    }

    (void) opentelemetry_write(queue, data, size);

    return 0;
}
