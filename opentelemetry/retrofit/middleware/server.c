#include <fcntl.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/un.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "middleware.h"

static constexpr unsigned short MAX_CLIENTS = 10;

static volatile sig_atomic_t running = 1;

static void sig_handler(__attribute__((unused)) const int sig) {
    printf("received signal %d\n", sig);
    running = 0;
}

typedef struct {
    int fd;
    pid_t pid;
    char wait[8 + 1];
} Client;

typedef struct {
    unsigned short size;
    Client clients[MAX_CLIENTS];
} Connections;

void client_reset(Client *client) {
    client->fd = -1;
    client->pid = 0;
    memset(client->wait, 0, sizeof(client->wait));
}

void client_connect(Connections *connections, const int fd) {
    if (fd == -1) return;

    if (connections->size >= MAX_CLIENTS) {
        close(fd);
        return;
    }

    unsigned short i = 0;
    while (i < MAX_CLIENTS && connections->clients[i].fd != -1) { i++; }

    Client *client = &connections->clients[i];
    client->fd = fd;
    memset(client->wait, 0, sizeof(client->wait));

    read(client->fd, &client->pid, sizeof(client->pid));
    connections->size++;
    printf("connected: client(fd: %d, pid: %d) connections: %d\n", fd, client->pid, connections->size);
}

void client_disconnect(Connections *connections, const unsigned short id) {
    Client *client = &connections->clients[id];

    close(client->fd);
    connections->size--;
    printf("disconnected: client(fd: %d, pid: %d) connections: %d\n", client->fd, client->pid, connections->size);
    client_reset(client);
}

void client_read(Client *client, const char *queue) {
    printf("read: client(fd: %d, pid: %d, wait: '%s') queue: '%s'\n", client->fd, client->pid, client->wait, queue);
    middleware_buffer_size = 0;
    memset(middleware_buffer, 0, sizeof(middleware_buffer));

    const int fd = open(queue, O_RDWR | O_EXCL, 0660);
    if (fd == -1) {
        printf("queue: '%s' no data available\n", queue);
        strncpy(client->wait, queue, sizeof(client->wait));
        return;
    }

    size_t position = 0;
    read(fd, &position, sizeof(position));

    struct stat statbuf;
    (void) fstat(fd, &statbuf);

    printf("queue: '%s' open position: %ld, size: %ld\n", queue, position, statbuf.st_size);
    if (statbuf.st_size == (off_t) position) {
        printf("queue: '%s' no data available\n", queue);
        strncpy(client->wait, queue, sizeof(client->wait));
        goto done;
    }

    if (lseek(fd, (off_t) position, SEEK_SET) == -1) goto done;

    if (read(fd, &middleware_buffer_size, sizeof(middleware_buffer_size)) != sizeof(middleware_buffer_size)) goto done;
    if (read(fd, &middleware_buffer, middleware_buffer_size) != (ssize_t) middleware_buffer_size) goto done;

    if (write(client->fd, &middleware_buffer_size, sizeof(middleware_buffer_size)) != sizeof(middleware_buffer_size))
        goto done;
    if (write(client->fd, &middleware_buffer, middleware_buffer_size) != (ssize_t) middleware_buffer_size) goto done;

    if (lseek(fd, 0, SEEK_SET) == -1) goto done;
    position += sizeof(middleware_buffer_size) + middleware_buffer_size;
    printf("queue: '%s' update position: %ld\n", queue, position);
    write(fd, &position, sizeof(position));

done:
    printf("queue: '%s' closed\n", queue);
    close(fd);
}

