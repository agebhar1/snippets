cmake_minimum_required(VERSION 3.28)
project(client C)

set(CMAKE_C_STANDARD 11)

add_compile_options(-Wall -Wextra -Wpedantic)

add_executable(client main.c)
target_link_libraries(client ssl crypto)
