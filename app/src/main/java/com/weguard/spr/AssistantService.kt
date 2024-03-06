package com.weguard.spr

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Locale

class AssistantService : Service() {

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var recognizerIntent: Intent
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this@AssistantService)
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private var timeNow =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())

    override fun onCreate() {
        super.onCreate()
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech Recognizer available", Toast.LENGTH_SHORT)
                .show()
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
                }

                override fun onError(error: Int) {
                    Log.d("OnError", "An network or recognition error occurred.")
                }

                override fun onResults(results: Bundle?) {
                    val speechResults =
                        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!speechResults.isNullOrEmpty()) {
//                        Toast.makeText(this@AssistantService, speechResults[0], Toast.LENGTH_SHORT)
//                            .show()
                        textToSpeech = TextToSpeech(applicationContext) { status ->
                            if (status == TextToSpeech.SUCCESS) {
                                val speakResult = textToSpeech.setLanguage(Locale.getDefault())
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

                            if (speechResults[0].contains(
                                    "time",
                                    ignoreCase = true
                                )
                            ) {
                                textToSpeech.speak(
                                    "Hi, Current Time is $timeNow",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    null
                                )
                            } else if (speechResults[0].contains("bye", ignoreCase = true)) {
                                textToSpeech.speak(
                                    "Bye...Take care",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    null
                                )
                            } else {
                                textToSpeech.speak(
                                    "Hi, I'm Chitra",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    null
                                )
                            }
                        }
                    } else {
                        Toast.makeText(this@AssistantService, "No results", Toast.LENGTH_SHORT)
                            .show()
                    }
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
        } else {
            Toast.makeText(
                this,
                "Speech Recognizer not available",
                Toast.LENGTH_SHORT
            )
                .show()
        }
        runnable = object : Runnable {
            override fun run() {
                speechRecognizer.startListening(recognizerIntent)
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(this, "Service Started...!", Toast.LENGTH_SHORT).show()
        handler.post(runnable)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.stopListening()
        handler.removeCallbacks(runnable)
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

}