package com.ahmed.vr

import android.util.Log
import com.google.ar.core.AugmentedImage
import com.google.ar.core.Anchor

/**
 * AugmentedImageManager: manage a single Anchor per AugmentedImage.
 * Creates the anchor when the image enters TRACKING and reuses/detaches on loss.
 */
class AugmentedImageManager {
    companion object {
        private const val TAG = "AugmentedImageMgr"
    }

    // Map from augmented image index to anchor
    private val imageAnchors = mutableMapOf<Int, Anchor>()

    fun update(images: Collection<AugmentedImage>) {
        for (image in images) {
            val index = image.index
            when (image.trackingState) {
                com.google.ar.core.TrackingState.TRACKING -> {
                    if (!imageAnchors.containsKey(index)) {
                        try {
                            val anchor = image.createAnchor(image.centerPose)
                            imageAnchors[index] = anchor
                            Log.i(TAG, "Created anchor for augmented image index=$index, name=${image.name}")
                        } catch (t: Throwable) {
                            Log.w(TAG, "Failed to create anchor for augmented image $index: ${t.message}")
                        }
                    }
                }
                com.google.ar.core.TrackingState.PAUSED, com.google.ar.core.TrackingState.STOPPED -> {
                    // If tracking lost or stopped, detach and remove anchor
                    if (imageAnchors.containsKey(index)) {
                        try {
                            imageAnchors[index]?.detach()
                        } catch (t: Throwable) { /* best effort */ }
                        imageAnchors.remove(index)
                        Log.i(TAG, "Removed anchor for augmented image index=$index due to tracking loss")
                    }
                }
                else -> {
                    // no-op
                }
            }
        }
    }

    fun getAnchorForImage(index: Int): Anchor? = imageAnchors[index]

    fun clearAll() {
        for (a in imageAnchors.values) {
            try { a.detach() } catch (_: Throwable) { }
        }
        imageAnchors.clear()
    }
}
