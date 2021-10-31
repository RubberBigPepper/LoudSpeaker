package ru.sberdigitalauto.loudspeaker

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder

data class AudioConfig(
    val audioSource: Int = MediaRecorder.AudioSource.MIC,
    val audioStream: Int = AudioManager.STREAM_MUSIC,
    val sampleRate: Int = 44100,
    val channelsIn: Int = AudioFormat.CHANNEL_IN_MONO,
    val channelsOut: Int = AudioFormat.CHANNEL_OUT_MONO,
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
){
    val recordingBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelsIn, audioFormat)
}