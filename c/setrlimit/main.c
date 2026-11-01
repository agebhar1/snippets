#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/resource.h>

int main(int argc, char *argv[]) {
    if (argc < 2) {
        fprintf(stderr, "Usage: %s <malloc_size_in_kbytes> [<limit_size_in_kbytes>]\n", argv[0]);
        return 1;
    }

    struct rlimit rlp;

    if (argc == 3) {
        const size_t limit_size = 1024 * strtoull(argv[2], NULL, 10);
        rlp.rlim_cur = limit_size;
        rlp.rlim_max = limit_size;
        printf("set virtual memory limit: soft: %lu, hard: %lu\n", rlp.rlim_cur, rlp.rlim_max);
        if (setrlimit(RLIMIT_AS, &rlp) == -1) {
            perror("setrlimit");
            return 1;
        }
    }

    memset(&rlp, 0, sizeof(rlp));
    if (getrlimit(RLIMIT_AS, &rlp) == -1) {
        perror("getrlimit");
        return 1;
    }
    printf("virtual memory limit: soft: %lu, hard: %lu (RLIM_INFINITY=%lu)\n",
           rlp.rlim_cur, rlp.rlim_max, RLIM_INFINITY);

    const size_t malloc_size = 1024 * strtoull(argv[1], NULL, 10);

    char *ptr = malloc(malloc_size);
    if (ptr == NULL) {
        perror("malloc");
        return 1;
    }
    memset(ptr, 0, malloc_size);

    printf("allocated %zu bytes at %p\n", malloc_size, (void *) ptr);
    printf("press enter to continue and free the memory...\n");

    getchar(); // wait for user input

    free(ptr);
    printf("memory freed\n");

    return 0;
}
