package ru.sberdigitalauto.loudspeaker.io

import java.io.*
import kotlin.experimental.and


class WavReader private constructor() {

    private enum class IOState {
        READING, CLOSED
    }

    private var ioState // Specifies the IO State of the Wav File (used for snaity checking)
            : IOState? = null
    private var bytesPerSample // Number of bytes required to store a single sample
            = 0
    var numFrames // Number of frames within the data section
            : Long = 0
        private set

    private var iStream // Input stream used for reading data
            : InputStream? = null
    private var floatScale // Scaling factor used for int <-> float conversion
            = 0f
    private var floatOffset // Offset factor used for int <-> float conversion
            = 0f
    private val wordAlignAdjust // Specify if an extra byte at the end of the data chunk is required for word alignment
            = false

    // Wav Header
    var numChannels // 2 bytes unsigned, 0x0001 (1) to 0xFFFF (65,535)
            = 0
        private set
    var sampleRate // 4 bytes unsigned, 0x00000001 (1) to 0xFFFFFFFF (4,294,967,295)
            : Long = 0
        private set

    // Although a java int is 4 bytes, it is signed, so need to use a long
    private var blockAlign // 2 bytes unsigned, 0x0001 (1) to 0xFFFF (65,535)
            = 0
    var validBits // 2 bytes unsigned, 0x0002 (2) to 0xFFFF (65,535)
            = 0
        private set

    // Buffering
    private val buffer // Local buffer used for IO
            : ByteArray
    private var bufferPointer // Points to the current position in local buffer
            = 0
    private var bytesRead // Bytes read after last read into local buffer
            = 0
    private var frameCounter // Current number of frames read or written
            : Long = 0
    var fileSize: Long = 0
        private set
    val framesRemaining: Long
        get() = numFrames - frameCounter
    val duration: Long
        get() = numFrames / sampleRate

    private fun readSample(): Long {
        var `val`: Long = 0
        for (b in 0 until bytesPerSample) {
            if (bufferPointer == bytesRead) {
                val read = iStream!!.read(buffer, 0, BUFFER_SIZE)
                if (read == -1) throw Exception("Not enough data available")
                bytesRead = read
                bufferPointer = 0
            }
            var v = buffer[bufferPointer].toInt()
            if (b < bytesPerSample - 1 || bytesPerSample == 1) v = v and 0xFF
            `val` += (v shl b * 8).toLong()
            bufferPointer++
        }
        return `val`
    }

    fun readFrames(sampleBuffer: FloatArray, numFramesToRead: Int): Int {
        return readFramesInternal(sampleBuffer, 0, numFramesToRead)
    }

    private fun readFramesInternal(
        sampleBuffer: FloatArray,
        offset: Int,
        numFramesToRead: Int
    ): Int {
        var offset = offset
        if (ioState != IOState.READING) throw IOException("Cannot read from WavFile instance")
        for (f in 0 until numFramesToRead) {
            if (frameCounter == numFrames) return f
            for (c in 0 until numChannels) {
                sampleBuffer[offset] = floatOffset + readSample().toFloat() / floatScale
                offset++
            }
            frameCounter++
        }
        return numFramesToRead
    }

    fun readFrames(sampleBuffer: IntArray, numFramesToRead: Int): Int {
        return readFramesInternal(sampleBuffer, 0, numFramesToRead)
    }

    private fun readFramesInternal(sampleBuffer: IntArray, offset: Int, numFramesToRead: Int): Int {
        var offset = offset
        if (ioState != IOState.READING) throw IOException("Cannot read from WavFile instance")
        for (f in 0 until numFramesToRead) {
            if (frameCounter == numFrames)
                return f
            for (c in 0 until numChannels) {
                sampleBuffer[offset] = readSample().toInt()
                offset++
            }
            frameCounter++
        }
        return numFramesToRead
    }

    //читаем WAV на два канала
    fun readFrames(): Pair<List<Short>, List<Short>> {
        val framesToRead = 16
        val intArray = IntArray(framesToRead * 2)
        val leftChannel = mutableListOf<Short>()
        val rightChannel = mutableListOf<Short>()
        while (true) {
            val read = readFrames(intArray, framesToRead)
            for (n in 0 until read) {//у нас в Int содержится сразу оба канала, сделаем сплит
                rightChannel.add(intArray[2 * n].toShort())
                leftChannel.add(intArray[2 * n + 1].toShort()) //n+1
            }
            if (read < framesToRead)
                break
        }
        return leftChannel to rightChannel
    }


    fun close() {
        // Close the input stream and set to null
        if (iStream != null) {
            iStream!!.close()
            iStream = null
        }
        // Flag that the stream is closed
        ioState = IOState.CLOSED
    }

