package com.fire.javalame

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object ComPermission
{
    enum class EnumPermission
    {
        STORAGE_PERMISSION, CAMERA_PERMISSION, RECORD_PERMISSION
    }

    const val PERMISSION_REQUEST_OK = 87

    private val m_arrStoragePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
    private val m_arrCameraPermissions = arrayOf(Manifest.permission.CAMERA)
    private val m_arrRecordPermission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)

    private fun getPermissionsType(enumPermission : EnumPermission) : Array<String>
    {
        when(enumPermission)
        {
            EnumPermission.STORAGE_PERMISSION   ->      return m_arrStoragePermissions
            EnumPermission.CAMERA_PERMISSION    ->      return m_arrCameraPermissions
            EnumPermission.RECORD_PERMISSION    ->      return m_arrRecordPermission
        }
    }

    fun getPermissionState(activity : Activity, enumPermission : EnumPermission) : Boolean
    {
        val arrPermission = getPermissionsType(enumPermission)
        var i = 0
        val size = arrPermission.size
        for (permission in arrPermission)
        {
            if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) i++
        }

        if (i == size) return true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            ActivityCompat.requestPermissions(activity, arrPermission, PERMISSION_REQUEST_OK)
        }
        return false
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray, enumPermission : EnumPermission) : Boolean
    {
        val myPermissions = getPermissionsType(enumPermission)
        if (requestCode == PERMISSION_REQUEST_OK)
        {
            var w = 0
            loop1@ for (permission in permissions)
            {
                for (cameraPermission in myPermissions)
                {
                    if (permission == cameraPermission)
                    {
                        w++
                        continue@loop1
                    }
                }
            }
            if (w != myPermissions.size) return false

            var i = 0
            for (grant in grantResults)
            {
                if (grant == PackageManager.PERMISSION_GRANTED) i++
            }

            if (i == permissions.size) return true
        }
        return false
    }
}