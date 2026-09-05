package xyz.skifty.mani.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sqrt

private const val TAG = "AudioSessionVisualizer"

// A slowly-decaying running max across all bars, used to normalize raw FFT magnitude sums (which
// have no fixed absolute scale) into a 0f..1f range - see SpectrumAccumulator.normalizeAndSmooth().
private const val RUNNING_MAX_DECAY = 0.98f

// Floor for the running-max denominator above, so near-silence doesn't divide a tiny sum by an
// equally tiny max and produce a wildly unstable ratio.
private const val RUNNING_MAX_FLOOR = 1f

// Frame-to-frame exponential smoothing weight on the previous value, applied per bar before it
// reaches VisualizerState - keeps bar length changes smooth rather than jittery.
private const val SMOOTHING_FACTOR = 0.7f

// Lower bound of the log-frequency mapping below - roughly the bottom of human hearing, and (not
// coincidentally) also what keeps ln() away from -infinity for the FFT's DC bin, whose own
// frequency is exactly 0Hz.
private const val MIN_FREQUENCY_HZ = 20f

// Passed to Visualizer.setCaptureSize() - clamped into the device's own supported range, since
// that range (and which powers of two within it are valid) is device-dependent.
private const val DESIRED_CAPTURE_SIZE = 1024

// Passed to Visualizer.setDataCaptureListener() - clamped against the device's own
// getMaxCaptureRate(), in milliHertz per that API's own units.
private const val TARGET_CAPTURE_RATE_MILLIHERTZ = 30_000

/** Accumulates raw FFT magnitude into [VISUALIZER_SEGMENTS_PER_RING] independent bars spanning the
 *  whole captured spectrum, then normalizes/smooths that into what [VisualizerState] actually
 *  publishes. A single shared running max across all bars (not one per bar) is deliberate -
 *  normalizing each bar against its own independent max would flatten out the relative differences
 *  between bars that make the spectrum look like a spectrum in the first place. */
private class SpectrumAccumulator {

    private val sums = FloatArray(VISUALIZER_SEGMENTS_PER_RING)
    private val smoothed = FloatArray(VISUALIZER_SEGMENTS_PER_RING)
    private var runningMax = RUNNING_MAX_FLOOR

    fun resetFrame() {
        sums.fill(0f)
    }

    /** Clears everything, including the smoothing/running-max memory - not just the current
     *  frame's sums like [resetFrame] - so a paused-then-resumed capture starts clean instead of
     *  smoothing back toward whatever levels were playing right before the pause. */
    fun clearHistory() {
        sums.fill(0f)
        smoothed.fill(0f)
        runningMax = RUNNING_MAX_FLOOR
    }

    fun accumulate(
        segmentIndex: Int,
        magnitude: Float,
    ) {
        sums[segmentIndex] += magnitude
    }

    fun normalizeAndSmooth(): FloatArray {
        runningMax = maxOf(runningMax * RUNNING_MAX_DECAY, sums.max())
        val denominator = runningMax.coerceAtLeast(RUNNING_MAX_FLOOR)
        for (index in smoothed.indices) {
            val normalized = (sums[index] / denominator).coerceIn(0f, 1f)
            smoothed[index] = smoothed[index] * SMOOTHING_FACTOR + normalized * (1f - SMOOTHING_FACTOR)
        }
        return smoothed.copyOf()
    }

}

/** Owns the actual [Visualizer] instance capturing [PlaybackService]'s ExoPlayer output, mapping
 *  each captured FFT frame onto a single spectrum (see [SpectrumAccumulator]) published on
 *  [visualizerState] - every ring in AudioVisualizer draws this same spectrum. Bin-to-bar mapping is
 *  log-scaled (not linear) across [MIN_FREQUENCY_HZ, Nyquist) - music's energy skews heavily toward
 *  the low end, so a linear mapping would leave all but the first couple of bars permanently quiet;
 *  log spacing keeps the whole ring visually active, matching how real spectrum-analyzer UIs bucket
 *  frequencies. Every native-API-facing call is wrapped in [runCatching] - an OEM quirk or a revoked
 *  permission should degrade to flat (all-zero) bars, never a crash and never a silent revert back
 *  to cover art (see AudioVisualizer's own doc comment). Ring rotation itself is not driven from
 *  here at all - it's a plain constant-speed animation owned by the composable, wholly decoupled
 *  from whether real capture is even working. */
