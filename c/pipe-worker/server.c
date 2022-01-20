#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <strings.h>
#include <unistd.h>

#define R 0
#define W 1

int main()
{
    char buffer[1024];

    int pipe_stdout[2];
    int pipe_stdin[2];
    int pipe_stderr[2];

    if (pipe(pipe_stdout) == -1 || pipe(pipe_stdin) == -1 || pipe(pipe_stderr) == -1)
    {
        perror("pipe");
        exit(errno);
    }

    int pid = fork();

    if (pid < 0)
    {
        perror("fork");
        exit(errno);
    }

    if (pid == 0)
    {
        close(pipe_stdout[R]);
        close(pipe_stdin[W]);
        close(pipe_stderr[R]);

        if (dup2(pipe_stdout[W], STDOUT_FILENO) == -1)
        {
            perror("dup2(pipe_stdout[W], STDOUT_FILENO)");
            exit(errno);
        }
        if (dup2(pipe_stdin[R], STDIN_FILENO) == -1)
        {
            perror("dup2(pipe_stdin[R], STDIN_FILENO)");
            exit(errno);
        }
        if (dup2(pipe_stderr[W], STDERR_FILENO) == -1)
        {
            perror("dup2(pipe_stderr[W], STDERR_FILENO)");
            exit(errno);
        }

        char *argv[] = {"worker", NULL};
        if (execv("./worker", argv) == -1)
        {
            perror("execv");
            exit(errno);
        }
    }

    close(pipe_stdout[W]);
    close(pipe_stdin[R]);
    close(pipe_stderr[W]);

    ssize_t count;
    while ((count = read(STDIN_FILENO, buffer, sizeof(buffer))) > 0)
    {
        write(pipe_stdin[W], buffer, count);

        bzero(buffer, sizeof(buffer));

        count = read(pipe_stdout[R], buffer, sizeof(buffer));

        write(STDOUT_FILENO, buffer, count);
    }

    close(pipe_stdout[R]);
    close(pipe_stdin[W]);
    close(pipe_stderr[R]);

    return 0;
}