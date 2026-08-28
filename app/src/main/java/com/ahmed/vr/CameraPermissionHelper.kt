package com.ahmed.vr

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest

/**
 * Camera permission helper with explicit states and reasoning for permanent denial.
 */
object CameraPermissionHelper {
    const val CAMERA_PERMISSION_CODE = 0x1001

    fun hasCameraPermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
    }

    fun isPermissionPermanentlyDenied(activity: Activity): Boolean {
        // If we don't have permission and shouldShowRequestPermissionRationale returns false,
        // the user either never saw the prompt yet or permanently denied. The caller should
        // interpret this in context (e.g., after a denial flow).
        val has = hasCameraPermission(activity)
        if (has) return false
        return !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    }

    fun handlePermissionsResult(requestCode: Int, grantResults: IntArray): PermissionResult {
        if (requestCode != CAMERA_PERMISSION_CODE) return PermissionResult.IGNORED
        if (grantResults.isEmpty()) return PermissionResult.DENIED
        return if (grantResults[0] == PackageManager.PERMISSION_GRANTED) PermissionResult.GRANTED else PermissionResult.DENIED
    }

    enum class PermissionResult { GRANTED, DENIED, IGNORED }
}
