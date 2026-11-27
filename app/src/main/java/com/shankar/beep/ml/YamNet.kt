package com.shankar.beep.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/** Anything that turns ~1 s of audio into one probability per AudioSet class. */
interface SoundClassifier {
    /** Number of samples (16 kHz) the classifier wants per call. */
    val windowSize: Int
    val labels: List<String>
    fun classify(window: FloatArray): FloatArray
    fun close() {}
}

/**
 * Google's YAMNet (MobileNet v1 trained on AudioSet) running locally through
 * TensorFlow Lite. Input: 15 600 samples (0.975 s) at 16 kHz in -1..1.
 * Output: 521 class scores in 0..1.
 */
class YamNet(context: Context) : SoundClassifier {

    override val windowSize: Int = WINDOW
    override val labels: List<String> = loadLabels(context)

    private val interpreter: Interpreter
    private val input: ByteBuffer
    private val output: ByteBuffer
    private val classCount: Int

    init {
        val options = Interpreter.Options().apply { setNumThreads(2) }
        interpreter = Interpreter(loadModel(context), options)
        val inputShape = interpreter.getInputTensor(0).shape()
        if (inputShape.fold(1) { a, b -> a * b } != WINDOW) {
            interpreter.resizeInput(0, intArrayOf(WINDOW))
        }
        interpreter.allocateTensors()
        input = ByteBuffer.allocateDirect(WINDOW * 4).order(ByteOrder.nativeOrder())
        val outTensor = interpreter.getOutputTensor(0)
        classCount = outTensor.shape().last()
        output = ByteBuffer.allocateDirect(outTensor.numBytes()).order(ByteOrder.nativeOrder())
    }

    @Synchronized
    override fun classify(window: FloatArray): FloatArray {
        require(window.size == WINDOW) { "YAMNet needs exactly $WINDOW samples" }
        input.rewind()
        input.asFloatBuffer().put(window)
        output.rewind()
        interpreter.run(input, output)
        output.rewind()
        val floats = output.asFloatBuffer()
        // Some exports emit one row per 0.48 s patch; average the rows if so.
        val rows = floats.remaining() / classCount
        val scores = FloatArray(classCount)
        for (r in 0 until rows) for (c in 0 until classCount) scores[c] += floats.get()
        if (rows > 1) for (c in 0 until classCount) scores[c] /= rows
        return scores
    }

    @Synchronized
    override fun close() = interpreter.close()

    companion object {
        const val WINDOW = 15_600
        private const val MODEL = "yamnet.tflite"
        private const val LABELS = "yamnet_class_map.csv"

        private fun loadModel(context: Context): MappedByteBuffer {
            context.assets.openFd(MODEL).use { fd ->
                FileInputStream(fd.fileDescriptor).use { stream ->
                    return stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }
        }

        private fun loadLabels(context: Context): List<String> =
            context.assets.open(LABELS).bufferedReader().useLines { lines ->
                lines.drop(1).map { line -> parseCsvName(line) }.toList()
            }

        /** Reads the display_name column, honouring quoted values with commas. */
        internal fun parseCsvName(line: String): String {
            val firstComma = line.indexOf(',')
            val secondComma = line.indexOf(',', firstComma + 1)
            if (secondComma < 0) return line
            return line.substring(secondComma + 1).trim().removeSurrounding("\"")
        }
    }
}
