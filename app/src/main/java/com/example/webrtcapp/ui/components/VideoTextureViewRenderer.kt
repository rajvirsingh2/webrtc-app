package com.example.webrtcapp.ui.components

import android.content.Context
import android.content.res.Resources
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.TextureView
import androidx.compose.runtime.Composable
import org.webrtc.EglBase
import org.webrtc.EglRenderer
import org.webrtc.GlRectDrawer
import org.webrtc.RendererCommon
import org.webrtc.ThreadUtils
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import java.util.concurrent.CountDownLatch

open class VideoTextureViewRenderer @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null
): TextureView(context, attributes), VideoSink, TextureView.SurfaceTextureListener{
    private val resourceName: String = getResourceName()
    private val eglRenderer: EglRenderer = EglRenderer(resourceName)
    private var rendererEvents: RendererCommon.RendererEvents? = null
    private val uiThreadHandler = Handler(Looper.getMainLooper())
    private var isFirstFrameRendered = false
    private var rotatedFrameHeight = 0
    private var rotatedFrameWidth = 0
    private var frameRotation = 0

    init{
        surfaceTextureListener = this
    }

    override fun onFrame(p0: VideoFrame) {
        eglRenderer.onFrame(p0)
        updateFrameData(p0)
    }

    private fun getResourceName(): String{
        return try{
            resources.getResourceEntryName(id)+":"
        }catch (e: Resources.NotFoundException){
            ""
        }
    }

    private fun updateFrameData(frame: VideoFrame){
        if(isFirstFrameRendered){
            rendererEvents?.onFirstFrameRendered()
            isFirstFrameRendered = true
        }
        if(frame.rotatedWidth != rotatedFrameWidth ||
            frame.rotatedHeight != rotatedFrameHeight ||
            frame.rotation != frameRotation
            ){
            rotatedFrameWidth = frame.rotatedWidth
            rotatedFrameHeight = frame.rotatedHeight
            frameRotation = frame.rotation

            uiThreadHandler.post {
                rendererEvents?.onFrameResolutionChanged(
                    rotatedFrameWidth,
                    rotatedFrameHeight,
                    frameRotation
                )
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        eglRenderer.setLayoutAspectRatio((right-left)/(bottom.toFloat()-top))
    }

    fun init(
        sharedContext: EglBase.Context,
        rendererEvents: RendererCommon.RendererEvents
    ){
        ThreadUtils.checkIsOnMainThread()
        this.rendererEvents = rendererEvents
        eglRenderer.init(sharedContext, EglBase.CONFIG_PLAIN, GlRectDrawer())
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        eglRenderer.createEglSurface(surfaceTexture)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        val completionLatch = CountDownLatch(1)
        eglRenderer.releaseEglSurface { completionLatch.countDown() }
        ThreadUtils.awaitUninterruptibly(completionLatch)
        return true
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    override fun onDetachedFromWindow() {
        eglRenderer.release()
        super.onDetachedFromWindow()
    }
}