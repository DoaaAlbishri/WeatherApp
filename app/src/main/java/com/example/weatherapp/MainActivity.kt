package com.example.weatherapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.view.isVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.Exception
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
class MainActivity : AppCompatActivity() {
    // zip code of city
    private var city = "10001"
    // API document Link https://openweathermap.org/current#zip
    // API key
    private val API = "2cda96c02772796298fcbdb1300c4614"
    // UI element
    private lateinit var errorButton: Button
    private lateinit var rlZip: RelativeLayout
    private lateinit var editTextZip: EditText
    private lateinit var buttonZip: Button

    private lateinit var tvaddress: TextView
    private lateinit var tvlastUpdate: TextView
    private lateinit var tvstatus: TextView
    private lateinit var tvtemp: TextView
    private lateinit var tvminTemp: TextView
    private lateinit var tvmaxTemp: TextView
    private lateinit var tvSunrise: TextView
    private lateinit var tvSunset: TextView
    private lateinit var tvWind: TextView
    private lateinit var tvPressure: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var llRefresh: LinearLayout

    //convert C to F
    private var c = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // if somthing wrong
        errorButton = findViewById(R.id.btError)
        errorButton.setOnClickListener {
            city = "10001"
            display()
        }

        //zip interface
        rlZip = findViewById(R.id.rlZip)
        editTextZip = findViewById(R.id.editText)
        buttonZip = findViewById(R.id.button)
        buttonZip.setOnClickListener {
            city = editTextZip.text.toString()
            display()
            editTextZip.text.clear()
            // hide zip interface
            rlZip.isVisible = false
        }

        display()
    }

    // function to invoke the right fun to loading or fetch data and update data or show error
    private fun display(){
        //println("CITY: $city")
        CoroutineScope(IO).launch {
            //loading
            updateInterface(-1)
            //fetch data and update data
            val data = async {
                fetchWeatherData()
            }.await()
            if(data.isNotEmpty()){
                weatherData(data)
                updateInterface(0)
            }else{
                //show error
                updateInterface(1)
            }
        }
    }

    //fill data
    private suspend fun weatherData(result: String){
        withContext(Main){
            //GET data
            val jsonObj = JSONObject(result)
            val main = jsonObj.getJSONObject("main")
            val sys = jsonObj.getJSONObject("sys")
            val wind = jsonObj.getJSONObject("wind")
            val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

            val lastUpdate:Long = jsonObj.getLong("dt")
            val lastUpdateText = "Updated at: " + SimpleDateFormat(
                    "dd/MM/yyyy hh:mm a",
                    Locale.ENGLISH).format(Date(lastUpdate*1000))
            val currentTemperature = main.getString("temp")
            val temp = try{
                currentTemperature.substring(0, currentTemperature.indexOf(".")) + "°C"
            }catch(e: Exception){
                currentTemperature + "°C"
            }
            val minTemperature = main.getString("temp_min")
            val tempMin = "Low: " + minTemperature.substring(0, minTemperature.indexOf("."))+"°C"
            val maxTemperature = main.getString("temp_max")
            val tempMax = "High: " + maxTemperature.substring(0, maxTemperature.indexOf("."))+"°C"
            val pressure = main.getString("pressure")
            val humidity = main.getString("humidity")

            val sunrise:Long = sys.getLong("sunrise")
            val sunset:Long = sys.getLong("sunset")
            val windSpeed = wind.getString("speed")
            val weatherDescription = weather.getString("description")

            val address = jsonObj.getString("name")+", "+sys.getString("country")

            //fill data in UI
            tvaddress = findViewById<TextView>(R.id.address)
            tvaddress.text = address
            tvaddress.setOnClickListener {
                // show zip interface
                rlZip.isVisible = true
            }
            tvlastUpdate = findViewById<TextView>(R.id.lastUpdated)
            tvlastUpdate.text =  lastUpdateText
            tvstatus = findViewById<TextView>(R.id.status)
            tvstatus.text = weatherDescription.capitalize(Locale.getDefault())
            tvtemp = findViewById<TextView>(R.id.temp)
            tvtemp.text = temp
            tvminTemp = findViewById<TextView>(R.id.minTemp)
            tvminTemp.text = tempMin
            tvmaxTemp= findViewById<TextView>(R.id.maxTemp)
            tvmaxTemp.text = tempMax
            tvSunrise= findViewById<TextView>(R.id.tvSunrise)
            tvSunrise.text = SimpleDateFormat("hh:mm a",
                    Locale.ENGLISH).format(Date(sunrise*1000))
            tvSunset=findViewById<TextView>(R.id.tvSunset)
            tvSunset.text = SimpleDateFormat("hh:mm a",
                    Locale.ENGLISH).format(Date(sunset*1000))
            tvWind=findViewById<TextView>(R.id.tvWind)
            tvWind.text = windSpeed
            tvPressure=findViewById<TextView>(R.id.tvPressure)
            tvPressure.text = pressure
            tvHumidity=findViewById<TextView>(R.id.tvHumidity)
            tvHumidity.text = humidity
            //refresh data
            llRefresh=findViewById<LinearLayout>(R.id.llRefresh)
            llRefresh.setOnClickListener { display() }
            //switch between Celsius and Fahrenheit
            tvtemp.setOnClickListener {
                if(c) {
                    val Dectemp = currentTemperature.substring(0, currentTemperature.indexOf("."))
                    val Decmin = minTemperature.substring(0, minTemperature.indexOf("."))
                    val Decmax = maxTemperature.substring(0, maxTemperature.indexOf("."))
                    var tempF = (Dectemp.toInt() * 1.8) + 32
                    var tempMinF = (Decmin.toInt() * 1.8) + 32
                    var tempMaxF = (Decmax.toInt() * 1.8) + 32
                    println(Dectemp)
                    println(tempMinF)
                    println(tempMaxF)
                    tvtemp.text = tempF.toString() + "°F"
                    tvminTemp.text = "Low: " +tempMinF.toString()  + "°F"
                    tvmaxTemp.text = "High: " +tempMaxF.toString()  + "°F"
                    c=false
                }else{
                    c = true
                    tvtemp.text = temp
                    tvminTemp.text = tempMin
                    tvmaxTemp.text = tempMax
                }
            }
        }
    }
    //fetch data from API
    private fun fetchWeatherData(): String{
        var response = ""
        try {
            response = URL("https://api.openweathermap.org/data/2.5/weather?zip=$city&units=metric&appid=$API")
                    .readText(Charsets.UTF_8)
        }catch (e: Exception){
            println("Error: $e")
        }
        return response
    }

    private suspend fun updateInterface(state: Int){
        // states: -1 : progress is visible , 0 = weather is visible , 1 = error is visible
        withContext(Main){
            when{
                state < 0 -> {
                    findViewById<ProgressBar>(R.id.progress).visibility = View.VISIBLE
                    findViewById<RelativeLayout>(R.id.rlMain).visibility = View.GONE
                    findViewById<LinearLayout>(R.id.llError).visibility = View.GONE
                }
                state == 0 -> {
                    findViewById<ProgressBar>(R.id.progress).visibility = View.GONE
                    findViewById<RelativeLayout>(R.id.rlMain).visibility = View.VISIBLE
                }
                state > 0 -> {
                    findViewById<ProgressBar>(R.id.progress).visibility = View.GONE
                    findViewById<LinearLayout>(R.id.llError).visibility = View.VISIBLE
                }
            }
        }
    }
}