package com.example.handy

import android.annotation.SuppressLint
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.tooling.preview.Preview
import com.example.handy.ui.theme.HandyTheme
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var measurementConverter: MeasurementConverter

    //Input fields
    private lateinit var spinnerMeasurementType: Spinner
    private lateinit var spinnerFingerType: Spinner
    private lateinit var editTextMeasurementValue: EditText
    private lateinit var buttonCalculate: Button
    private lateinit var textViewResult: TextView

    // Available measurement types
    private val measurementTypes = listOf("Hand", "Finger", "Forearm")
    private val fingerTypes = listOf("Thumb", "Index", "Middle", "Ring", "Pinky")

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Initialise views
        spinnerMeasurementType = findViewById(R.id.spinnerMeasurementType)
        spinnerFingerType = findViewById(R.id.spinnerFingerType)
        editTextMeasurementValue = findViewById(R.id.editTextMeasurementValue)
        buttonCalculate = findViewById(R.id.buttonCalculate)
        textViewResult = findViewById(R.id.textViewResult)


        //Load saved measurements or show setup dialog if not available
        val prefs = applicationContext.getSharedPreferences("measurements_prefs", MODE_PRIVATE)
        val hasSetupMeasurements = prefs.getBoolean("has_setup_measurements", false)

        if (hasSetupMeasurements){
            loadMeasurements()
        }
        else{
            showSetupDialog()
        }

        //Setup spinners
        setupSpinners()

        //Setup calculate button
        buttonCalculate.setOnClickListener {
            calculateMeasurement()
        }

    }

    private fun setupSpinners(){

        //Set up measurement type spinner
        val measurementAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, measurementTypes
        )
        measurementAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMeasurementType.adapter = measurementAdapter

        //Setup finger type spinner
        val fingerAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, fingerTypes
        )
        fingerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFingerType.adapter = fingerAdapter

        //show/hide spinner based on selected measurement type
        spinnerMeasurementType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent:AdapterView<*>?, view: View?, position: Int, id:Long){
                spinnerFingerType.visibility = if (position == 1) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                spinnerFingerType.visibility = View.GONE
            }

        }
    }

    private fun loadMeasurements(){
        //Load saved measurements from savedPreferences
        val prefs = applicationContext.getSharedPreferences("measurements_prefs", MODE_PRIVATE)

        val handLength = prefs.getFloat("hand_length", 18.0f).toDouble()

        val fingerLengths = mutableMapOf<String, Double>()
        for (finger in fingerTypes){
            val length = prefs.getFloat("finger_${finger.lowercase(Locale.ROOT)}", 7.0f).toDouble()
            fingerLengths[finger.lowercase(Locale.ROOT)]= length
        }

        val forearmLength = prefs.getFloat("forearm_length", 26.5f).toDouble()

        val personalMeasurements = PersonalMeasurements(handLength, fingerLengths, forearmLength)
        measurementConverter = MeasurementConverter(personalMeasurements)
    }

    private fun showSetupDialog(){

        //create and show the setup dialogue
        val setupDialog = MeasurementSetupDialogFragment()
        setupDialog.show(supportFragmentManager, "MeasurementSetupDialog")
        setupDialog.setOnMeasurementsListener { measurements ->

            //save measurements to SharedPreferences
            val prefs = applicationContext.getSharedPreferences("measurements_prefs", MODE_PRIVATE)
            prefs.edit().apply{
                putFloat("hand_length", measurements.handLengthCm.toFloat())

                for((finger, length) in measurements.fingerLengthsCm){
                    putFloat("finger_$finger", length.toFloat())
                }

                putFloat("forearm_length", measurements.forearmLengthCm.toFloat())
                putBoolean("has_setup_measurements", true)
                apply()
            }

            //Initialise the converter
            measurementConverter = MeasurementConverter(measurements)

            Toast.makeText(this,  "Measurements saved!", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun calculateMeasurement() {
        try {
            val measurementType = spinnerMeasurementType.selectedItem as String
            val inputValue = editTextMeasurementValue.text.toString().toDoubleOrNull()

            if (inputValue == null) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
                return
            }

            val result = when (measurementType) {
                "Hand" -> measurementConverter.handsToMetric(inputValue)
                "Finger" -> {
                    val fingerType = spinnerFingerType.selectedItem as String
                    measurementConverter.fingersToMetric(fingerType.lowercase(Locale.ROOT), inputValue)
                }
                "Forearm" -> measurementConverter.forearmsToMetric(inputValue)
                else -> throw IllegalArgumentException("Unknown measurement type")
            }

            // Display the result
            textViewResult.text = "$inputValue $measurementType ${
                if (measurementType == "Finger") "(" + spinnerFingerType.selectedItem + ")" else ""
            } = ${result.value} ${result.unit}"

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

}