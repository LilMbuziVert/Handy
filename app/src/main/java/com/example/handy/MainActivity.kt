package com.example.handy

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var measurementConverter: MeasurementConverter

    //Input fields
    private lateinit var editTextMeasurementValue: EditText
    private lateinit var buttonCalculate: Button
    private lateinit var buttonSettings: Button
    private lateinit var textViewResult: TextView

    // Available measurement types
    private val measurementTypes = listOf("Hand", "Finger", "Forearm")
    private val fingerTypes = listOf("Thumb", "Index", "Middle", "Ring", "Pinky")


    private var selectedMeasurementType:String = "Hand"
    private var selectedFingerType: String = "Thumb"


    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Initialise views
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


        setupMeasurementButtons()


        //Setup calculate button
        buttonCalculate.setOnClickListener {
            calculateMeasurement()
        }


        //setup measurement button
        buttonSettings.setOnClickListener{
            showSetupDialog()
        }

    }

    private fun setupMeasurementButtons(){
        val handButton = findViewById<Button>(R.id.buttonHand)
        val fingerButton = findViewById<Button>(R.id.buttonFinger)
        val forearmButton = findViewById<Button>(R.id.buttonForearm)
        val fingerButtonsLayout= findViewById<LinearLayout>(R.id.fingerButtonsLayout)

        //Finger buttons
        val fingerButtons = mapOf(
            "Thumb" to findViewById<Button>(R.id.buttonThumb),
            "Index" to findViewById<Button>(R.id.buttonIndex),
            "Middle" to findViewById<Button>(R.id.buttonMiddle),
            "Ring" to findViewById<Button>(R.id.buttonRing),
            "Pinky" to findViewById<Button>(R.id.buttonPinky)

        )

        handButton.setOnClickListener{
            selectedMeasurementType = "Hand"
            fingerButtonsLayout.visibility = View.GONE
        }

        fingerButton.setOnClickListener{
            selectedMeasurementType = "Finger"
            fingerButtonsLayout.visibility = View.VISIBLE
        }

        forearmButton.setOnClickListener{
            selectedMeasurementType = "Forearm"
            fingerButtonsLayout.visibility = View.GONE
        }

        for ((finger, button) in fingerButtons){
            button.setOnClickListener{
                selectedFingerType = finger
                Toast.makeText(this, "$finger selected", Toast.LENGTH_SHORT).show()
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


    private fun calculateMeasurement() {
        try {
            val inputValue = editTextMeasurementValue.text.toString().toDoubleOrNull()

            if (inputValue == null) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
                return
            }

            val result = when (selectedMeasurementType) {
                "Hand" -> measurementConverter.handsToMetric(inputValue)
                "Finger" -> measurementConverter.fingersToMetric(selectedFingerType.lowercase(Locale.ROOT), inputValue)
                "Forearm" -> measurementConverter.forearmsToMetric(inputValue)
                else -> throw IllegalArgumentException("Unknown measurement type")
            }

            // Display the result
            val fingerText = if  (selectedMeasurementType == "Finger") "($selectedFingerType)" else ""
            "$inputValue $selectedMeasurementType$fingerText = ${result.value}${result.unit}".also { textViewResult.text = it }

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

}