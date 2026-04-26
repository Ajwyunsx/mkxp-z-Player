LOCAL_PATH := $(call my-dir)
SDL2_SOUND_PATH := $(LOCAL_PATH)/SDL2_sound

include $(CLEAR_VARS)

LOCAL_MODULE := SDL2_sound

LOCAL_C_INCLUDES := \
	$(SDL2_SOUND_PATH) \
	$(SDL2_SOUND_PATH)/src

LOCAL_CFLAGS := -DSOUND_SUPPORTS_MIDI=0

LOCAL_SRC_FILES := \
	SDL2_sound/src/SDL_sound.c \
	SDL2_sound/src/SDL_sound_aiff.c \
	SDL2_sound/src/SDL_sound_au.c \
	SDL2_sound/src/SDL_sound_coreaudio.c \
	SDL2_sound/src/SDL_sound_flac.c \
	SDL2_sound/src/SDL_sound_mp3.c \
	SDL2_sound/src/SDL_sound_midi.c \
	SDL2_sound/src/SDL_sound_modplug.c \
	SDL2_sound/src/SDL_sound_raw.c \
	SDL2_sound/src/SDL_sound_shn.c \
	SDL2_sound/src/SDL_sound_voc.c \
	SDL2_sound/src/SDL_sound_vorbis.c \
	SDL2_sound/src/SDL_sound_wav.c

LOCAL_SRC_FILES += $(patsubst $(LOCAL_PATH)/%,%,$(wildcard $(SDL2_SOUND_PATH)/src/libmodplug/*.c))

LOCAL_SHARED_LIBRARIES := SDL2
LOCAL_STATIC_LIBRARIES := libogg libvorbis

LOCAL_EXPORT_C_INCLUDES := \
	$(SDL2_SOUND_PATH) \
	$(SDL2_SOUND_PATH)/src

include $(BUILD_SHARED_LIBRARY)
