package com.example.mklive.ui.video

import android.graphics.Bitmap
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.mklive.model.VideoStreamMode
import com.example.mklive.video.H264MediaCodecPlayer

@Composable
fun VideoSurfaceView(
    videoMode: VideoStreamMode,
    h264Player: H264MediaCodecPlayer,
    mjpegBitmap: Bitmap?,
    isStreamConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (videoMode == VideoStreamMode.H264_MEDIACODEC) {
            // Hardware H.264 SurfaceView: aspect ratio 16:9 (1280x720 active area)
            AndroidView(
                factory = { context ->
                    SurfaceView(context).apply {
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                h264Player.attachSurface(holder.surface)
                            }

                            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                h264Player.detachSurface()
                            }
                        })
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(16f / 9f)
            )
        } else {
            // Fallback MJPEG Bitmap view
            if (mjpegBitmap != null) {
                Image(
                    bitmap = mjpegBitmap.asImageBitmap(),
                    contentDescription = "Kart Camera Feed",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Overlay if stream disconnected
        if (!isStreamConnected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xBB000000)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.mklive.R.string.video_no_feed),
                    color = Color(0xFFFF9100),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
