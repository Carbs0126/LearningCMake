#ifndef  __SMBPITCHSHIFT_H_
#define __SMBPITCHSHIFT_H_
#include<stdio.h>

void smbFft(float *fftBuffer, long fftFrameSize, long sign);

void smbPitchShift(float pitchShift, long numSampsToProcess, long fftFrameSize, long osamp, float sampleRate, float *indata, float *outdata);

#endif