void client_write(const Client *client, const ssize_t size, const char *queue) {
    printf("write: client(fd: %d, pid: %d, wait: '%s') queue: '%s' (%lu bytes)\n",
           client->fd, client->pid, client->wait, queue, size);
    middleware_buffer_size = 0;
    memset(middleware_buffer, 0, sizeof(middleware_buffer));

    if (size > 0) {
        middleware_buffer_size = size;
        if (read(client->fd, &middleware_buffer, middleware_buffer_size) == -1) return;

        int fd = open(queue, O_APPEND | O_WRONLY | O_EXCL, 0660);
        if (fd == -1) {
            fd = open(queue, O_CREAT | O_WRONLY | O_EXCL, 0660);
            if (fd == -1) {
                perror("open");
                return;
            }
            constexpr size_t position = sizeof(size_t);
            if (write(fd, &position, sizeof(position)) == -1) goto done;

            printf("queue: '%s' created\n", queue);
        }

        if (write(fd, &middleware_buffer_size, sizeof(middleware_buffer_size)) == -1) goto done;
        (void) write(fd, &middleware_buffer, middleware_buffer_size);

    done:
        close(fd);
    }
}

void client_wakeup(Connections *connections, const char *queue) {
    for (unsigned short i = 0; i < MAX_CLIENTS; i++) {
        Client *client = &connections->clients[i];
        if (client->fd != -1) {
            if (strcmp(queue, client->wait) == 0) {
                printf("wakeup: client(fd: %d, pid: %d, wait: '%s')\n", client->fd, client->pid, client->wait);
                client_read(client, queue);
            }
        }
    }
}

void client_dispatch(Connections *connections, const unsigned short id) {
    Client *client = &connections->clients[id];

    MessageHdr hdr;
    switch (read(client->fd, &hdr, sizeof(hdr))) {
        case -1: break; // error
        case 0: client_disconnect(connections, id);
            break;
        default:
            switch (hdr.opcode) {
                case READ:
                    client_read(client, hdr.queue);
                    break;
                case WRITE:
                    client_write(client, hdr.size, hdr.queue);
                    client_wakeup(connections, hdr.queue);
                    break;
            }
    }
}

int main(/*int argc, char *argv[]*/) {
    signal(SIGINT, sig_handler);

    const int server_fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (server_fd == -1) {
        perror("socket");
        exit(EXIT_FAILURE);
    }

    struct sockaddr_un addr = {.sun_family = AF_LOCAL};
    strcpy(addr.sun_path, "/tmp/middleware.sock");
    (void) unlink(addr.sun_path);

    if (bind(server_fd, (struct sockaddr *) &addr, sizeof(addr)) == -1) {
        perror("bind");
        close(server_fd);
        exit(EXIT_FAILURE);
    }

    if (listen(server_fd, MAX_CLIENTS) == -1) {
        perror("listen");
        close(server_fd);
        exit(EXIT_FAILURE);
    }

    printf("listening on %s\n", addr.sun_path);

    Connections connections = {.size = 0};
    for (unsigned short i = 0; i < MAX_CLIENTS; i++) {
        client_reset(&connections.clients[i]);
    }

    fd_set readfds;
    struct timeval timeout;
    while (running) {
        FD_ZERO(&readfds);
        FD_SET(server_fd, &readfds);

        int nfds = server_fd;
        for (unsigned short i = 0; i < MAX_CLIENTS; i++) {
            if (connections.clients[i].fd != -1) {
                FD_SET(connections.clients[i].fd, &readfds);
                if (connections.clients[i].fd > nfds) {
                    nfds = connections.clients[i].fd;
                }
            }
        }

        timeout.tv_usec = 50000;
        const int n = select(nfds + 1, &readfds, NULL, NULL, &timeout);
        if (n > 0) {
            printf("--<< ready >>--\n");
            if (FD_ISSET(server_fd, &readfds)) {
                client_connect(&connections, accept(server_fd, NULL, NULL));
            }
            for (unsigned short i = 0; i < MAX_CLIENTS; i++) {
                if (FD_ISSET(connections.clients[i].fd, &readfds)) {
                    client_dispatch(&connections, i);
                }
            }
        }
    }

    return EXIT_SUCCESS;
}
