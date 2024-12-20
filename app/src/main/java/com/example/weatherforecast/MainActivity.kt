package com.example.weatherforecast

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.weatherforecast.ui.theme.WeatherForecastTheme
import data.WeatherModel
import org.json.JSONObject

const val API_KEY = "8227ae3d22ea472bbab120915240910"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherForecastTheme {
                val daysList = remember { mutableStateOf<List<WeatherModel>>(emptyList()) }
                val dialogState = remember { mutableStateOf(false) }
                val currentDay = remember { mutableStateOf(WeatherModel()) }

                if (dialogState.value) {
                    DialogSearch(dialogState) { city ->
                        fetchWeatherData(city, daysList, currentDay)
                    }
                }

                fetchWeatherData("London", daysList, currentDay)

                Image(
                    painter = painterResource(id = R.drawable.sky),
                    contentDescription = "background image",
                    modifier = Modifier.fillMaxSize().alpha(0.5f),
                    contentScale = ContentScale.FillBounds
                )

                Column {
                    MainScreen(
                        currentDay,
                        onClickSync = { fetchWeatherData("London", daysList, currentDay) },
                        onClickSearch = { dialogState.value = true }
                    )
                    TabLayout(daysList, currentDay)
                }
            }
        }
    }

    private fun fetchWeatherData(
        city: String,
        daysList: MutableState<List<WeatherModel>>,
        currentDay: MutableState<WeatherModel>
    ) {
        val url = "https://api.weatherapi.com/v1/forecast.json?key=$API_KEY&q=$city&days=3&aqi=no&alerts=no"
        val queue: RequestQueue = Volley.newRequestQueue(this)

        val request = StringRequest(
            com.android.volley.Request.Method.GET,
            url,
            { response ->
                parseWeatherData(response, daysList, currentDay)
            },
            { error ->
                Log.e("WeatherError", "VolleyError: ${error.message}")
            }
        )

        queue.add(request)
    }

    private fun parseWeatherData(
        response: String,
        daysList: MutableState<List<WeatherModel>>,
        currentDay: MutableState<WeatherModel>
    ) {
        try {
            val weatherData = extractWeatherData(response)
            if (weatherData.isNotEmpty()) {
                currentDay.value = weatherData[0]
                daysList.value = weatherData
            } else {
                Log.d("WeatherInfo", "No data found for the specified city")
            }
        } catch (e: Exception) {
            Log.e("WeatherError", "Error parsing weather data: ${e.message}")
        }
    }

    private fun extractWeatherData(response: String): List<WeatherModel> {
        val weatherList = mutableListOf<WeatherModel>()
        try {
            val jsonResponse = JSONObject(response)
            val forecastDays = jsonResponse.getJSONObject("forecast").getJSONArray("forecastday")

            for (i in 0 until forecastDays.length()) {
                val forecast = forecastDays.getJSONObject(i)
                val condition = forecast.getJSONObject("day").getJSONObject("condition")

                weatherList.add(
                    WeatherModel(
                        jsonResponse.getJSONObject("location").getString("name"),
                        forecast.getString("date"),
                        condition.getString("text"),
                        condition.getString("icon"),
                        forecast.getJSONObject("day").getString("maxtemp_c"),
                        forecast.getJSONObject("day").getString("mintemp_c"),
                        forecast.getJSONArray("hour").toString()
                    )
                )
            }

            val currentWeather = jsonResponse.getJSONObject("current")
            weatherList[0] = weatherList[0].copy(
                time = currentWeather.getString("last_updated"),
                currentTemp = currentWeather.getString("temp_c")
            )
        } catch (e: Exception) {
            Log.e("WeatherError", "Error extracting weather data: ${e.message}")
        }
        return weatherList
    }
}
