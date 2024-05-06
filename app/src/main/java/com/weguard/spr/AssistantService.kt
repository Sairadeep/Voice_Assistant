package com.weguard.spr

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.text.SimpleDateFormat
import java.util.Locale

class AssistantService : Service() {

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var timer: CountDownTimer
    private lateinit var recognizerIntent: Intent
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this@AssistantService)
    private var timeNow =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(System.currentTimeMillis())
    var isGreetingProvided = false

    override fun onCreate() {
        super.onCreate()
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech Recognizer available", Toast.LENGTH_SHORT)
                .show()
            speechRec()
        } else {
            Toast.makeText(
                this,
                "Speech Recognizer not available",
                Toast.LENGTH_SHORT
            )
                .show()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationServ()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun notificationServ() {
//     creating a notification for a foreground service.
        val notificationChannelID = "Service Run"
        if (Build.VERSION.SDK_INT > VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelID,
                "MakeItRanAllTime",
                NotificationManager.IMPORTANCE_HIGH
            )

            val notificationManager =
                this@AssistantService.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(channel)

            val notificationBuilder =
                NotificationCompat.Builder(this, notificationChannelID).setContentTitle("SPR")
                    .setContentText("Assistant Service is running").setSmallIcon(R.drawable.mf)
                    .setPriority(NotificationCompat.PRIORITY_MAX).setOngoing(true)

            val notificationIntent = Intent(this@AssistantService, AssistantService::class.java)

            val pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

            notificationBuilder.setContentIntent(pendingIntent)

            val notification = notificationBuilder.build()

            startForeground(98, notification)

        } else {
            Toast.makeText(this@AssistantService, "No Notification allowed", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private fun speechRec() {
        val newText = mutableStateOf("Hi")
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
//        audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, AudioManager.FLAG_PLAY_SOUND)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        // Whatever the language the device is, the speech listener will work from that language and translate them to the default language
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("OnReadyForSpeech", "User Ready for speaking.")
            }

            override fun onBeginningOfSpeech() {
                Log.d("onBeginningOfSpeech", "The user has started to speak.")
            }

            override fun onRmsChanged(rmsdB: Float) {
                Log.d("OnRmsChanged", "Change in the level of sound")
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                Log.d("OnBufferReceived", "More sound has been received.")
            }

            override fun onEndOfSpeech() {
                Log.d("onEndOfSpeech", "Called after the user stops speaking.")
                speechRecognizer.stopListening()
            }

            override fun onError(error: Int) {
                Log.d("OnError", "An network or recognition error occurred.")
                speechRecognizer.startListening(recognizerIntent)
            }

            override fun onResults(results: Bundle?) {
                val timerDuration = mutableLongStateOf(60000)
                val speechResults =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                timer = object : CountDownTimer(timerDuration.longValue, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        Log.d(
                            "timerState",
                            "Timer Running... ${(millisUntilFinished) / 1000}"
                        )
                        val remaining = (millisUntilFinished / 1000)
                        if (remaining < 50) {
                            // This won't work as I'm not able to dynamically increase the count as it is possible in compose with remember.
                            timerDuration.longValue = 100000
                        }
                    }

                    override fun onFinish() {
                        isGreetingProvided = false
                    }

                }

                if (!speechResults.isNullOrEmpty()) {
                    Log.d("VoiceAssistantState", "Current Value : ${speechResults[0]}")
                    textToSpeech = TextToSpeech(applicationContext) { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            val speakResult = textToSpeech.setLanguage(Locale.US)
                            if (speakResult == TextToSpeech.LANG_MISSING_DATA || speakResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                                Toast.makeText(
                                    this@AssistantService,
                                    "Language not supported",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@AssistantService,
                                "I'm not knowing what's wrong..!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        if (!isGreetingProvided && speechResults[0].contains(
                                "Google",
                                ignoreCase = true
                            )
                        ) {
                            speechRecognizer.stopListening()
                            timer.start()
                            Log.d("VoiceAssistantState", "Ella reporting..: ${speechResults[0]}")
                            val text = "Hi, I'm Ella from WeGuard"
                            val translate =
                                TranslatorOptions.Builder().setSourceLanguage(Locale.US.toString())
                                    .setTargetLanguage(
                                        Locale.FRENCH.toString()
                                    ).build()
                            val translator = Translation.getClient(translate)
                            translator.translate(text).addOnSuccessListener {
                                newText.value = it
                                Log.d("WEware", newText.value)
                            }
                            textToSpeech.speak(
                                newText.value,
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                null
                            )
                            isGreetingProvided = true
                        } else if (isGreetingProvided) {
                            subsequentSpeechCommands(speechResults)
                            // Need to increase the timer dynamically and also reduce the deep sound.
                        } else {
                            timer.cancel()
                        }
                    }
                } else {
                    Toast.makeText(this@AssistantService, "No results", Toast.LENGTH_SHORT)
                        .show()
                }
                speechRecognizer.startListening(recognizerIntent)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                Log.d(
                    "OnPartialResults",
                    "Called when partial recognition results are available."
                )
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d("OnEvent", "Reserved for adding future events $eventType")
            }
        })
        speechRecognizer.startListening(recognizerIntent)
    }

    private fun subsequentSpeechCommands(command: ArrayList<String>) {
        if (command[0].contains("time", ignoreCase = true)) {
            textToSpeech.speak(
                "Okay, Current Time is $timeNow",
                TextToSpeech.QUEUE_FLUSH,
                null,
                null
            )
        } else if (command[0].contains(
                "bye",
                ignoreCase = true
            )
        ) {
            textToSpeech.speak(
                "Bye...Take care",
                TextToSpeech.QUEUE_FLUSH,
                null,
                null
            )
        } else if (command[0].contains(
                "call chakrapani",
                ignoreCase = true
            )
        ) {
            textToSpeech.speak(
                "Ok, calling Chakrapani",
                TextToSpeech.QUEUE_FLUSH,
                null,
                null
            )
            //   but how to handle when the device is in lock state
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.data = Uri.parse("tel: ${8801874959}")
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            this@AssistantService.startActivity(callIntent)
        } else {
            textToSpeech.speak(
                "Sorry, I couldn't help you out with this..!",
                TextToSpeech.QUEUE_FLUSH,
                null,
                null
            )
        }
    }
}