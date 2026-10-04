// https://github.com/google/sanitizers/wiki/AddressSanitizerLeakSanitizer
// https://github.com/llvm/llvm-project/tree/main/compiler-rt/test/asan/TestCases
#include <stdlib.h>

void *p;

int main() {
    p = malloc(42);
    p = nullptr;
    return 0;
}

// =================================================================
// ==89==ERROR: LeakSanitizer: detected memory leaks
//
// Direct leak of 42 byte(s) in 1 object(s) allocated from:
//     #0 0x7faad04eda1d in malloc (/usr/lib/libasan.so.8+0xeda1d) (BuildId: 49cdec61ea4a7e68d1b3321b11e733d1f2124ac9)
//     #1 0x5649085891d2 in main /mnt/sanitizer/memory-leak.c:7
//     #2 0x7faad0c3b193  (/lib/ld-musl-x86_64.so.1+0x42193) (BuildId: 2b26dfbb1a8172e32ed88052349fd6c997e6aa79)
//
// SUMMARY: AddressSanitizer: 42 byte(s) leaked in 1 allocation(s).
