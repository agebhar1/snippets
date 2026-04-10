cmake_minimum_required(VERSION 3.28)
project(experiments C)

set(CMAKE_C_STANDARD 23)

# https://github.com/google/sanitizers/wiki/addresssanitizerflags
set(CMAKE_C_FLAGS_ASAN "${CMAKE_C_FLAGS_RELWITHDEBINFO} -fsanitize=address -fno-omit-frame-pointer -fno-common" CACHE STRING "" FORCE)
set(CMAKE_CXX_FLAGS_ASAN "${CMAKE_CXX_FLAGS_RELWITHDEBINFO} -fsanitize=address -fno-omit-frame-pointer -fno-common" CACHE STRING "" FORCE)
set(CMAKE_EXE_LINKER_FLAGS_ASAN "${CMAKE_EXE_LINKER_FLAGS_RELWITHDEBINFO} -fsanitize=address" CACHE STRING "" FORCE)
set(CMAKE_SHARED_LINKER_FLAGS_ASAN "${CMAKE_SHARED_LINKER_FLAGS_RELWITHDEBINFO} -fsanitize=address" CACHE STRING "" FORCE)

# https://github.com/google/sanitizers/wiki/ThreadSanitizerFlags
# sudo sysctl vm.mmap_rnd_bits=28
set(CMAKE_C_FLAGS_TSAN "${CMAKE_C_FLAGS_RELWITHDEBINFO} -fsanitize=thread" CACHE STRING "" FORCE)
set(CMAKE_CXX_FLAGS_TSAN "${CMAKE_CXX_FLAGS_RELWITHDEBINFO} -fsanitize=thread" CACHE STRING "" FORCE)
set(CMAKE_EXE_LINKER_FLAGS_TSAN "${CMAKE_EXE_LINKER_FLAGS_RELWITHDEBINFO} -fsanitize=thread" CACHE STRING "" FORCE)
set(CMAKE_SHARED_LINKER_FLAGS_TSAN "${CMAKE_SHARED_LINKER_FLAGS_RELWITHDEBINFO} -fsanitize=thread" CACHE STRING "" FORCE)

set(CMAKE_C_FLAGS_UBSAN "${CMAKE_C_FLAGS_RELWITHDEBINFO} -fsanitize=undefined" CACHE STRING "" FORCE)
set(CMAKE_CXX_FLAGS_UBSAN "${CMAKE_CXX_FLAGS_RELWITHDEBINFO} -fsanitize=undefined" CACHE STRING "" FORCE)
set(CMAKE_EXE_LINKER_FLAGS_UBSAN "${CMAKE_EXE_LINKER_FLAGS_RELWITHDEBINFO} -fsanitize=undefined" CACHE STRING "" FORCE)
set(CMAKE_SHARED_LINKER_FLAGS_UBSAN "${CMAKE_SHARED_LINKER_FLAGS_RELWITHDEBINFO} -fsanitize=undefined" CACHE STRING "" FORCE)

set_property(CACHE CMAKE_BUILD_TYPE PROPERTY STRINGS "Debug" "Release" "MinSizeRel" "RelWithDebInfo" "ASan" "TSan" "UBSan")

add_compile_options(-Wall -Wextra -Wpedantic -Werror)

add_executable(worker worker.c)
target_link_libraries(worker pthread)

add_executable(client-v1 client-v1.c)
target_link_libraries(client-v1 m ssl crypto)

add_executable(client-v2 client-v2.c)
target_link_libraries(client-v2 m ssl crypto)

add_executable(client-v3 client-v3.c)
target_link_libraries(client-v3 m ssl crypto)

add_executable(client-v4 client-v4.c)
target_link_libraries(client-v4 m ssl crypto pthread)

add_executable(client-v5 client-v5.c)
target_link_libraries(client-v5 m ssl crypto pthread)

# cmake -DCMAKE_C_FLAGS='-D...' <build>
# -DDELAY -DNO_DIGEST