cmake_minimum_required(VERSION 3.28)
project(cases C)

set(CMAKE_C_STANDARD 23)

add_compile_options(-Wall -Wextra -Wpedantic)

add_executable(client client.c)
