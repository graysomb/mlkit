package com.google.mlkit.vision.demo

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Choreographer
import android.widget.Button
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.demo.java.posedetector.PoseDetectorProcessor
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

class VideoActivity : AppCompatActivity(), Choreographer.FrameCallback {

    private lateinit var videoView: VideoView
    private lateinit var graphicOverlay: GraphicOverlay
    private lateinit var poseDetectorProcessor: PoseDetectorProcessor
    private lateinit var retriever: MediaMetadataRetriever
    private var isProcessing = false
    private val REQUEST_CODE_SELECT_VIDEO = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        videoView = findViewById(R.id.video_view)
        graphicOverlay = findViewById(R.id.graphic_overlay)
        val selectVideoButton = findViewById<Button>(R.id.select_video_button)

        selectVideoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_CODE_SELECT_VIDEO)
        }

        // Initialize the PoseDetectorProcessor
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        poseDetectorProcessor = PoseDetectorProcessor(this, options, false, false, false, false, true)

        videoView.setOnPreparedListener {
            isProcessing = true
            Choreographer.getInstance().postFrameCallback(this)
        }

        videoView.setOnCompletionListener {
            isProcessing = false
            retriever.release()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SELECT_VIDEO && resultCode == RESULT_OK) {
            val videoUri: Uri? = data?.data
            videoView.setVideoURI(videoUri)
            videoView.start()

            videoUri?.let {
                retriever = MediaMetadataRetriever()
                retriever.setDataSource(this, it)
            }
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isProcessing) {
            return
        }

        val currentPosition = videoView.currentPosition.toLong()
        val frame: Bitmap? = retriever.getFrameAtTime(currentPosition * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

        frame?.let {
            graphicOverlay.clear()
            poseDetectorProcessor.processBitmap(it, graphicOverlay)
        }

        Choreographer.getInstance().postFrameCallback(this)
    }
}
