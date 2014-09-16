
#include "WITVadSimple.h"
#include <jni.h>


static wvs_state* wit_vad_g_struct = 0;

int Java_ai_wit_sdk_WitMic_VadInit()
{
    double th = 8.0;
    int sample_rate = 44100;

    wit_vad_g_struct = wvs_init(th, sample_rate);

    return 0;
}


int Java_ai_wit_sdk_WitMic_VadStillTalking(JNIEnv *env, jobject obj, jshortArray arr)
{
  int *samples;
  jsize len = (*env)->GetArrayLength(env, arr);
  int i, sum = 0;
  jint *body = (*env)->GetIntArrayElements(env, arr, 0);
  samples = malloc(sizeof(samples) * len);
  for (i=0; i<len; i++) {
    samples[i] = body[i];
  }
  (*env)->ReleaseIntArrayElements(env, arr, body, 0);

  return wvs_still_talking(wit_vad_g_struct, samples, len);
}

void Java_ai_wit_sdk_WitMic_VadClean()
{
    wvs_clean(wit_vad_g_struct);
}
