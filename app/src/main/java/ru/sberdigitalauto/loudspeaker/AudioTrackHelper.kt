package ru.sberdigitalauto.loudspeaker

import android.media.AudioTrack
import android.util.Log
import java.lang.Exception

interface AudioTrackHelperListener{
    fun onPrepared(player: AudioTrackHelper)
    fun onStopped(player: AudioTrackHelper)
    fun onStarted(player: AudioTrackHelper)
    fun onDataPlayed(player: AudioTrackHelper)
    fun onPlayerError(player: AudioTrackHelper)
}

class AudioTrackHelper(audioConfig: AudioConfig, private val listener: AudioTrackHelperListener) {

    private val audioStream = audioConfig.audioStream
    private val sampleRate = audioConfig.sampleRate
    private val channels: Int = audioConfig.channelsOut
    private val audioFormat: Int = audioConfig.audioFormat
    private val bufferSize = audioConfig.recordingBufferSize

    private var audioTrack: AudioTrack? = null

    private val TAG = "AudioTrackHelper"

    val isPlaying: Boolean
        get() {
            audioTrack?.playState?.let {
                return it == AudioTrack.PLAYSTATE_PLAYING
            }
            return false
        }

    fun stopPlaying() {
        if (isPlaying) {
            audioTrack?.stop()
            audioTrack?.release()
            listener.onStopped(this)
        }
    }

    private fun prepareAudioTrack(): AudioTrack? {
        try {
            return AudioTrack(
                audioStream,
                sampleRate,
                channels,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM
            );
        } catch (ex: Exception) {
            ex.printStackTrace()
            listener.onPlayerError(this)
            return null
        }
    }

    fun startPlaying() {
        if (isPlaying)
            return
        audioTrack = prepareAudioTrack()
        if (audioTrack == null) {
            return
        }
        listener.onPrepared(this)
        try {
            audioTrack?.play()
            listener.onStarted(this)
        } catch (exception: Exception) {
            Log.e(TAG, "error initializing " + exception.printStackTrace())
            listener.onPlayerError(this)
        }
    }

    fun setPlayData(data: ByteArray, leng: Int) {
        audioTrack?.let {
            val written = it.write(data, 0, leng)
            if (written <= 0)
                listener.onPlayerError(this)
            else
                listener.onDataPlayed(this)
        }
    }
}