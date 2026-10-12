 package com.beats.app.components

 import androidx.compose.material3.Button
 import androidx.compose.material3.Text
 import androidx.compose.runtime.Composable

 @Composable
fun FilledButtonExample(onClick: () -> Unit) {
    Button(onClick = { onClick() }) {
        Text("Filled")
    }
}