    companion object {
        private const val BUFFER_SIZE = 4096
        private const val FMT_CHUNK_ID = 0x20746D66
        private const val DATA_CHUNK_ID = 0x61746164
        private const val RIFF_CHUNK_ID = 0x46464952
        private const val RIFF_TYPE_ID = 0x45564157

        fun openWavFile(stream: InputStream): WavReader {
            // Instantiate new Wavfile and store the file reference
            val wavFile = WavReader()

            // Create a new file input stream for reading file data
            wavFile.iStream = stream

            // Read the first 12 bytes of the file
            var bytesRead = wavFile.iStream!!.read(wavFile.buffer, 0, 12)
            if (bytesRead != 12) throw Exception("Not enough wav file bytes for header")

            // Extract parts from the header
            val riffChunkID = getLE(wavFile.buffer, 0, 4)
            var chunkSize = getLE(wavFile.buffer, 4, 4)
            val riffTypeID = getLE(wavFile.buffer, 8, 4)

            // Check the header bytes contains the correct signature
            if (riffChunkID != RIFF_CHUNK_ID.toLong()) throw Exception("Invalid Wav Header data, incorrect riff chunk ID")
            if (riffTypeID != RIFF_TYPE_ID.toLong()) throw Exception("Invalid Wav Header data, incorrect riff type ID")

            // Check that the file size matches the number of bytes listed in header
            //if (stream.length() != chunkSize + 8) {
              //  throw Exception("Header chunk size (" + chunkSize + ") does not match file size (" + file.length() + ")")
            //}
            wavFile.fileSize = chunkSize
            var foundFormat = false
            var foundData = false

            // Search for the Format and Data Chunks
            while (true) {
                // Read the first 8 bytes of the chunk (ID and chunk size)
                bytesRead = wavFile.iStream!!.read(wavFile.buffer, 0, 8)
                if (bytesRead == -1) throw Exception("Reached end of file without finding format chunk")
                if (bytesRead != 8) throw Exception("Could not read chunk header")

                // Extract the chunk ID and Size
                val chunkID = getLE(wavFile.buffer, 0, 4)
                chunkSize = getLE(wavFile.buffer, 4, 4)

                // Word align the chunk size
                // chunkSize specifies the number of bytes holding data. However,
                // the data should be word aligned (2 bytes) so we need to calculate
                // the actual number of bytes in the chunk
                var numChunkBytes = if (chunkSize % 2 == 1L) chunkSize + 1 else chunkSize
                if (chunkID == FMT_CHUNK_ID.toLong()) {
                    // Flag that the format chunk has been found
                    foundFormat = true

                    // Read in the header info
                    bytesRead = wavFile.iStream!!.read(wavFile.buffer, 0, 16)

                    // Check this is uncompressed data
                    val compressionCode = getLE(wavFile.buffer, 0, 2).toInt()
                    if (compressionCode != 1) throw Exception("Compression Code $compressionCode not supported")

                    // Extract the format information
                    wavFile.numChannels = getLE(wavFile.buffer, 2, 2).toInt()
                    wavFile.sampleRate = getLE(wavFile.buffer, 4, 4)
                    wavFile.blockAlign = getLE(wavFile.buffer, 12, 2).toInt()
                    wavFile.validBits = getLE(wavFile.buffer, 14, 2).toInt()
                    if (wavFile.numChannels == 0) throw Exception("Number of channels specified in header is equal to zero")
                    if (wavFile.blockAlign == 0) throw Exception("Block Align specified in header is equal to zero")
                    if (wavFile.validBits < 2) throw Exception("Valid Bits specified in header is less than 2")
                    if (wavFile.validBits > 64) throw Exception("Valid Bits specified in header is greater than 64, this is greater than a long can hold")

                    // Calculate the number of bytes required to hold 1 sample
                    wavFile.bytesPerSample = (wavFile.validBits + 7) / 8
                    if (wavFile.bytesPerSample * wavFile.numChannels != wavFile.blockAlign) throw Exception(
                        "Block Align does not agree with bytes required for validBits and number of channels"
                    )

                    // Account for number of format bytes and then skip over
                    // any extra format bytes
                    numChunkBytes -= 16
                    if (numChunkBytes > 0) wavFile.iStream!!.skip(numChunkBytes)
                } else if (chunkID == DATA_CHUNK_ID.toLong()) {
                    // Check if we've found the format chunk,
                    // If not, throw an exception as we need the format information
                    // before we can read the data chunk
                    if (foundFormat == false) throw Exception("Data chunk found before Format chunk")

                    // Check that the chunkSize (wav data length) is a multiple of the
                    // block align (bytes per frame)
                    if (chunkSize % wavFile.blockAlign != 0L) throw Exception("Data Chunk size is not multiple of Block Align")

                    // Calculate the number of frames
                    wavFile.numFrames = chunkSize / wavFile.blockAlign

                    // Flag that we've found the wave data chunk
                    foundData = true
                    break
                } else {
                    // If an unknown chunk ID is found, just skip over the chunk data
                    wavFile.iStream!!.skip(numChunkBytes)
                }
            }

            // Throw an exception if no data chunk has been found
            if (foundData == false) throw Exception("Did not find a data chunk")

            // Calculate the scaling factor for converting to a normalised double
            if (wavFile.validBits > 8) {
                // If more than 8 validBits, data is signed
                // Conversion required dividing by magnitude of max negative value
                wavFile.floatOffset = 0f
                wavFile.floatScale = (1 shl wavFile.validBits - 1).toFloat()
            } else {
                // Else if 8 or less validBits, data is unsigned
                // Conversion required dividing by max positive value
                wavFile.floatOffset = -1f
                wavFile.floatScale = 0.5f * ((1 shl wavFile.validBits) - 1)
            }
            wavFile.bufferPointer = 0
            wavFile.bytesRead = 0
            wavFile.frameCounter = 0
            wavFile.ioState = IOState.READING
            return wavFile
        }

        private fun getLE(buffer: ByteArray, pos: Int, numBytes: Int): Long {
            var pos = pos
            var numBytes = numBytes
            numBytes--
            pos += numBytes
            var `val`: Long = (buffer[pos] and 0xFF.toByte()).toLong()
            for (b in 0 until numBytes) `val` = (`val` shl 8) + (buffer[--pos] and 0xFF.toByte())
            return `val`
        }
    }

    // Cannot instantiate WavFile directly, must either use newWavFile() or openWavFile()
    init {
        buffer = ByteArray(BUFFER_SIZE)
    }
}