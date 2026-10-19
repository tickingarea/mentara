# CMake configuration for JNI whisper integration
cmake_minimum_required(VERSION 3.10.2)

# Project name and language
project(whisper-android)

# Add Android-specific settings
if(ANDROID)
    # Set C++ standard
    set(CMAKE_CXX_STANDARD 17)
    set(CMAKE_CXX_STANDARD_REQUIRED ON)
    
    # Add log library for Android logging
    find_library(log-lib log)
endif()

# Create JNI library with mock implementation
add_library(jni_whisper SHARED
    jni_whisper.cpp
)

# Include directories
target_include_directories(jni_whisper PRIVATE
    ${ANDROID_NDK}/sources/cxx-stl/llvm-libc++
)

# Link libraries
target_link_libraries(jni_whisper
    ${log-lib}
    log
)

# Set output properties
set_target_properties(jni_whisper PROPERTIES
    CXX_STANDARD 17
    CXX_STANDARD_REQUIRED ON
    CXX_EXTENSIONS OFF
)

# Install rules
install(TARGETS jni_whisper
    LIBRARY DESTINATION lib
    ARCHIVE DESTINATION lib
)
