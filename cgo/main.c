#include <string.h>
#include <stdio.h>

#include "libtinker.h"

int main(int argc, char *argv[])
{
    GoRun();

    uintptr_t h1 = NewMyStruct();
    uintptr_t h2 = NewMyStruct();

    MyStructFunction(h1);
    MyStructFunction(h2);
    MyStructFunction(h1);
    MyStructFunction(h2);

    for (int i = 1; i < argc; i++)
    {
        GoString gstr = {.p = argv[i], .n = strlen(argv[i])};
        printf("IsBlank(%s): %d\n", argv[i], IsBlank(gstr));
    }

    // uintptr_t handles[] = {h1, h2};
    // GoSlice hs = {.cap = sizeof handles, .data = handles, .len = sizeof handles};
    // DeleteHandles(hs);
    DeleteHandle(h1);
    DeleteHandle(h2);

    char c;
    printf("Hit enter to close ...\n");
    scanf("%c", &c);
    
    return 0;
}