cmake_minimum_required(VERSION 3.22.1)
project(mentara_whisper CXX)

set(WHISPER_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../../whisper-src/whisper.cpp-master")

set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)

add_subdirectory(${WHISPER_ROOT} ${CMAKE_BINARY_DIR}/whisper_build EXCLUDE_FROM_ALL)

add_library(whisperjni SHARED whisper_jni.cpp)
target_include_directories(whisperjni PRIVATE
    ${WHISPER_ROOT}/include
    ${WHISPER_ROOT}/ggml/include)

find_library(log-lib log)
target_link_libraries(whisperjni whisper ${log-lib})
