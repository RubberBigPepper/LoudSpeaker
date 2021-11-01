package ru.sberdigitalauto.loudspeaker.io

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.sberdigitalauto.loudspeaker.EchoCancelationNativeHelper
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

class WavManager {
    private var convertJob: Job? = null

    fun convertFile(inputWav: Uri, context: Context){
        convertJob?.cancel()
        convertJob = CoroutineScope(Dispatchers.IO).launch {
            convert(inputWav, context)
        }
    }

    private suspend fun convert(inputWav: Uri, context: Context){
        context.contentResolver.openInputStream(inputWav)?.let { inputStream ->
            val wavRead = WavReader.openWavFile(inputStream)
            if (wavRead.numChannels != 2)
                return
            val wavWriter = WavWriter(wavRead.sampleRate.toInt(), 1)
            val channelsData = wavRead.readFrames()
            val size = channelsData.first.size
            val aec = EchoCancelationNativeHelper()
            val bufferSize = 1024
            val filterSize = 16384
            aec.prepareAEC(bufferSize, filterSize)
            for (n in 0 until size step bufferSize) {
                val end = n + bufferSize
                if (end >= size)
                    break
                val inputBuffer = channelsData.first.subList(n, end).toShortArray()
                val echoBuffer = channelsData.second.subList(n, end).toShortArray()
                echoBuffer.map { it*2 }
                val outData = aec.makeAEC(inputBuffer, echoBuffer)
                //val outData = echoBuffer
                wavWriter.writeData(outData, 0, outData.size - 1)
            }
            val bytes = wavWriter.stopRecord()
            val file = File(Environment.getExternalStorageDirectory().absolutePath + "/out_res.wav")
            FileOutputStream(file).use {
                val writeStep = 16384
                for (n in bytes.indices step writeStep) {
                    val end = min(bytes.size, n + writeStep)
                    it.write(bytes.subList(n, end).toByteArray())
                }
            }
            Log.e("WavManager", "Write successful")
        }
    }
}