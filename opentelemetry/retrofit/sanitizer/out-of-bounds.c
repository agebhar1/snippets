// https://clang.llvm.org/docs/UndefinedBehaviorSanitizer.html
// https://blogs.oracle.com/linux/improving-application-security-with-undefinedbehaviorsanitizer-ubsan-and-gcc
// https://github.com/llvm/llvm-project/tree/main/compiler-rt/test/ubsan

int main() {
    int my_array[10] = {0};
    return my_array[-1];
}

// out-of-bounds.c:3:20: runtime error: index -1 out of bounds for type 'int [10]'
