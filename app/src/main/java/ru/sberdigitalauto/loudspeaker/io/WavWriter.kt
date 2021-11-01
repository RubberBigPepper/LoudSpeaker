package ru.sberdigitalauto.loudspeaker.io

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class WavWriter(sampleRate: Int, nChannels: Short) {
    private val LONGINT = 4
    private val SMALLINT = 2
    private val INTEGER = 4
    private val ID_STRING_SIZE = 4
    private val WAV_RIFF_SIZE = LONGINT + ID_STRING_SIZE
    private val WAV_FMT_SIZE = 4 * SMALLINT + INTEGER * 2 + LONGINT + ID_STRING_SIZE
    private val WAV_DATA_SIZE = ID_STRING_SIZE + LONGINT
    private val WAV_HDR_SIZE = WAV_RIFF_SIZE + ID_STRING_SIZE + WAV_FMT_SIZE + WAV_DATA_SIZE
    private val PCM: Short = 1
    private val SAMPLE_SIZE = 2

    var nSamples: Int = 0
    var output = mutableListOf<Byte>()

    var seekToAllSize = 0
    var seekToSamplesCount = 0

    init {
        buildHeader(sampleRate, nChannels)
        write("data")
        seekToSamplesCount = output.size
        write(nSamples * SMALLINT)
    }

    // ------------------------------------------------------------
    private fun buildHeader(sampleRate: Int, nChannels: Short) {
        write("RIFF")
        seekToAllSize = output.size
        write(output.size)
        write("WAVE")
        writeFormat(sampleRate, nChannels)
    }

    // ------------------------------------------------------------
    private fun writeFormat(sampleRate: Int, nChannels: Short) {
        write("fmt ")
        write(WAV_FMT_SIZE - WAV_DATA_SIZE)
        write(PCM)
        write(nChannels)
        write(sampleRate)
        write(nChannels * sampleRate * SAMPLE_SIZE)
        write((nChannels * SAMPLE_SIZE).toShort())
        write(16.toShort())
    }

    // ------------------------------------------------------------
    fun writeData(data: ShortArray, start: Int, end: Int) {
        var i = start
        while (i <= end) {
            write(data[i++])
        }
        nSamples += end - start + 1
    }

    fun writeData(data: ByteArray, start: Int, end: Int) {
        var i = start
        while (i <= end) {
            write(data[i++])
        }
        nSamples += (end - start + 1) / 2
    }

    // ------------------------------------------------------------
    private fun write(b: Byte, cursor: Int = -1) {
        if (cursor >= 0)
            output[cursor] = b
        else
            output.add(b)
    }

    // ------------------------------------------------------------
    private fun write(id: String, cursor: Int = -1) {
        if (id.length == ID_STRING_SIZE) {
            for (i in 0 until ID_STRING_SIZE)
                write(id[i].code.toByte(), if (cursor >= 0) cursor + i else -1)
        }
    }

    // ------------------------------------------------------------
    private fun write(i: Int, cursor: Int = -1) {
        var iVar = i
        write((iVar and 0xFF).toByte(), if (cursor >= 0) cursor else -1)
        iVar = iVar shr 8
        write((iVar and 0xFF).toByte(), if (cursor >= 0) cursor + 1 else -1)
        iVar = iVar shr 8
        write((iVar and 0xFF).toByte(), if (cursor >= 0) cursor + 2 else -1)
        iVar = iVar shr 8
        write((iVar and 0xFF).toByte(), if (cursor >= 0) cursor + 3 else -1)
    }

    // ------------------------------------------------------------
    private fun write(i: Short, cursor: Int = -1) {
        var iInt = i.toInt()
        write((iInt and 0xFF).toByte(), if (cursor >= 0) cursor else -1)
        iInt = iInt shr 8
        write((iInt and 0xFF).toByte(), if (cursor >= 0) cursor + 1 else -1)
    }

    fun stopRecord(): List<Byte> {
        write(output.size, seekToAllSize)
        write(nSamples * SMALLINT, seekToSamplesCount)
        return output
    }

}