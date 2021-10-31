package ru.sberdigitalauto.loudspeaker

import kotlinx.coroutines.InternalCoroutinesApi

class AudioDataManager {
    private val audioConfig = AudioConfig()

    private val audioRecorderListener = object : AudioRecorderHelperListener {
        override fun onPrepared(recorder: AudioRecorderHelper) {

        }

        override fun onStopped(recorder: AudioRecorderHelper) {

        }

        override fun onStarted(recorder: AudioRecorderHelper) {

        }

        override fun onDataRecorded(
            recorder: AudioRecorderHelper,
            data: ByteArray,
            readBytes: Int
        ) {
            if (enableAEC && aec!=null && prevDataTrack!= null) {//обработаем эхо
                val dataAEC = aec?.makeAEC(data, prevDataTrack)
                if (dataAEC!=null) {
                    prevDataTrack = dataAEC
                    audioTrackHelper?.setPlayData(dataAEC, readBytes)
                }
                else {
                    audioTrackHelper?.setPlayData(data, readBytes)
                    prevDataTrack = data
                }
            }
            else {
                audioTrackHelper?.setPlayData(data, readBytes)
                prevDataTrack = data
            }
        }

        override fun onRecorderError(recorder: AudioRecorderHelper) {
            stop()
        }
    }

    private val audioTrackListener = object : AudioTrackHelperListener {
        override fun onPrepared(player: AudioTrackHelper) {

        }

        override fun onStopped(player: AudioTrackHelper) {

        }

        override fun onStarted(player: AudioTrackHelper) {

        }

        override fun onDataPlayed(player: AudioTrackHelper) {

        }

        override fun onPlayerError(player: AudioTrackHelper) {
            stop()
        }

    }

    private var audioTrackHelper: AudioTrackHelper? = null
    private var audioRecorderHelper: AudioRecorderHelper? = null
    private var aec:EchoCancelationNativeHelper? = null

    var enableAEC: Boolean = false

    private var prevDataTrack: ByteArray? = null

    @InternalCoroutinesApi
    fun start() {
        stop()
        aec = EchoCancelationNativeHelper()
        aec?.prepareAEC(audioConfig)

        audioTrackHelper = AudioTrackHelper(audioConfig, audioTrackListener)
        audioRecorderHelper = AudioRecorderHelper(audioConfig, audioRecorderListener)
        audioRecorderHelper?.startRecording()
        audioTrackHelper?.startPlaying()
    }

    fun stop() {
        val audioTrack = audioTrackHelper
        audioTrackHelper = null
        audioTrack?.stopPlaying()

        val audioRecord = audioRecorderHelper
        audioRecorderHelper = null
        audioRecord?.stopRecording()

        val aecTemp = aec
        aec = null
        aecTemp?.releaseAEC()
    }


}