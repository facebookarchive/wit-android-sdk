
#include "WITCvad.h"
#include <jni.h>


static s_wv_detector_cvad_state* wit_vad_g_struct = 0;

int Java_ai_wit_sdk_WitMic_VadInit()
{
    double th = 8.0;
    int sample_rate = 16000;
    int speech_timeout = 8000; //timeout in ms

    wit_vad_g_struct = wv_detector_cvad_init(sample_rate, 0, speech_timeout);

    return 0;
}


int Java_ai_wit_sdk_WitMic_VadStillTalking(JNIEnv *env, jobject obj, jshortArray java_arr, jint arr_len)
{
  short int *samples;
  int i, sum = 0;
  int result;
  jshort *native_arr = (*env)->GetShortArrayElements(env, java_arr, NULL);

  samples = malloc(sizeof(*samples) * arr_len);
  for (i = 0; i < arr_len; i++) {
    samples[i] = native_arr[i];

  }
  (*env)->ReleaseShortArrayElements(env, java_arr, native_arr, 0);

  result = wvs_cvad_detect_talking(wit_vad_g_struct, samples, arr_len);
  free(samples);

  return result;
}

void Java_ai_wit_sdk_WitMic_VadClean()
{
    if (wit_vad_g_struct) {
        wv_detector_cvad_clean(wit_vad_g_struct);
        wit_vad_g_struct = 0;
    }
}
