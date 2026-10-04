cmake_minimum_required(VERSION 3.28)
project(sanitizer C)

set(CMAKE_C_STANDARD 23)

add_compile_options(-Wall -Wextra -Wpedantic -Werror)

add_executable(address memory-leak.c)
target_compile_options(address PRIVATE -fsanitize=address)
target_link_options(address PRIVATE -fsanitize=address)

add_executable(thread simple-race.c)
target_compile_options(thread PRIVATE -fsanitize=thread)
target_link_options(thread PRIVATE -fsanitize=thread)

add_executable(undefined out-of-bounds.c)
target_compile_options(undefined PRIVATE -fsanitize=undefined)
target_link_options(undefined PRIVATE -fsanitize=undefined)
