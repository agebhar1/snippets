cmake_minimum_required(VERSION 3.28)
project(cases C)

set(CMAKE_C_STANDARD 23)

add_compile_options(-Wall -Wextra -Wpedantic)

add_executable(threads threads.c)
target_link_libraries(threads pthread)

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
#target_compile_definitions(client-v1 PRIVATE DEBUG)
#target_compile_definitions(client-v2 PRIVATE DEBUG)
#target_compile_definitions(client-v3 PRIVATE DEBUG)
#target_compile_definitions(client-v4 PRIVATE DEBUG)
#target_compile_definitions(client-v5 PRIVATE DEBUG)

target_compile_definitions(client-v1 PRIVATE DIGEST)
target_compile_definitions(client-v2 PRIVATE DIGEST)
target_compile_definitions(client-v3 PRIVATE DIGEST)
target_compile_definitions(client-v4 PRIVATE DIGEST)
target_compile_definitions(client-v5 PRIVATE DIGEST)

#target_compile_definitions(client-v4 PRIVATE DELAY)
#target_compile_definitions(client-v5 PRIVATE DELAY)
