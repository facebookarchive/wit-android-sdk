
#include "WITVadSimple.h"
#include <jni.h>


static wvs_state* wit_vad_g_struct = 0;

int Java_ai_wit_sdk_WitMic_VadInit()
{
    double th = 8.0;
    int sample_rate = 16000;

    wit_vad_g_struct = wvs_init(th, sample_rate);

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

  result = wvs_still_talking(wit_vad_g_struct, samples, arr_len);
  free(samples);

  return result;
}

void Java_ai_wit_sdk_WitMic_VadClean()
{
    if (wit_vad_g_struct) {
        wvs_clean(wit_vad_g_struct);
        wit_vad_g_struct = 0;
    }
}
