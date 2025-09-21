#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <stdint.h>

#define SERVER_IP   "127.0.0.1"  // Change to your server's IP
#define SERVER_PORT 12345        // Change to your server's port

// Helper function to convert uint64_t to network byte order
uint64_t htonll(uint64_t value) {
#if __BYTE_ORDER == __LITTLE_ENDIAN
    return ((uint64_t)htonl(value & 0xFFFFFFFF) << 32) | htonl(value >> 32);
#else
    return value;
#endif
}

int main(int argc, char *argv[]) {
    int sockfd;
    struct sockaddr_in server_addr;
    socklen_t addr_len = sizeof(server_addr);

    if (argc < 2) {
        fprintf(stderr, "Usage: %s <number> [end]\n", argv[0]);
        exit(EXIT_FAILURE);
    }

    // Parse arguments
    char *endptr;
    uint64_t start = strtoull(argv[1], &endptr, 10);
    if (*endptr != '\0') {
        fprintf(stderr, "Invalid uint64_t value: %s\n", argv[1]);
        exit(EXIT_FAILURE);
    }

    uint64_t end = start;
    if (argc >= 3) {
        end = strtoull(argv[2], &endptr, 10);
        if (*endptr != '\0') {
            fprintf(stderr, "Invalid uint64_t end value: %s\n", argv[2]);
            exit(EXIT_FAILURE);
        }
        if (end < start) {
            fprintf(stderr, "End value must be greater than or equal to start value\n");
            exit(EXIT_FAILURE);
        }
    }

    // Create UDP socket
    if ((sockfd = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
        perror("socket");
        exit(EXIT_FAILURE);
    }

    // Server address
    memset(&server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_port = htons(SERVER_PORT);
    if (inet_pton(AF_INET, SERVER_IP, &server_addr.sin_addr) <= 0) {
        perror("inet_pton");
        exit(EXIT_FAILURE);
    }

    // Send each uint64_t in the range [start, end] (or single number if only one)
    for (uint64_t number = start; number <= end; ++number) {
        uint64_t net_number = htonll(number);
        ssize_t sent_bytes = sendto(sockfd, &net_number, sizeof(net_number), 0,
                                    (struct sockaddr *)&server_addr, addr_len);
        if (sent_bytes < 0) {
            perror("sendto");
            exit(EXIT_FAILURE);
        }
        printf("Sent uint64_t (%llu) in network order to %s:%d\n",
               (unsigned long long)number, SERVER_IP, SERVER_PORT);
    }

    close(sockfd);
    return 0;
}