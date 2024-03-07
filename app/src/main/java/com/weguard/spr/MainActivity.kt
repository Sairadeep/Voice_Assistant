package com.weguard.spr

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.weguard.spr.ui.theme.SPRTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity as Activity,
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CALL_PHONE),
                99
            )
        } else {
            Toast.makeText(this@MainActivity, "Record Audio and Call Permissions granted", Toast.LENGTH_SHORT)
                .show()
        }
        setContent {
            SPRTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HideSpeechRecognizerAnnotation()
                }
            }
        }
        val intent = Intent(this@MainActivity, AssistantService::class.java)
        startService(intent)
    }
}

@Composable
fun HideSpeechRecognizerAnnotation() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Assistant service...!", fontSize = 24.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SPRTheme {
        HideSpeechRecognizerAnnotation()
    }
}