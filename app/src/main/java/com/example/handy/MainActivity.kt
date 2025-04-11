package com.example.handy

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var measurementConverter: MeasurementConverter

    //Input fields
    private lateinit var spinnerMeasurementType: MaterialAutoCompleteTextView
    private lateinit var spinnerFingerType: MaterialAutoCompleteTextView
    private lateinit var editTextMeasurementValue: EditText
    private lateinit var buttonCalculate: Button
    private lateinit var buttonSettings: Button
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
        buttonSettings = findViewById(R.id.buttonSettings)
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


        //setup measurement button
        buttonSettings.setOnClickListener{
            showSetupDialog()
        }

    }

    private fun setupSpinners() {

        // Set up measurement type dropdown
        val measurementAdapter = ArrayAdapter(
            this, android.R.layout.simple_dropdown_item_1line, measurementTypes
        )
        (spinnerMeasurementType as? MaterialAutoCompleteTextView)?.setAdapter(measurementAdapter)

        // Setup finger type dropdown
        val fingerAdapter = ArrayAdapter(
            this, android.R.layout.simple_dropdown_item_1line, fingerTypes
        )
        (spinnerFingerType as? MaterialAutoCompleteTextView)?.setAdapter(fingerAdapter)

        // Handle selection for measurement type
        (spinnerMeasurementType as? MaterialAutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            spinnerFingerType.visibility = if (position == 1) View.VISIBLE else View.GONE
        }

        // Set initial selection to trigger the visibility logic
        (spinnerMeasurementType as? MaterialAutoCompleteTextView)?.setText(measurementTypes[0], false)
        spinnerFingerType.visibility = View.GONE
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
            val measurementType = spinnerMeasurementType.text.toString()
            val inputValue = editTextMeasurementValue.text.toString().toDoubleOrNull()

            if (inputValue == null) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
                return
            }

            val result = when (measurementType) {
                "Hand" -> measurementConverter.handsToMetric(inputValue)
                "Finger" -> {
                    val fingerType = spinnerFingerType.text.toString()
                    measurementConverter.fingersToMetric(fingerType.lowercase(Locale.ROOT), inputValue)
                }
                "Forearm" -> measurementConverter.forearmsToMetric(inputValue)
                else -> throw IllegalArgumentException("Unknown measurement type")
            }

            // Display the result
            textViewResult.text = "$inputValue $measurementType ${
                if (measurementType == "Finger") "(" + spinnerFingerType.text.toString() + ")" else ""
            } = ${result.value} ${result.unit}"

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

}