package com.example.handy

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import com.google.android.material.button.MaterialButton

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

    // Track the currently selected button
    private var selectedButton: MaterialButton? = null
    private var selectedFingerButton: MaterialButton? = null


    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Initialise views
        editTextMeasurementValue = findViewById(R.id.editTextMeasurementValue)
        buttonCalculate = findViewById(R.id.buttonCalculate)
        buttonSettings = findViewById(R.id.buttonSettings)
        textViewResult = findViewById(R.id.textViewResult)

        // Prevent keyboard from showing while keeping cursor visible
        editTextMeasurementValue.showSoftInputOnFocus = false

        // Request focus and show cursor immediately
        editTextMeasurementValue.requestFocus()

        //Set initial cursor position at the end
        editTextMeasurementValue.setSelection(editTextMeasurementValue.text.length)


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

    private fun setupToggleButton(button: MaterialButton, type: String, showFingerButtons: Boolean) {

        val fingerButtonsLayout= findViewById<LinearLayout>(R.id.fingerButtonsLayout)

        button.setOnClickListener {
            if (selectedButton == button) {

                //force button to stay selected
                button.isChecked = true

            }else{
                // Deselect previous button
                selectedButton?.isChecked = false

                // Select this button
                button.isChecked = true
                selectedButton = button

                // Apply your specific logic
                selectedMeasurementType = type
                fingerButtonsLayout.visibility = if (showFingerButtons) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupMeasurementButtons(){
        val handButton : MaterialButton = findViewById(R.id.buttonHand)
        val fingerButton : MaterialButton = findViewById(R.id.buttonFinger)
        val forearmButton : MaterialButton = findViewById(R.id.buttonForearm)
        //Finger buttons
        val fingerButtons = mapOf(
            "Thumb" to findViewById(R.id.buttonThumb),
            "Index" to findViewById(R.id.buttonIndex),
            "Middle" to findViewById(R.id.buttonMiddle),
            "Ring" to findViewById(R.id.buttonRing),
            "Pinky" to findViewById<MaterialButton>(R.id.buttonPinky)

        )

        // Set up each button
        setupToggleButton(handButton, "Hand", false)
        setupToggleButton(fingerButton, "Finger", true)
        setupToggleButton(forearmButton, "Forearm", false)

        // Set initial state
        handButton.isChecked = true
        selectedButton = handButton
        selectedMeasurementType = "Hand"

        // Set up finger buttons
       fingerButtons.forEach{ (fingerName, button) ->

           button.isCheckable =true

           button.setOnClickListener {
               if (selectedFingerButton == button) {
                   //force button to stay selected
                   button.isChecked = true

               }else{
                   // Deselect previous button
                   selectedFingerButton?.isChecked = false

                   // Select this button
                   button.isChecked = true
                   selectedFingerButton = button

                   selectedFingerType = fingerName
               }

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

    fun onKeypadClick(view: View) {
        val inputField = findViewById<EditText>(R.id.editTextMeasurementValue)
        val display = findViewById<TextView>(R.id.textViewResult)
        val currentText = inputField.text.toString()
        val currentCursorPosition = inputField.selectionStart

        // Check if it's the backspace button
        if (view.id == R.id.btnBack) {

            // Only proceed if there's text and cursor isn't at the beginning
            if (currentText.isNotEmpty() && currentCursorPosition > 0) {
                // Create new text by removing the character before the cursor
                val newText = StringBuilder(currentText)
                    .deleteCharAt(currentCursorPosition - 1)
                    .toString()

                // Update text field
                inputField.setText(newText)

                // Place cursor at correct position (one position back from where it was)
                inputField.setSelection(currentCursorPosition - 1)
            }
        }
        else if (view.id == R.id.btnAC) {

            //clear the display
            inputField.setText("")
            display.text = ""
            inputField.setSelection(0)
        }
        else {
            // This is a normal button (number)
            val button = view as MaterialButton

            when (button.text) {
                "." -> {
                    // Prevent multiple dots in the input field
                    if (!currentText.contains(".")) {
                        // Insert at cursor position
                        val newText = StringBuilder(currentText)
                            .insert(currentCursorPosition, ".")
                            .toString()
                        inputField.setText(newText)
                        inputField.setSelection(currentCursorPosition + 1)
                    }
                }
                else -> {
                    // For all other digits, insert at cursor position
                    val newText = StringBuilder(currentText)
                        .insert(currentCursorPosition, button.text)
                        .toString()
                    inputField.setText(newText)
                    inputField.setSelection(currentCursorPosition + 1)
                }
            }
        }
    }

}