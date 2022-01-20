#include <fcntl.h>
#include <errno.h>
#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

char buffer[1024];

int main()
{
    pid_t pid = getpid();

    char fn[128] = {0};
    sprintf(fn, "worker.%" PRIdMAX ".log", (intmax_t)pid);

    int fd = open(fn, O_CREAT | O_WRONLY, S_IRUSR | S_IWUSR);
    if (fd == -1)
    {
        perror("open");
        exit(errno);
    }

    ssize_t count;
    while ((count = read(STDIN_FILENO, buffer, sizeof(buffer))) > 0)
    {
        write(STDOUT_FILENO, buffer, count);
        write(fd, buffer, count);
    }

    write(fd, "\n-- exit --\n", 12);
    close(fd);

    return 0;
}