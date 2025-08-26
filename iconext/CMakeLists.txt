cmake_minimum_required(VERSION 3.21)

project(iconext)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED True)

if (WIN32)
    add_library(iconext SHARED windows/iconext.cpp)

    target_include_directories(iconext PRIVATE . $ENV{JAVA_HOME}/include $ENV{JAVA_HOME}/include/win32)
endif()
