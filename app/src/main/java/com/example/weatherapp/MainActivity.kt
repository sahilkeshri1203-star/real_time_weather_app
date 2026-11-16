package com.example.weatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherScreen()
                }
            }
        }
    }
}

@Composable
fun WeatherScreen() {
    var cityInput by remember { mutableStateOf("") }
    var cityName by remember { mutableStateOf("Search a city") }
    var temperature by remember { mutableStateOf("--°C") }
    var condition by remember { mutableStateOf("") }
    var humidity by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val apiKey = "01147e50e95f057b524c922f72b99b95" // <-- Paste your OpenWeatherMap API key here

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Real-Time Weather",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = cityInput,
            onValueChange = { cityInput = it },
            label = { Text("Enter City Name (e.g., Delhi, London)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (cityInput.isNotBlank()) {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = ""
                        try {
                            val result = fetchWeatherData(cityInput.trim(), apiKey)
                            cityName = result.city
                            temperature = "${result.temp}°C"
                            condition = result.description.replaceFirstChar { it.uppercase() }
                            humidity = "Humidity: ${result.humidity}%"
                        } catch (e: Exception) {
                            errorMessage = "City not found or check internet/API key."
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Weather")
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        } else {
            Text(text = cityName, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = temperature, fontSize = 56.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = condition, fontSize = 20.sp)
            Text(text = humidity, fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

// Simple data holder
data class WeatherInfo(
    val city: String,
    val temp: Double,
    val description: String,
    val humidity: Int
)

// Built-in Android networking & JSON parsing (No external libraries)
suspend fun fetchWeatherData(city: String, apiKey: String): WeatherInfo {
    return withContext(Dispatchers.IO) {
        val urlString =
            "https://api.openweathermap.org/data/2.5/weather?q=$city&units=metric&appid=$apiKey"
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(responseText)

        val name = json.getString("name")
        val main = json.getJSONObject("main")
        val temp = main.getDouble("temp")
        val humidity = main.getInt("humidity")
        val description = json.getJSONArray("weather")
            .getJSONObject(0)
            .getString("description")

        WeatherInfo(name, temp, description, humidity)
    }
}