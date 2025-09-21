#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <stdint.h>

#define SERVER_PORT 12345        // Change as needed

// Helper function to convert uint64_t from network byte order
uint64_t ntohll(uint64_t value) {
#if __BYTE_ORDER == __LITTLE_ENDIAN
    return ((uint64_t)ntohl(value & 0xFFFFFFFF) << 32) | ntohl(value >> 32);
#else
    return value;
#endif
}

int main() {
    int sockfd;
    struct sockaddr_in server_addr, client_addr;
    uint64_t buffer;
    socklen_t addr_len = sizeof(client_addr);

    // Create UDP socket
    if ((sockfd = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
        perror("socket");
        exit(EXIT_FAILURE);
    }

    // Bind address
    memset(&server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = INADDR_ANY;
    server_addr.sin_port = htons(SERVER_PORT);

    if (bind(sockfd, (struct sockaddr *)&server_addr, sizeof(server_addr)) < 0) {
        perror("bind");
        close(sockfd);
        exit(EXIT_FAILURE);
    }

    printf("UDP server listening on port %d...\n", SERVER_PORT);

    while (1) {
        ssize_t recv_bytes = recvfrom(sockfd, &buffer, sizeof(buffer), 0,
                                      (struct sockaddr *)&client_addr, &addr_len);
        if (recv_bytes < 0) {
            perror("recvfrom");
            continue;
        }
        if (recv_bytes != sizeof(uint64_t)) {
            fprintf(stderr, "Received unexpected size: %zd bytes\n", recv_bytes);
            continue;
        }

        uint64_t host_number = ntohll(buffer);

        printf("Received uint64_t (%llu) from %s:%d\n",
               (unsigned long long)host_number,
               inet_ntoa(client_addr.sin_addr),
               ntohs(client_addr.sin_port));
    }

    // Never reached in this example
    close(sockfd);
    return 0;
}