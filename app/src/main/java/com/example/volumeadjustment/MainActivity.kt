package com.example.volumeadjustment

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.widget.AppCompatButton
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaRecorder
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import java.io.IOException
import kotlin.math.absoluteValue


const val REQUEST_CODE = 200
class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {

    private lateinit var buttonStart : Button
    private lateinit var textViewAmplitude : TextView
    private lateinit var textViewVolume : TextView
    private lateinit var buttonSettings : Button

    private var permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS)
    private var permissionGranted = false

    private lateinit var recorder : MediaRecorder

    private lateinit var audioManager: AudioManager
    private var maxVolume : Int = 0

    private lateinit var timer : Timer

    private var amplitudeValue = 0
    private var oldAmplitude = 0
    private var lineAmplitude = 0
    private var volumeValue = 0

    private var lineCounter = 0
    private var counter = 0

    private var isWorking = false
    private var isInLine = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        permissionGranted = ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED

        if(!permissionGranted)
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)

        buttonStart = findViewById(R.id.buttonStart)
        buttonSettings = findViewById(R.id.buttonSettings)
        textViewAmplitude = findViewById(R.id.TextViewAmplitude)
        textViewVolume = findViewById(R.id.TextViewVolume)

        buttonStart.setOnClickListener(){
            isWorking = !isWorking

            if(buttonStart.text == "Start")
                buttonStart.text = "Stop"
            else{buttonStart.text = "Start"}
        }

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        timer = Timer(this)

        recorder = MediaRecorder()

        try{
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile("${externalCacheDir?.absolutePath}/audio.mp3")
            recorder.prepare()
        }catch (e: IOException){}

        recorder.start()
        timer.start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE)
            permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    private fun setVolume(){

        oldAmplitude = if( isInLine )
            lineAmplitude
        else { amplitudeValue }

        amplitudeValue = recorder.maxAmplitude

        if( (amplitudeValue - oldAmplitude).absoluteValue > 500 ) {
            lineCounter += 1
            lineAmplitude = oldAmplitude
            isInLine = true

            if( lineCounter < 5) {
                return
            }

            lineCounter = 0
            isInLine = false

            when{
                amplitudeValue < 7000 -> {
                    volumeValue = (0.2 * maxVolume).toInt()
                    textViewVolume.text = "Volume level set to 20%"
                }
                amplitudeValue in 7000..11000 -> {
                    volumeValue = (0.4 * maxVolume).toInt()
                    textViewVolume.text = "Volume level set to 40%"
                }
                amplitudeValue in 11000..14000 -> {
                    volumeValue = (0.5 * maxVolume).toInt()
                    textViewVolume.text = "Volume level set to 50%"
                }
                amplitudeValue > 14000 -> {
                    volumeValue = (0.6 * maxVolume).toInt()
                    textViewVolume.text = "Volume level set to 60%"
                }
            }

            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                volumeValue,
                AudioManager.FLAG_PLAY_SOUND)
        }
    }

    override fun onTimerTick(duration: String) {
        amplitudeValue = recorder.maxAmplitude

        textViewAmplitude.text = "Outside Noise: ${amplitudeValue} amplitude"

        if(isWorking)
            setVolume()
    }
}