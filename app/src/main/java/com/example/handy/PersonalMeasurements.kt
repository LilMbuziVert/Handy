package com.example.handy

import android.icu.util.Measure
import kotlin.math.round

data class PersonalMeasurements(
    val handLengthCm: Double,
    val fingerLengthsCm: Map<String, Double>,
    val forearmLengthCm: Double
)

data class Measurement(val value: Double, val unit: String){
    override fun toString(): String = "$value $unit"
}

class MeasurementConverter(private val personalMeasurements: PersonalMeasurements){

    //convert number of hand lengths to metric
    fun handsToMetric(numberOfHands: Double): Measurement{
        val totalCm = numberOfHands * personalMeasurements.handLengthCm
        return formatResult(totalCm)
    }

    fun fingersToMetric(fingerType: String, numberOfFingers: Double) : Measurement {
        val fingerLength = personalMeasurements.fingerLengthsCm[fingerType]
            ?: throw IllegalArgumentException("Unknown finger type : $fingerType")

        val totalCm = numberOfFingers * fingerLength
        return formatResult(totalCm)
    }

    fun forearmsToMetric(numberOfForearms: Double) : Measurement {
        val totalCm = numberOfForearms * personalMeasurements.forearmLengthCm
        return formatResult (totalCm)
    }

    private fun formatResult(totalCm: Double): Measurement {
        return if (totalCm >= 100){
            val meters = totalCm /100
            Measurement(round(meters * 100)/100, "m")

        }else{
            Measurement(round(totalCm * 10) /10,  "cm")
        }
    }
}