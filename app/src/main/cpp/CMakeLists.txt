# cmake最低版本要求
cmake_minimum_required(VERSION 3.4.1)

# 声明模块名 audio-processor 打包目标
# so库 SHARED
# 源文件列表
# 从第三个参数开始，就是文件列表了
add_library(audio-processor SHARED
            smbPitchShift.c
            audioprocessor.c)

# 暂时不知道如何生成多个so库
#add_library(audio-processor-b SHARED
#            audioprocessor.c)

# so库需要包含的模块，这里包含了audio-processor android log 这三个模块
target_link_libraries(audio-processor
            android
            log)