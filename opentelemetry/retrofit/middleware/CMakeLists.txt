cmake_minimum_required(VERSION 3.28)
project(middleware C)

set(CMAKE_C_STANDARD 23)
#set(CMAKE_SKIP_BUILD_RPATH TRUE)

add_compile_options(-Wall -Wextra -Wpedantic)

add_library(middleware SHARED
        middleware.h middleware.c
        opentelemetry.h opentelemetry.c
)

add_executable(client client.c middleware.h)
add_executable(server server.c
        middleware.h middleware.c
        opentelemetry.h dummy.c)

target_link_libraries(client m middleware ssl crypto)