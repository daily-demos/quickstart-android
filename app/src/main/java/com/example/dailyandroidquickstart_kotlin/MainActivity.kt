package com.example.dailyandroidquickstart_kotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ToggleButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import co.daily.CallClient
import co.daily.CallClientListener
import co.daily.model.OutboundMediaType
import co.daily.model.Participant
import co.daily.model.ParticipantId
import co.daily.settings.InputSettings
import co.daily.view.VideoView


class MainActivity : AppCompatActivity() {

    private val TAG: String = "MainActivity"
    private lateinit var toggleCamera: ToggleButton
    private lateinit var toggleMicrophone: ToggleButton

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { !it }) {
                checkPermissions()
            } else {
                // permission is granted, we can initialize
                initializeCallClient()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toggleCamera = findViewById(R.id.toggleCamera)
        toggleMicrophone = findViewById(R.id.toggleMicrophone)

        checkPermissions()
    }

    private fun initializeCallClient() {
        //initialize the Daily Call Client object once permissions have been granted

        // Create call client
        val call = CallClient(applicationContext)

        // Create map of video views
        val videoViews = mutableMapOf<ParticipantId, VideoView>()

        val layout = findViewById<LinearLayout>(R.id.videoLinearLayout)

        // Listen for events
        call.addListener(object : CallClientListener {
            // Handle a remote participant joining
            override fun onParticipantJoined(participant: Participant) {
                val participantView = layoutInflater.inflate(R.layout.participant_view, layout, false)
                val videoView = participantView.findViewById<VideoView>(R.id.participant_video)
                videoView.track = participant.media?.camera?.track
                videoViews[participant.id] = videoView

                layout.addView(participantView)
            }

            // Handle a participant updating (e.g. their tracks changing)
            override fun onParticipantUpdated(participant: Participant) {
                val videoView = videoViews[participant.id]
                videoView?.track = participant.media?.camera?.track
            }

            override fun onInputsUpdated(inputSettings: InputSettings) {
                toggleCamera.isChecked = inputSettings.camera.isEnabled
                toggleMicrophone.isChecked = inputSettings.microphone.isEnabled
            }
        })

        findViewById<Button>(R.id.leave)
            .setOnClickListener {
                Log.d("BUTTONS", "User tapped the Leave button")
                call.leave {
                    if (it.isError) {
                        Log.e(TAG, "Got error while leaving call: ${it.error}")
                    } else {
                        Log.d(TAG, "Call has been left")
                    }
                }
            }

        toggleCamera.setOnCheckedChangeListener { _, isChecked ->
            Log.d("BUTTONS", "User tapped the Cam button")
            call.setInputEnabled(OutboundMediaType.Camera, isChecked)
        }

        toggleMicrophone.setOnCheckedChangeListener { _, isChecked ->
            Log.d("BUTTONS", "User tapped the Mute button")
            call.setInputEnabled(OutboundMediaType.Microphone, isChecked)
        }

        // Join the call
        call.join(url = "[YOUR_DAILY_ROOM_URL]") {
            it.error?.apply {
                Log.e(TAG, "Got error while joining call: $msg")
            }
            it.success?.apply {
                Log.i(TAG, "Successfully joined call.")
                toggleCamera.isChecked = call.inputs().camera.isEnabled
                toggleMicrophone.isChecked = call.inputs().microphone.isEnabled
            }
        }
    }

    private fun checkPermissions() {
        // Check whether permissions have been granted and if not, ask for permissions
        val appContext: Context = applicationContext

        val permissionList: Array<String> = appContext.packageManager.getPackageInfo(
            appContext.packageName,
            PackageManager.GET_PERMISSIONS
        ).requestedPermissions

        val notGrantedPermissions:MutableList<String> = ArrayList()
        for(permission in permissionList) {
            if (ContextCompat.checkSelfPermission(appContext, permission)
                != PackageManager.PERMISSION_GRANTED) {
                notGrantedPermissions.add(permission)
            }
        }

        if (notGrantedPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(notGrantedPermissions.toTypedArray())
        } else {
            // permission is granted, we can initialize
            initializeCallClient()
        }
    }
}