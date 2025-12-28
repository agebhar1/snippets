#include <sys/socket.h>
#include <sys/un.h>
#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

#include <openssl/md5.h>

struct uid {
    struct timespec ts;
    uint8_t key[16];
};

struct request {
    uint8_t cmd;
    struct uid read;
    struct uid write;
};

int socket_connect() {
    const int fd = socket(AF_LOCAL, SOCK_STREAM, 0);
    if (fd == -1) {
        perror("socket");
        exit(1);
    }

    struct sockaddr_un addr = {.sun_family = AF_LOCAL};
    strcpy(addr.sun_path, "/tmp/echo.sock");

    if (connect(fd, (struct sockaddr *) &addr, sizeof(addr))) {
        perror("connect");
        exit(1);
    }
    return fd;
}

int main(/*int argc, char **argv*/) {
    printf("sizeof(struct request): %"PRId64"\n", sizeof(struct request));
    fflush(stdout);

    int fd = socket_connect();

    printf("connected\n");
    unsigned char buf[BUFSIZ];

    while (1) {
        const ssize_t n = read(STDIN_FILENO, buf, sizeof(buf));
        if (n == 0) {
            break;
        }

        struct request req = {.cmd = 1, .read = {{0}}, .write = {{0}}};
        MD5(buf, n, req.read.key);
        clock_gettime(CLOCK_REALTIME, &req.read.ts);

        struct timespec duration = {.tv_sec = 1, .tv_nsec = 0}; // simulate processing
        nanosleep(&duration, NULL);

        MD5(buf, n, req.write.key);
        clock_gettime(CLOCK_REALTIME, &req.write.ts);

        while (send(fd, &req, sizeof(req), MSG_NOSIGNAL) == -1) {
            fd = socket_connect();
            if (fd == -1) {
                goto end;
            }
        }
    }

end:
    printf("closing\n");
    close(fd);

    return 0;
}
