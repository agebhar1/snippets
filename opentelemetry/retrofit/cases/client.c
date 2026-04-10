#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>

int main() {
    const int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (fd == -1) {
        perror("socket");
        exit(EXIT_FAILURE);
    }

    // https://man7.org/linux/man-pages/man7/socket.7.html
    int sndbuf;
    socklen_t len = sizeof(sndbuf);
    getsockopt(fd, SOL_SOCKET, SO_SNDBUF, &sndbuf, &len);
    printf("Socket send buffer size (SO_SNDBUF): %d\n", sndbuf / 2);

    struct sockaddr_un addr = {.sun_family = AF_LOCAL};
    strcpy(addr.sun_path, "main.sock");

    if (connect(fd, (struct sockaddr *) &addr, sizeof(addr)) == -1) {
        close(fd);
        perror("connect");
        exit(EXIT_FAILURE);
    }

    const struct timespec duration = {.tv_sec = 0, .tv_nsec = 100000};

    unsigned int i = 0;
    unsigned int j = 0;
    unsigned char buffer[64] = {0};
    while (true) {
        // https://man7.org/linux/man-pages/man2/sendmsg.2.html
        bool done = false;
        while (!done) {
            const ssize_t sent = send(fd, buffer, sizeof(buffer), MSG_DONTWAIT | MSG_NOSIGNAL);
            if (sent == -1) {
                if (errno == EAGAIN || errno == EWOULDBLOCK) {
                    printf("[%s] sent %ld bytes (i: %d, delta: %d)\n",
                           errno == EAGAIN ? "EAGAIN" : "EWOULDBLOCK", i * sizeof(buffer), i, i - j);
                    j = i;
                    nanosleep(&duration, nullptr);
                    continue;
                }
                perror("send");
                exit(EXIT_FAILURE);
            }
            done = true;
        }
        i++;
        // if (i % 1024 == 0) {
        //     printf("sent %ld bytes\n", sizeof(buffer) * i);
        // }
    }

    close(fd);
    return EXIT_SUCCESS;
}
