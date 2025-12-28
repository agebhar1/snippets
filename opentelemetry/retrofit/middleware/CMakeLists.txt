cmake_minimum_required(VERSION 3.28)
project(middleware C)

set(CMAKE_C_STANDARD 23)
#set(CMAKE_SKIP_BUILD_RPATH TRUE)

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

add_compile_options(-Wall -Wextra -Wpedantic)

add_library(middleware SHARED
        middleware.h middleware.c
        opentelemetry.h opentelemetry.c
)

add_executable(client client.c middleware.h)
add_executable(server server.c
        middleware.h middleware.c
        opentelemetry.h dummy.c)
add_executable(producer producer.c middleware.h)

target_link_libraries(client middleware m ssl crypto)
target_link_libraries(producer middleware m ssl crypto)
