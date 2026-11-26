package com.example.ui.components

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.ActionGlassBg
import com.example.ui.theme.TextPrimary

@Composable
fun ReelVideoPlayer(
    videoSource: String,
    isActive: Boolean,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mediaPlayer by remember(videoSource) { mutableStateOf<MediaPlayer?>(null) }
    var isPrepared by remember(videoSource) { mutableStateOf(false) }
    var isUserPaused by remember(videoSource) { mutableStateOf(false) }
    var isLifecycleResumed by remember { mutableStateOf(true) }
    var videoWidth by remember(videoSource) { mutableIntStateOf(0) }
    var videoHeight by remember(videoSource) { mutableIntStateOf(0) }
    var textureViewRef by remember(videoSource) { mutableStateOf<TextureView?>(null) }
    var playbackFailed by remember(videoSource) { mutableStateOf(false) }

    fun applyCenterCrop(tv: TextureView?, vw: Int, vh: Int) {
        if (tv == null || vw <= 0 || vh <= 0 || tv.width <= 0 || tv.height <= 0) return
        val viewWidth = tv.width.toFloat()
        val viewHeight = tv.height.toFloat()
        val videoAspect = vw.toFloat() / vh.toFloat()
        val viewAspect = viewWidth / viewHeight

        val scaleX: Float
        val scaleY: Float
        if (videoAspect > viewAspect) {
            scaleX = videoAspect / viewAspect
            scaleY = 1f
        } else {
            scaleX = 1f
            scaleY = viewAspect / videoAspect
        }

        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
        tv.setTransform(matrix)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isLifecycleResumed = true
                Lifecycle.Event.ON_PAUSE -> isLifecycleResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isActive, isUserPaused, isLifecycleResumed, isPrepared, isMuted, mediaPlayer) {
        val mp = mediaPlayer ?: return@LaunchedEffect
        if (!isPrepared) return@LaunchedEffect
        try {
            val vol = if (isMuted) 0f else 1f
            mp.setVolume(vol, vol)
            val shouldPlay = isActive && !isUserPaused && isLifecycleResumed
            if (shouldPlay && !mp.isPlaying) {
                mp.start()
            } else if (!shouldPlay && mp.isPlaying) {
                mp.pause()
            }
        } catch (_: IllegalStateException) {
        }
    }

    DisposableEffect(videoSource) {
        onDispose {
            try {
                mediaPlayer?.stop()
            } catch (_: Exception) {
            }
            try {
                mediaPlayer?.release()
            } catch (_: Exception) {
            }
            mediaPlayer = null
        }
    }

    if (!playbackFailed) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isUserPaused = !isUserPaused
                }
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    TextureView(ctx).apply {
                        textureViewRef = this
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                surfaceTexture: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {
                                try {
                                    val surface = Surface(surfaceTexture)
                                    val player = MediaPlayer().apply {
                                        setSurface(surface)
                                        isLooping = true
                                        setOnPreparedListener { mp ->
                                            videoWidth = mp.videoWidth
                                            videoHeight = mp.videoHeight
                                            applyCenterCrop(textureViewRef, videoWidth, videoHeight)
                                            isPrepared = true
                                            val vol = if (isMuted) 0f else 1f
                                            mp.setVolume(vol, vol)
                                            if (isActive && !isUserPaused && isLifecycleResumed) {
                                                mp.start()
                                            }
                                        }
                                        setOnVideoSizeChangedListener { _, w, h ->
                                            videoWidth = w
                                            videoHeight = h
                                            applyCenterCrop(textureViewRef, w, h)
                                        }
                                        setOnErrorListener { _, _, _ ->
                                            playbackFailed = true
                                            true
                                        }
                                        setDataSource(context, Uri.parse(videoSource))
                                        prepareAsync()
                                    }
                                    mediaPlayer = player
                                } catch (_: Exception) {
                                    playbackFailed = true
                                }
                            }

                            override fun onSurfaceTextureSizeChanged(
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {
                                applyCenterCrop(textureViewRef, videoWidth, videoHeight)
                            }

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                try {
                                    mediaPlayer?.release()
                                } catch (_: Exception) {
                                }
                                mediaPlayer = null
                                isPrepared = false
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                        }
                    }
                }
            )

            if (isUserPaused && isActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ActionGlassBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume video",
                        tint = TextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}
