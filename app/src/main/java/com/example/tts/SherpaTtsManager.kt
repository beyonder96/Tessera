package com.example.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

sealed interface SherpaModelStatus {
    data object NotDownloaded : SherpaModelStatus
    data class Downloading(val progressPercent: Int) : SherpaModelStatus
    data object Ready : SherpaModelStatus
    data class Error(val message: String) : SherpaModelStatus
}

class SherpaTtsManager(private val context: Context) {

    companion object {
        private const val TAG = "SherpaTtsManager"
        private const val MODEL_FOLDER = "vits-piper-pt_BR-edresson-low-int8"
        private const val MODEL_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-pt_BR-edresson-low-int8.tar.bz2"
    }

    private val baseModelsDir: File by lazy {
        File(context.filesDir, "sherpa_models").apply { mkdirs() }
    }

    private val targetModelDir: File by lazy {
        File(baseModelsDir, MODEL_FOLDER)
    }

    private val modelOnnxFile: File by lazy {
        File(targetModelDir, "pt_BR-edresson-low.onnx")
    }

    private val tokensFile: File by lazy {
        File(targetModelDir, "tokens.txt")
    }

    private val espeakDir: File by lazy {
        File(targetModelDir, "espeak-ng-data")
    }

    private val _modelStatus = MutableStateFlow<SherpaModelStatus>(SherpaModelStatus.NotDownloaded)
    val modelStatus: StateFlow<SherpaModelStatus> = _modelStatus.asStateFlow()

    private var offlineTts: OfflineTts? = null
    private var currentAudioTrack: AudioTrack? = null
    @Volatile
    private var isPlayingRequested = false

    init {
        checkInitialStatus()
    }

    fun isModelReady(): Boolean = _modelStatus.value is SherpaModelStatus.Ready

    private fun checkInitialStatus() {
        if (hasAllModelFiles()) {
            _modelStatus.value = SherpaModelStatus.Ready
        } else {
            _modelStatus.value = SherpaModelStatus.NotDownloaded
        }
    }

    private fun hasAllModelFiles(): Boolean {
        return modelOnnxFile.exists() && modelOnnxFile.length() > 1000L &&
                tokensFile.exists() && tokensFile.length() > 0L &&
                espeakDir.exists() && espeakDir.isDirectory
    }

    suspend fun initializeEngine(): Boolean = withContext(Dispatchers.IO) {
        if (offlineTts != null) return@withContext true
        if (!hasAllModelFiles()) {
            _modelStatus.value = SherpaModelStatus.NotDownloaded
            return@withContext false
        }

        try {
            val vitsConfig = OfflineTtsVitsModelConfig(
                model = modelOnnxFile.absolutePath,
                tokens = tokensFile.absolutePath,
                dataDir = espeakDir.absolutePath,
                noiseScale = 0.667f,
                noiseScaleW = 0.8f,
                lengthScale = 1.0f
            )

            val modelConfig = OfflineTtsModelConfig(
                vits = vitsConfig,
                numThreads = 2,
                debug = false,
                provider = "cpu"
            )

            val config = OfflineTtsConfig(
                model = modelConfig,
                maxNumSentences = 1
            )

            offlineTts = OfflineTts(assetManager = null, config = config)
            _modelStatus.value = SherpaModelStatus.Ready
            Log.i(TAG, "Sherpa-ONNX OfflineTts inicializado com sucesso!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar OfflineTts", e)
            _modelStatus.value = SherpaModelStatus.Error("Falha ao inicializar o motor neural: ${e.message}")
            false
        }
    }

