cmake_minimum_required(VERSION 3.22.1)
project(mkxpz_bridge)

add_library(mkxpz_bridge SHARED mkxp_bridge.cpp)

target_link_libraries(mkxpz_bridge android log)

if(ANDROID)
    target_link_options(mkxpz_bridge PRIVATE "-Wl,-z,max-page-size=16384")
endif()
