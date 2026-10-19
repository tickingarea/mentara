# Install script for directory: C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml

# Set the install prefix
if(NOT DEFINED CMAKE_INSTALL_PREFIX)
  set(CMAKE_INSTALL_PREFIX "C:/Program Files (x86)/mentara_whisper")
endif()
string(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# Set the install configuration name.
if(NOT DEFINED CMAKE_INSTALL_CONFIG_NAME)
  if(BUILD_TYPE)
    string(REGEX REPLACE "^[^A-Za-z0-9_]+" ""
           CMAKE_INSTALL_CONFIG_NAME "${BUILD_TYPE}")
  else()
    set(CMAKE_INSTALL_CONFIG_NAME "Debug")
  endif()
  message(STATUS "Install configuration: \"${CMAKE_INSTALL_CONFIG_NAME}\"")
endif()

# Set the component getting installed.
if(NOT CMAKE_INSTALL_COMPONENT)
  if(COMPONENT)
    message(STATUS "Install component: \"${COMPONENT}\"")
    set(CMAKE_INSTALL_COMPONENT "${COMPONENT}")
  else()
    set(CMAKE_INSTALL_COMPONENT)
  endif()
endif()

# Install shared libraries without execute permission?
if(NOT DEFINED CMAKE_INSTALL_SO_NO_EXE)
  set(CMAKE_INSTALL_SO_NO_EXE "0")
endif()

# Is this installation the result of a crosscompile?
if(NOT DEFINED CMAKE_CROSSCOMPILING)
  set(CMAKE_CROSSCOMPILING "TRUE")
endif()

# Set default install directory permissions.
if(NOT DEFINED CMAKE_OBJDUMP)
  set(CMAKE_OBJDUMP "C:/Android/Sdk/ndk/25.2.9519653/toolchains/llvm/prebuilt/windows-x86_64/bin/llvm-objdump.exe")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/Administrator/Documents/Default Project/learnagent/androidApp/.cxx/Debug/3639661h/arm64-v8a/whisper_build/ggml/src/cmake_install.cmake")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib" TYPE STATIC_LIBRARY FILES "C:/Users/Administrator/Documents/Default Project/learnagent/androidApp/.cxx/Debug/3639661h/arm64-v8a/whisper_build/ggml/src/libggml.a")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include" TYPE FILE FILES
    "C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml/include/ggml.h"
    "C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml/include/ggml-cpu.h"
    "C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml/include/ggml-alloc.h"
    "C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml/include/ggml-backend.h"
    "C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml/include/ggml-blas.h"
    "C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml/include/ggml-cann.h"
    "C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml/include/ggml-cpp.h"
    "C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml/include/ggml-cuda.h"
    "C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml/include/ggml-opt.h"
    "C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml/include/ggml-metal.h"
    "C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml/include/ggml-rpc.h"
    "C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml/include/ggml-virtgpu.h"
    "C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml/include/ggml-sycl.h"
    "C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml/include/ggml-vulkan.h"
    "C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml/include/ggml-webgpu.h"
    "C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml/include/ggml-zendnn.h"
    "C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml/include/ggml-openvino.h"
    "C:/Users/Administrator/Documents/Default Project/learnagent/whisper-src/whisper.cpp-master/ggml/include/gguf.h"
    )
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib" TYPE STATIC_LIBRARY FILES "C:/Users/Administrator/Documents/Default Project/learnagent/androidApp/.cxx/Debug/3639661h/arm64-v8a/whisper_build/ggml/src/libggml-base.a")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/ggml" TYPE FILE FILES
    "C:/Users/Administrator/Documents/Default Project/learnagent/androidApp/.cxx/Debug/3639661h/arm64-v8a/whisper_build/ggml/ggml-config.cmake"
    "C:/Users/Administrator/Documents/Default Project/learnagent/androidApp/.cxx/Debug/3639661h/arm64-v8a/whisper_build/ggml/ggml-config-version.cmake"
    )
endif()

