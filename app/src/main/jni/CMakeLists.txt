cmake_minimum_required(VERSION 3.18.0)

project(yuyan_asr VERSION 1.0.0)

set(CMAKE_CXX_STANDARD 17)

set(JNI_DIR "${CMAKE_CURRENT_SOURCE_DIR}")
set(ASR_SRC_DIR "${JNI_DIR}")
set(KNF_ROOT "${ASR_SRC_DIR}/third_party/knf")
set(KNF_LOCAL_DIR "${KNF_ROOT}/kaldi-native-fbank-1.22.3")
set(FETCHCONTENT_SOURCE_DIR_KISSFFT "${KNF_ROOT}/kissfft")

# 引入 kaldi-native-fbank
option(KNF_BUILD_TESTS "" OFF)
add_subdirectory(${KNF_LOCAL_DIR} knf_build)

# 引入 ONNX Runtime 库
set(ONNXRUNTIME_DIR "${JNI_DIR}/onnxruntime")

add_library(onnxruntime SHARED IMPORTED)
set_target_properties(onnxruntime PROPERTIES
    IMPORTED_LOCATION "${ONNXRUNTIME_DIR}/lib/${ANDROID_ABI}/libonnxruntime.so"
    INTERFACE_INCLUDE_DIRECTORIES "${JNI_DIR}/include;${JNI_DIR}/include/onnxruntime"
)

add_library(onnx_env SHARED
    onnx_env.cc
    onnx_env.h
)

target_include_directories(onnx_env PRIVATE
    ${JNI_DIR}/include
    ${JNI_DIR}/include/onnxruntime
)

target_link_libraries(onnx_env
    onnxruntime
    log
    dl
)

add_library(asr_jni SHARED
    ${ASR_SRC_DIR}/asr/asr_jni.cc
    ${ASR_SRC_DIR}/asr/zipformer2_model.cc
    ${ASR_SRC_DIR}/asr/feature_extractor.cc
    ${ASR_SRC_DIR}/asr/streaming_recognizer.cc
)

target_include_directories(asr_jni PRIVATE
    ${JNI_DIR}/include
    ${JNI_DIR}/include/onnxruntime
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${KNF_LOCAL_DIR}
)

target_link_libraries(asr_jni
    onnxruntime
    onnx_env
    kaldi-native-fbank-core
    log
    dl
)

target_link_options(asr_jni PRIVATE
    "-Wl,--allow-shlib-undefined"
    "-Wl,-z,max-page-size=16384"
)
