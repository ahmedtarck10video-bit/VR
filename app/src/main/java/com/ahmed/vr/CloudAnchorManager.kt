package com.ahmed.vr

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.CloudAnchorState
import com.google.ar.core.Session

/**
 * CloudAnchorManager wraps ARCore Cloud Anchor host/resolve operations.
 * It only reports success when ARCore reports a valid Cloud Anchor state.
 */
class CloudAnchorManager(private val session: Session) {
    companion object {
        private const val TAG = "CloudAnchorManager"
        private const val POLL_MS = 500L
    }

    enum class HostResult { SUCCESS, ERROR }
    enum class ResolveResult { SUCCESS, ERROR }

    interface HostCallback {
        fun onHostComplete(hostedAnchor: Anchor?, result: HostResult, message: String?)
    }

    interface ResolveCallback {
        fun onResolveComplete(resolvedAnchor: Anchor?, result: ResolveResult, message: String?)
    }

    private val handler = Handler(Looper.getMainLooper())

    fun host(anchor: Anchor, callback: HostCallback) {
        try {
            val hosted = session.hostCloudAnchor(anchor)
            // Poll the cloud anchor state until terminal
            handler.post(object : Runnable {
                override fun run() {
                    try {
                        val state = hosted.cloudAnchorState
                        Log.d(TAG, "Cloud host state: $state")
                        when (state) {
                            CloudAnchorState.SUCCESS -> callback.onHostComplete(hosted, HostResult.SUCCESS, null)
                            CloudAnchorState.TASK_IN_PROGRESS, CloudAnchorState.NONE -> handler.postDelayed(this, POLL_MS)
                            else -> callback.onHostComplete(hosted, HostResult.ERROR, state.toString())
                        }
                    } catch (t: Throwable) {
                        callback.onHostComplete(null, HostResult.ERROR, t.message)
                    }
                }
            })
        } catch (t: Throwable) {
            Log.w(TAG, "Host failed: ${t.message}")
            callback.onHostComplete(null, HostResult.ERROR, t.message)
        }
    }

    fun resolve(cloudAnchorId: String, callback: ResolveCallback) {
        try {
            val resolved = session.resolveCloudAnchorId(cloudAnchorId)
            handler.post(object : Runnable {
                override fun run() {
                    try {
                        val state = resolved.cloudAnchorState
                        Log.d(TAG, "Cloud resolve state: $state")
                        when (state) {
                            CloudAnchorState.SUCCESS -> callback.onResolveComplete(resolved, ResolveResult.SUCCESS, null)
                            CloudAnchorState.TASK_IN_PROGRESS, CloudAnchorState.NONE -> handler.postDelayed(this, POLL_MS)
                            else -> callback.onResolveComplete(resolved, ResolveResult.ERROR, state.toString())
                        }
                    } catch (t: Throwable) {
                        callback.onResolveComplete(null, ResolveResult.ERROR, t.message)
                    }
                }
            })
        } catch (t: Throwable) {
            Log.w(TAG, "Resolve failed: ${t.message}")
            callback.onResolveComplete(null, ResolveResult.ERROR, t.message)
        }
    }
}
