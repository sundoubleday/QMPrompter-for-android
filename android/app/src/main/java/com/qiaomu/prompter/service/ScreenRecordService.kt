package com.qiaomu.prompter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecordService : Service() {

    companion object {
        const val CHANNEL_ID = "screen_record_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        private var _isRecording = false
        val isRecording: Boolean get() = _isRecording

        private var _outputPath: String? = null
        val outputPath: String? get() = _outputPath

        private var _durationSeconds: Int = 0
        val durationSeconds: Int get() = _durationSeconds

        private var onRecordingComplete: ((String?) -> Unit)? = null

        fun setOnRecordingComplete(callback: ((String?) -> Unit)?) {
            onRecordingComplete = callback
        }
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var startTime: Long = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val data = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                if (data != null) {
                    startRecording(resultCode, data)
                }
            }
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording(resultCode: Int, data: Intent) {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = manager.getMediaProjection(resultCode, data)

        val outputDir = File(getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES), "Recordings")
        outputDir.mkdirs()
        val fileName = "QM_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date())}.mp4"
        val outputFile = File(outputDir, fileName)
        _outputPath = outputFile.absolutePath

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(outputFile.absolutePath)
            setVideoSize(1080, 1920)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncodingBitRate(8 * 1024 * 1024)
            setVideoFrameRate(30)
            setOrientationHint(0)
            prepare()
        }

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenRecord",
            1080, 1920, resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface,
            null, null
        )

        mediaRecorder?.start()
        startTime = System.currentTimeMillis()
        _isRecording = true

        val notification = buildNotification("正在录制...")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopRecording() {
        _durationSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        _isRecording = false

        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {}

        mediaRecorder?.release()
        mediaRecorder = null
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.stop()
        mediaProjection = null

        onRecordingComplete?.invoke(_outputPath)
        onRecordingComplete = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("乔木提词器")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "屏幕录制",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}
