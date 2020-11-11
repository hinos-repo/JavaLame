package com.fire.javalame

import android.annotation.SuppressLint
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import net.sourceforge.lame.lowlevel.LameEncoder
import net.sourceforge.lame.mp3.Lame
import net.sourceforge.lame.mp3.MPEGMode
import java.io.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.sound.sampled.AudioFormat

class MainActivity : AppCompatActivity()
{
    private var m_serviceExcutor: ExecutorService? = null
    private var m_recorder: AudioRecord? = null
    private var m_strPcmPath = ""
    private var m_strMp3Path = ""

    private var m_audioSource = MediaRecorder.AudioSource.MIC
    private var m_nSampleRates = 44100
    private var m_nChannelCount = android.media.AudioFormat.CHANNEL_IN_STEREO
    private var m_nAudioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT

    private var m_nRecoBufferSize = 0

    private var m_bRecord = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ComPermission.getPermissionState(this, ComPermission.EnumPermission.RECORD_PERMISSION)

        initViewSetting()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        ComPermission.onRequestPermissionsResult(requestCode, permissions, grantResults, ComPermission.EnumPermission.RECORD_PERMISSION)
    }

    fun initViewSetting()
    {
        m_serviceExcutor = Executors.newSingleThreadExecutor()

        m_nRecoBufferSize = AudioRecord.getMinBufferSize(m_nSampleRates, m_nChannelCount, m_nAudioFormat)


        m_strPcmPath = Environment.getExternalStorageDirectory().absolutePath + "/record.pcm"
        m_strMp3Path = Environment.getExternalStorageDirectory().absolutePath + "/record.mp3"
    }

    fun onBtnStartRecord(view: View)
    {
        m_recorder = AudioRecord(m_audioSource, m_nSampleRates, m_nChannelCount, m_nAudioFormat, m_nRecoBufferSize)

        m_bRecord = true

        class RecordThread : Runnable
        {
            override fun run()
            {
                m_recorder?.startRecording()

                val recordData = ByteArray(m_nRecoBufferSize)
                var pcmFos : FileOutputStream? = null
                try {
                    pcmFos = FileOutputStream(m_strPcmPath)
                }catch (e : Exception)
                {
                    e.printStackTrace()
                }

                while (m_bRecord)
                {
                    m_recorder?.read(recordData, 0, m_nRecoBufferSize)
                    try {
                        pcmFos?.write(recordData, 0, m_nRecoBufferSize)
                    }catch (e : Exception)
                    {
                        e.printStackTrace()
                    }
                }

                m_recorder?.stop()
                m_recorder?.release()

                pcmFos?.close()
            }
        }
        m_serviceExcutor?.execute(RecordThread())
    }

    fun onBtnStopRecord(view: View)
    {
        m_bRecord = false
    }

    fun onBtnPlayPCM(view: View)
    {

    }

    fun onBtnChangeMP3(view: View)
    {
        class ConvertMp3Thread : Runnable
        {
            override fun run()
            {
                val pcmFile = File(m_strPcmPath)
                encodePcmToMp3(pcmFile.readBytes())
            }
        }

        m_serviceExcutor?.execute(ConvertMp3Thread())
    }

    fun onBtnRecordWithMp3(view: View)
    {
        m_recorder = AudioRecord(m_audioSource, m_nSampleRates, m_nChannelCount, m_nAudioFormat, m_nRecoBufferSize)

        m_bRecord = true

        class RecordWithMp3Thread : Runnable
        {
            override fun run()
            {
                val encoder = LameEncoder(
                    AudioFormat((44100 / 2).toFloat(), 32, 2, true, false),
                    128,
                    MPEGMode.STEREO,
                    Lame.QUALITY_HIGH,
                    false
                )

                m_recorder?.startRecording()

                val recordData = ByteArray(m_nRecoBufferSize)
                val bosMp3 = ByteArrayOutputStream()
                val currentPcmPosition = 0

                while (m_bRecord)
                {
                    val nReadSize = m_recorder!!.read(recordData, 0, m_nRecoBufferSize)
                    if (nReadSize >= 0)
                    {
                        val buffer = ByteArray(m_nRecoBufferSize)
                        var bytesToTransfer = buffer.size
                        val bytesWritten = encoder.encodeBuffer(recordData, currentPcmPosition, bytesToTransfer ,buffer)
                        if (bytesWritten > 0)
                        {
                            bosMp3.write(buffer, 0, bytesWritten)
                        }
                    }
                }

                encoder.close()
                m_recorder?.stop()
                m_recorder?.release()

                val mp3File = File(m_strMp3Path)
                mp3File.deleteOnExit()

                var fos: FileOutputStream? = null
                try
                {
                    fos = FileOutputStream(m_strMp3Path)
                    fos.write(bosMp3.toByteArray())
                } catch (e: FileNotFoundException)
                {
                    e.printStackTrace()
                } catch (e: IOException)
                {
                    e.printStackTrace()
                }
            }
        }
        m_serviceExcutor?.execute(RecordWithMp3Thread())
    }

    private val m_hOnHandler : Handler = @SuppressLint("HandlerLeak")
    object : Handler()
    {
        val byteArrayInArray = mutableListOf<ByteArray>()
        override fun handleMessage(msg: Message)
        {
            when(msg.what)
            {
                1 -> //add
                {
                    if (msg.obj != null)
                    {
                        val buffer = msg.obj as ByteArray
                        byteArrayInArray.add(buffer)
                    }
                }

                2 -> //exit
                {
                    val mp3 = ByteArrayOutputStream()
                    val newByteArray = byteArrayInArray.flatMap { it.toList() }

                }
            }
        }
    }


    fun encodePcmToMp3(pcm: ByteArray)
    {
        val mp3File = File(m_strMp3Path)
        mp3File.deleteOnExit()

        val encoder = LameEncoder(
            AudioFormat((44100 / 2).toFloat(), 32, 2, true, false),
            128,
            MPEGMode.STEREO,
            Lame.QUALITY_HIGH,
            false
        )
        val mp3 = ByteArrayOutputStream()
        val buffer = ByteArray(encoder.pcmBufferSize)
//        var bytesToTransfer = Math.min(buffer.size, pcm.size)
        var bytesToTransfer = buffer.size
        var bytesWritten: Int
        var currentPcmPosition = 0
        while (0 < encoder.encodeBuffer(pcm, currentPcmPosition, bytesToTransfer, buffer).also { bytesWritten = it })
        {
            currentPcmPosition += bytesToTransfer
            bytesToTransfer = Math.min(buffer.size, pcm.size - currentPcmPosition)
            Log.e("logmessage", "current position: $currentPcmPosition")
            mp3.write(buffer, 0, bytesWritten)
        }
        encoder.close()
        val file = File(m_strMp3Path)
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("logmessage", "cannot create file")
            }
            var stream: FileOutputStream? = null
            try {
                stream = FileOutputStream(m_strMp3Path)
                stream.write(mp3.toByteArray())
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        //   return mp3.toByteArray();
    }
}