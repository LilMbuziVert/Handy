package com.example.handy

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment

class MeasurementSetupDialogFragment :  DialogFragment() {
    private var onMeasurementsSetListener: ((PersonalMeasurements) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_measurement_setup, null)

        //Get references to all input fields
        val editTextHandLength = view.findViewById<EditText>(R.id.editTextHandLength)
        val editTextThumbLength = view.findViewById<EditText>(R.id.editTextThumbLength)
        val editTextIndexLength = view.findViewById<EditText>(R.id.editTextIndexLength)
        val editTextMiddleLength = view.findViewById<EditText>(R.id.editTextMiddleLength)
        val editTextRingLength = view.findViewById<EditText>(R.id.editTextRingLength)
        val editTextPinkyLength = view.findViewById<EditText>(R.id.editTextPinkyLength)
        val editTextForearmLength = view.findViewById<EditText>(R.id.editTextForearmLength)

        val buttonSave = view.findViewById<Button>(R.id.buttonSave)

        //create the dialog
        builder.setView(view)
            .setTitle("Set your Measurements")

        val dialog = builder.create()


        //Set click listener for save button
        buttonSave.setOnClickListener {
            // Get values from input fields
            val handLength = editTextHandLength.text.toString().toDoubleOrNull() ?: 18.0
            val thumbLength = editTextThumbLength.text.toString().toDoubleOrNull() ?: 6.5
            val indexLength = editTextIndexLength.text.toString().toDoubleOrNull() ?: 7.5
            val middleLength = editTextMiddleLength.text.toString().toDoubleOrNull() ?: 8.0
            val ringLength = editTextRingLength.text.toString().toDoubleOrNull() ?: 7.4
            val pinkyLength = editTextPinkyLength.text.toString().toDoubleOrNull() ?: 6.0
            val forearmLength = editTextForearmLength.text.toString().toDoubleOrNull() ?: 26.5

            // Create finger lengths map
            val fingerLengths = mapOf(
                "thumb" to thumbLength,
                "index" to indexLength,
                "middle" to middleLength,
                "ring" to ringLength,
                "pinky" to pinkyLength
            )

            // Create personal measurements
            val measurements = PersonalMeasurements(handLength, fingerLengths, forearmLength)

            // Notify listener
            onMeasurementsSetListener?.invoke(measurements)

            // Dismiss dialog
            dismiss()
        }

        return dialog
    }

    fun setOnMeasurementsListener(listener: (PersonalMeasurements) -> Unit) {
        onMeasurementsSetListener = listener
    }

}


