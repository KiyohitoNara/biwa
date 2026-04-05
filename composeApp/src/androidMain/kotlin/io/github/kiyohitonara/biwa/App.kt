package io.github.kiyohitonara.biwa

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.kiyohitonara.biwa.presentation.addmedia.AddMediaScreen

@Composable
fun App() {
    MaterialTheme {
        AddMediaScreen()
    }
}

@Preview
@Composable
private fun AppPreview() {
    App()
}
