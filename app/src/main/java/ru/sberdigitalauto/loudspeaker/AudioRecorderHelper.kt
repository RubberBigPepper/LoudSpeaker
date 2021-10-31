package ru.sberdigitalauto.loudspeaker

import android.media.AudioRecord
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.isActive

interface AudioRecorderHelperListener{
    fun onPrepared(recorder: AudioRecorderHelper)
    fun onStopped(recorder: AudioRecorderHelper)
    fun onStarted(recorder: AudioRecorderHelper)
    fun onDataRecorded(recorder: AudioRecorderHelper, data: ByteArray, readBytes: Int)
    fun onRecorderError(recorder: AudioRecorderHelper)
}

class AudioRecorderHelper (audioConfig: AudioConfig, private val listener: AudioRecorderHelperListener) {
    private var audioRecord: AudioRecord? = null

    // for raw audio, use MediaRecorder.AudioSource.UNPROCESSED, see note in MediaRecorder section
    private val audioSource = audioConfig.audioSource
    private val sampleRate = audioConfig.sampleRate
    private val channels: Int = audioConfig.channelsIn
    private val audioFormat: Int = audioConfig.audioFormat
    private val recordingBufferSize = audioConfig.recordingBufferSize

    private val TAG = "AudioRecorderHelper"

    private var recordingJob: Job? = null

    val isRecording: Boolean
        get() {
            return recordingJob?.isActive ?: false
        }

    fun stopRecording(){
        recordingJob?.cancel()
    }

    @InternalCoroutinesApi
    fun startRecording() {
        if (isRecording)
            return
        audioRecord = prepareAudioRecord()
        if (audioRecord == null) {
            return
        }
        listener.onPrepared(this)
        try {
            audioRecord?.startRecording()
            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                recordAudio()
            }
            listener.onStarted(this)
        }
        catch (exception: Exception){
            Log.e(TAG, "error initializing " + exception.printStackTrace())
            listener.onRecorderError(this)
        }
    }

    private fun prepareAudioRecord(): AudioRecord?{
        try {
            audioRecord = AudioRecord(
                audioSource,
                sampleRate,
                channels,
                audioFormat,
                recordingBufferSize
            )
            if (audioRecord?.getState() != AudioRecord.STATE_INITIALIZED) { // check for proper initialization
                Log.e(TAG, "error initializing unknown")
                return null
            }
            return audioRecord
        }
        catch (exception: Exception){
            Log.e(TAG, "error initializing " + exception.printStackTrace())
            return null
        }
    }

    @InternalCoroutinesApi
    private suspend fun recordAudio() { // to be called in a Runnable for a Thread created after call to startRecording()
        while (isActive) { // continueRecording can be toggled by a button press, handled by the main (UI) thread
            val data = ByteArray(recordingBufferSize) // assign size so that bytes are read in in chunks inferior to AudioRecord internal buffer size
            val read = audioRecord?.read(data, 0, data.size)
            if (read != null && read > 0){
                listener.onDataRecorded(this, data, read)
            }
            else{
                listener.onRecorderError(this)
            }
        }
        // Clean up
        try {
            audioRecord?.stop()
            audioRecord?.release()
        }
        catch (ex: java.lang.Exception){
            ex.printStackTrace()
        }
        audioRecord = null
        listener.onStopped(this)
    }
}