class AudioSessionVisualizer(
    private val context: Context,
    private val visualizerState: VisualizerState,
) {

    private var visualizer: Visualizer? = null
    private var capturing = false
    private var hasLoggedFirstFrame = false

    private val spectrum = SpectrumAccumulator()

    private val captureListener = object : Visualizer.OnDataCaptureListener {

        override fun onWaveFormDataCapture(
            source: Visualizer?,
            waveform: ByteArray?,
            samplingRate: Int,
        ) {
            // Unused - only FFT capture was requested (see attach() below), but the interface
            // requires overriding both callbacks.
        }

        override fun onFftDataCapture(
            source: Visualizer?,
            fft: ByteArray?,
            samplingRate: Int,
        ) {
            fft?.let { frame ->
                processFft(
                    fft = frame,
                    samplingRateMilliHertz = samplingRate,
                )
            }
        }

    }

    /** (Re-)attaches to [audioSessionId] - [PlaybackService] calls this once, right after
     *  generating and assigning that id itself (see its own doc comment for why it doesn't rely on
     *  Player.Listener's onAudioSessionIdChanged for this). Capture starts in whatever enabled
     *  state [setCaptureEnabled] was last asked for; that method is the one actually tied to
     *  playback state. */
    fun attach(
        audioSessionId: Int,
    ) {
        release()

        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) {
            Log.w(TAG, "attach() called with an unset audio session id - staying idle.")
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            // Nothing to attach to without the permission - AudioVisualizer still renders (flat,
            // all-zero bars) rather than crashing or silently reverting to cover art.
            Log.w(TAG, "RECORD_AUDIO not granted - staying idle.")
            return
        }

        runCatching {
            val captureSizeRange = Visualizer.getCaptureSizeRange()
            val captureRateMilliHertz = TARGET_CAPTURE_RATE_MILLIHERTZ.coerceAtMost(Visualizer.getMaxCaptureRate())
            Visualizer(audioSessionId).apply {
                captureSize = DESIRED_CAPTURE_SIZE.coerceIn(captureSizeRange[0], captureSizeRange[1])
                setDataCaptureListener(
                    /* listener = */ captureListener,
                    /* rate = */ captureRateMilliHertz,
                    /* waveform = */ false,
                    /* fft = */ true,
                )
                enabled = capturing
            }
        }
            .onSuccess { created ->
                visualizer = created
                Log.d(TAG, "Attached to audio session $audioSessionId, captureSize=${created.captureSize}.")
            }
            .onFailure { error ->
                Log.w(TAG, "Failed to attach to audio session $audioSessionId.", error)
            }
    }

    /** Ties data capture to actual playback state - paused/stopped means no capture, matching what
     *  [AndroidAudioPlayer]'s `isPlaying` already reflects to the UI. */
    fun setCaptureEnabled(
        enabled: Boolean,
    ) {
        if (enabled == capturing) {
            return
        }
        capturing = enabled
        Log.d(TAG, "setCaptureEnabled($enabled), visualizer attached=${visualizer != null}")
        runCatching { visualizer?.enabled = enabled }
        if (!enabled) {
            // Paused - no more FFT frames means visualizerState.bars would otherwise just sit
            // frozen at whatever it last held, mid-song. Explicitly drop it back to its resting
            // (all-zero) state, and clear the smoothing memory too so a later resume starts clean
            // instead of smoothing back toward pre-pause levels.
            spectrum.clearHistory()
            visualizerState.bars = FloatArray(VISUALIZER_SEGMENTS_PER_RING)
        }
    }

    fun release() {
        runCatching { visualizer?.release() }
        visualizer = null
        capturing = false
        hasLoggedFirstFrame = false
    }

    private fun processFft(
        fft: ByteArray,
        samplingRateMilliHertz: Int,
    ) {
        // Guards against a real race, not a hypothetical one: Visualizer.setEnabled(false) does not
        // retroactively cancel a capture callback that was already queued on the main thread right
        // before we disabled it, so a stray last frame can still land here just after
        // setCaptureEnabled(false) has already reset visualizerState.bars back to resting -
        // silently un-resetting it. Bailing out here if we're not supposed to be capturing anymore
        // is what actually makes that reset stick every time, not just most of the time.
        if (!capturing) {
            return
        }

        val binCount = fft.size / 2
        if (binCount < 2) {
            return
        }

        if (!hasLoggedFirstFrame) {
            hasLoggedFirstFrame = true
            Log.d(TAG, "First FFT frame captured (${fft.size} bytes, sampling rate ${samplingRateMilliHertz}mHz).")
        }

        val samplingRateHz = samplingRateMilliHertz / 1000f
        val nyquistHz = samplingRateHz / 2f
        val hzPerBin = samplingRateHz / fft.size
        val logMinFrequency = ln(MIN_FREQUENCY_HZ)
        val logMaxFrequency = ln(nyquistHz.coerceAtLeast(MIN_FREQUENCY_HZ * 2f))
        val logRange = logMaxFrequency - logMinFrequency

        // Every "regular" (non-DC, non-Nyquist) bin's magnitude, kept as a plain array rather than
        // consumed inline in a single pass - the empty-bucket backfill pass further down needs to
        // look bins up out of order, after the fact.
        val regularBinMagnitudes = FloatArray(binCount)
        for (bin in 1 until binCount) {
            val real = fft[2 * bin].toInt().toFloat()
            val imaginary = fft[2 * bin + 1].toInt().toFloat()
            regularBinMagnitudes[bin] = hypot(real, imaginary)
        }

        fun bucketIndexFor(frequencyHz: Float): Int {
            val clampedFrequency = frequencyHz.coerceAtLeast(MIN_FREQUENCY_HZ)
            val fraction = ((ln(clampedFrequency) - logMinFrequency) / logRange).coerceIn(0f, 1f)
            return (fraction * VISUALIZER_SEGMENTS_PER_RING).toInt()
                .coerceIn(0, VISUALIZER_SEGMENTS_PER_RING - 1)
        }

        // A genuine interpolation (weighted average of the two real bins nearest frequencyHz), not
        // a split of either bin's own magnitude - only used below to backfill a bucket that no bin
        // was ever assigned to, so it never dims a bucket that already has a real bin of its own.
        fun interpolatedMagnitudeAt(frequencyHz: Float): Float {
            val continuousBin = (frequencyHz / hzPerBin).coerceIn(1f, (binCount - 1).toFloat())
            val lowerBin = continuousBin.toInt().coerceIn(1, binCount - 1)
            val upperBin = (lowerBin + 1).coerceAtMost(binCount - 1)
            val upperWeight = (continuousBin - lowerBin).coerceIn(0f, 1f)
            return regularBinMagnitudes[lowerBin] * (1f - upperWeight) + regularBinMagnitudes[upperBin] * upperWeight
        }

        spectrum.resetFrame()

        // getFft()'s documented layout packs DC and Nyquist specially, both real-valued, at
        // indices 0 and 1 - added straight to the bottom/top bucket. Real audio typically has
        // ~zero energy at either (no DC offset, and anti-aliasing rolls off before Nyquist), so this
        // alone rarely moves those two bars much - the two passes below are what actually give every
        // bucket, these two included, a proper value from the real spectrum.
        spectrum.accumulate(segmentIndex = 0, magnitude = abs(fft[0].toInt().toFloat()))
        spectrum.accumulate(
            segmentIndex = VISUALIZER_SEGMENTS_PER_RING - 1,
            magnitude = abs(fft[1].toInt().toFloat()),
        )

        // Pass 1: assign each regular bin to its single nearest bucket and sum - never split a bin
        // between two buckets, so one bucket's value is never diminished just to feed another.
        val binsInBucket = IntArray(VISUALIZER_SEGMENTS_PER_RING)
        for (bin in 1 until binCount) {
            val bucketIndex = bucketIndexFor(bin * hzPerBin)
            spectrum.accumulate(segmentIndex = bucketIndex, magnitude = regularBinMagnitudes[bin])
            binsInBucket[bucketIndex]++
        }

        // Pass 2: any bucket the loop above never touched sits where the bars' own log spacing is
        // narrower than the FFT's linear bin spacing (only possible near the very bottom) - e.g. bar
        // 2 used to sit exactly between bin #1 (~47Hz, landing in bar 1) and bin #2 (~94Hz, landing
        // in bar 3), so it never received anything at all. Interpolating the spectrum at that
        // bucket's own center frequency is what actually fills a gap like that, rather than either
        // leaving it at zero or stealing part of a neighboring bucket's own bin.
        for (bucketIndex in 0 until VISUALIZER_SEGMENTS_PER_RING) {
            if (binsInBucket[bucketIndex] > 0) {
                continue
            }
            val bandStartHz = exp(logMinFrequency + logRange * bucketIndex / VISUALIZER_SEGMENTS_PER_RING)
            val bandEndHz = exp(logMinFrequency + logRange * (bucketIndex + 1) / VISUALIZER_SEGMENTS_PER_RING)
            val centerHz = sqrt(bandStartHz * bandEndHz)
            spectrum.accumulate(segmentIndex = bucketIndex, magnitude = interpolatedMagnitudeAt(centerHz))
        }

        visualizerState.bars = spectrum.normalizeAndSmooth()
    }

}
