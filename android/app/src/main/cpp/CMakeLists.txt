cmake_minimum_required(VERSION 3.22.1)
project("emergencylaneguard")

# Set NCNN path
set(NCNN_DIR ${CMAKE_SOURCE_DIR}/libs/ncnn/${ANDROID_ABI})

# Add NCNN include directory
include_directories(${NCNN_DIR}/include/ncnn)

# Add NCNN library
add_library(ncnn STATIC IMPORTED)
set_target_properties(ncnn PROPERTIES IMPORTED_LOCATION ${NCNN_DIR}/lib/libncnn.a)

# Add glslang libraries (required by NCNN for Vulkan support)
add_library(glslang STATIC IMPORTED)
set_target_properties(glslang PROPERTIES IMPORTED_LOCATION ${NCNN_DIR}/lib/libglslang.a)

add_library(SPIRV STATIC IMPORTED)
set_target_properties(SPIRV PROPERTIES IMPORTED_LOCATION ${NCNN_DIR}/lib/libSPIRV.a)

add_library(MachineIndependent STATIC IMPORTED)
set_target_properties(MachineIndependent PROPERTIES IMPORTED_LOCATION ${NCNN_DIR}/lib/libMachineIndependent.a)

add_library(GenericCodeGen STATIC IMPORTED)
set_target_properties(GenericCodeGen PROPERTIES IMPORTED_LOCATION ${NCNN_DIR}/lib/libGenericCodeGen.a)

add_library(OSDependent STATIC IMPORTED)
set_target_properties(OSDependent PROPERTIES IMPORTED_LOCATION ${NCNN_DIR}/lib/libOSDependent.a)

add_library(glslang-default-resource-limits STATIC IMPORTED)
set_target_properties(glslang-default-resource-limits PROPERTIES IMPORTED_LOCATION ${NCNN_DIR}/lib/libglslang-default-resource-limits.a)

add_library(${CMAKE_PROJECT_NAME} SHARED
    native-lib.cpp
    LaneDetector.cpp
    stl_fix.cpp
)

# Find OpenMP
find_package(OpenMP REQUIRED)

# Link libraries in correct order
target_link_libraries(${CMAKE_PROJECT_NAME}
    ncnn
    glslang
    SPIRV
    MachineIndependent
    GenericCodeGen
    OSDependent
    glslang-default-resource-limits
    OpenMP::OpenMP_CXX
    android
    log
    jnigraphics
    z
    c++_shared
)
