cmake_minimum_required(VERSION 3.22.1)
project(inno_cli_bridge)

add_library(inno_cli_bridge SHARED inno_cli_bridge.cpp)

target_link_libraries(inno_cli_bridge android log dl)

if(ANDROID)
    target_link_options(inno_cli_bridge PRIVATE "-Wl,-z,max-page-size=16384")
endif()
