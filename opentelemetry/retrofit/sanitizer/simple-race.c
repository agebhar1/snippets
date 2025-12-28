// https://github.com/google/sanitizers/wiki/ThreadSanitizerCppManual
// https://github.com/llvm/llvm-project/tree/main/compiler-rt/test/tsan
#include <pthread.h>
#include <stdio.h>
#ifdef ATOMIC
#include <stdatomic.h>

atomic_int Global;
#else
static int Global;
#endif

static void *Thread1(void *) {
#ifdef ATOMIC
    atomic_fetch_add(&Global, 1);
#else
    Global++;
#endif
    return nullptr;
}

static void *Thread2(void *) {
#ifdef ATOMIC
    atomic_fetch_sub(&Global, 1);
#else
    Global--;
#endif
    return nullptr;
}

int main() {
    pthread_t t[2];
    pthread_create(&t[0], NULL, Thread1, nullptr);
    pthread_create(&t[1], NULL, Thread2, nullptr);
    pthread_join(t[0], NULL);
    pthread_join(t[1], NULL);
    return 0;
}

// ==================
// WARNING: ThreadSanitizer: data race (pid=31)
//   Read of size 4 at 0x5634886fd070 by thread T2:
//     #0 Thread2 /mnt/sanitizer/simple_race.c:12 (simple_race+0x1306) (BuildId: bb4d79612902d2b65c278dda166f6b34ecd180c2)
//     #1 <null> <null> (libtsan.so.2+0x3de5e) (BuildId: 48e2d2ccd79ae93ce659545762d78d542d72ba21)
//
//   Previous write of size 4 at 0x5634886fd070 by thread T1:
//     #0 Thread1 /mnt/sanitizer/simple_race.c:7 (simple_race+0x12db) (BuildId: bb4d79612902d2b65c278dda166f6b34ecd180c2)
//     #1 <null> <null> (libtsan.so.2+0x3de5e) (BuildId: 48e2d2ccd79ae93ce659545762d78d542d72ba21)
//
//   Location is global 'Global' of size 4 at 0x5634886fd070 (simple_race+0x4070)
//
//   Thread T2 (tid=34, running) created by main thread at:
//     #0 pthread_create <null> (libtsan.so.2+0x47f2d) (BuildId: 48e2d2ccd79ae93ce659545762d78d542d72ba21)
//     #1 main /mnt/sanitizer/simple_race.c:19 (simple_race+0x1105) (BuildId: bb4d79612902d2b65c278dda166f6b34ecd180c2)
//
//   Thread T1 (tid=33, finished) created by main thread at:
//     #0 pthread_create <null> (libtsan.so.2+0x47f2d) (BuildId: 48e2d2ccd79ae93ce659545762d78d542d72ba21)
//     #1 main /mnt/sanitizer/simple_race.c:18 (simple_race+0x10f0) (BuildId: bb4d79612902d2b65c278dda166f6b34ecd180c2)
//
// SUMMARY: ThreadSanitizer: data race /mnt/sanitizer/simple_race.c:12 in Thread2
// ==================
// ThreadSanitizer: reported 1 warnings
