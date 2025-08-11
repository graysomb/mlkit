package com.google.mlkit.vision.demo

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.demo.java.posedetector.PoseDetectorProcessor
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var graphicOverlay: GraphicOverlay
    private lateinit var poseDetectorProcessor: PoseDetectorProcessor
    private lateinit var retriever: MediaMetadataRetriever
    private val REQUEST_CODE_SELECT_VIDEO = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        imageView = findViewById(R.id.image_view)
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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SELECT_VIDEO && resultCode == RESULT_OK) {
            val videoUri: Uri? = data?.data
            videoUri?.let {
                retriever = MediaMetadataRetriever()
                retriever.setDataSource(this, it)
                processVideo(it)
            }
        }
    }

    private fun processVideo(videoUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
                val frameRate = 30
                val interval = (1000 / frameRate).toLong()

                for (i in 0 until duration step interval) {
                    val frame: Bitmap? = retriever.getFrameAtTime(i * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    frame?.let {
                        withContext(Dispatchers.Main) {
                            graphicOverlay.setImageSourceInfo(it.width, it.height, false)
                            imageView.setImageBitmap(it)
                            graphicOverlay.clear()
                            poseDetectorProcessor.processBitmap(it, graphicOverlay)
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }
        }
    }
}