    suspend fun downloadAndPrepareModel(): Boolean = withContext(Dispatchers.IO) {
        if (hasAllModelFiles()) {
            _modelStatus.value = SherpaModelStatus.Ready
            return@withContext initializeEngine()
        }

        _modelStatus.value = SherpaModelStatus.Downloading(0)
        val tempArchive = File(baseModelsDir, "model_download.tar.bz2")

        try {
            // 1. Download com medição de progresso (0 - 70%)
            val url = URL(MODEL_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 20000
                readTimeout = 30000
                instanceFollowRedirects = true
            }

            if (connection.responseCode !in 200..299) {
                throw Exception("Falha na conexão HTTP (${connection.responseCode}) ao baixar o modelo neural.")
            }

            val totalSize = connection.contentLength.toLong()
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(tempArchive).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalSize > 0) {
                            val percent = ((downloadedBytes * 70) / totalSize).toInt().coerceIn(0, 70)
                            _modelStatus.value = SherpaModelStatus.Downloading(percent)
                        }
                    }
                    output.flush()
                }
            }

            // 2. Extração do pacote .tar.bz2 (70% - 100%)
            _modelStatus.value = SherpaModelStatus.Downloading(75)
            extractTarBz2(tempArchive, baseModelsDir)
            _modelStatus.value = SherpaModelStatus.Downloading(95)

            // Limpa o arquivo compactado após a extração
            tempArchive.delete()

            if (!hasAllModelFiles()) {
                throw Exception("Arquivo de pesos ou fonemas ausente após extração.")
            }

            _modelStatus.value = SherpaModelStatus.Downloading(100)
            val success = initializeEngine()
            if (success) {
                _modelStatus.value = SherpaModelStatus.Ready
            }
            success
        } catch (e: Exception) {
            tempArchive.delete()
            Log.e(TAG, "Falha durante download ou extração do modelo neural", e)
            _modelStatus.value = SherpaModelStatus.Error(e.message ?: "Erro ao baixar modelo neural.")
            false
        }
    }

    private fun extractTarBz2(archiveFile: File, outputDir: File) {
        archiveFile.inputStream().use { fis ->
            BufferedInputStream(fis).use { bis ->
                BZip2CompressorInputStream(bis).use { bz2In ->
                    TarArchiveInputStream(bz2In).use { tarIn ->
                        var entry = tarIn.nextTarEntry
                        while (entry != null) {
                            val outputFile = File(outputDir, entry.name)
                            if (entry.isDirectory) {
                                outputFile.mkdirs()
                            } else {
                                outputFile.parentFile?.mkdirs()
                                FileOutputStream(outputFile).use { fos ->
                                    tarIn.copyTo(fos)
                                }
                            }
                            entry = tarIn.nextTarEntry
                        }
                    }
                }
            }
        }
    }

    /**
     * Sintetiza e reproduz o texto de um versículo.
     * Retorna true se a reprodução completou normalmente, false se foi cancelada.
     */
    suspend fun speakText(text: String, speed: Float = 1.0f): Boolean = withContext(Dispatchers.IO) {
        if (offlineTts == null) {
            val initialized = initializeEngine()
            if (!initialized) return@withContext false
        }

        val engine = offlineTts ?: return@withContext false
        isPlayingRequested = true

        try {
            // Sintetiza áudio no CPU via Sherpa-ONNX
            val cleanText = text.trim()
            val audio = engine.generate(text = cleanText, sid = 0, speed = speed)
            if (!isPlayingRequested) return@withContext false

            val samples = audio.samples
            val sampleRate = audio.sampleRate
            if (samples.isEmpty()) return@withContext true

            // Converte amostras Float (-1.0 a 1.0) para PCM 16-bit
            val pcmData = ShortArray(samples.size)
            for (i in samples.indices) {
                val clamped = samples[i].coerceIn(-1.0f, 1.0f)
                pcmData[i] = (clamped * 32767).toInt().toShort()
            }

            // Reprodução via AudioTrack
            playPcmAudio(pcmData, sampleRate)
        } catch (e: CancellationException) {
            stopAudioPlayback()
            false
        } catch (e: Exception) {
            Log.e(TAG, "Erro na síntese do versículo", e)
            false
        }
    }

    private suspend fun playPcmAudio(pcmData: ShortArray, sampleRate: Int): Boolean = withContext(Dispatchers.IO) {
        stopAudioPlayback()
        if (!isPlayingRequested) return@withContext false

        val bufferSize = pcmData.size * 2
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao instanciar AudioTrack", e)
            return@withContext false
        }

        currentAudioTrack = track
        track.write(pcmData, 0, pcmData.size)
        track.play()

        // Calcula a duração total em ms para aguardar a reprodução terminar
        val durationMs = (pcmData.size.toDouble() / sampleRate * 1000).toLong()
        val stepMs = 50L
        var elapsed = 0L

        while (isPlayingRequested && elapsed < durationMs) {
            kotlinx.coroutines.delay(stepMs)
            elapsed += stepMs
            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) break
        }

        try {
            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.stop()
                track.release()
            }
        } catch (_: Exception) {}

        currentAudioTrack = null
        isPlayingRequested
    }

    fun stop() {
        isPlayingRequested = false
        stopAudioPlayback()
    }

    private fun stopAudioPlayback() {
        try {
            currentAudioTrack?.let { track ->
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.pause()
                    track.flush()
                    track.stop()
                    track.release()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao parar AudioTrack: ${e.message}")
        } finally {
            currentAudioTrack = null
        }
    }

    fun release() {
        stop()
        offlineTts?.release()
        offlineTts = null
    }
}
