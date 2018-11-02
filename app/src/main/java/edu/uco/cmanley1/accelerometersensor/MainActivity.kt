package edu.uco.cmanley1.accelerometersensor

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity(), SensorEventListener
{
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let{
            accelerometer = it
        }

        grp_delays.setOnCheckedChangeListener { group, checkedId ->
            sensorManager.unregisterListener(this, accelerometer)
            when(checkedId)
            {
                rdo_delayUI.id -> {
                    sensorManager.registerListener(this, accelerometer,
                        SensorManager.SENSOR_DELAY_UI)
                }
                rdo_delayNormal.id -> {
                    sensorManager.registerListener(this, accelerometer,
                        SensorManager.SENSOR_DELAY_NORMAL)
                }
                rdo_delayGame.id -> {
                    sensorManager.registerListener(this, accelerometer,
                            SensorManager.SENSOR_DELAY_GAME)
                }
                rdo_delayFastest.id -> {
                    sensorManager.registerListener(this, accelerometer,
                            SensorManager.SENSOR_DELAY_FASTEST)
                }
            }
        }


    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int)
    {
        //Toast.makeText(this, accuracy.toString(), Toast.LENGTH_SHORT).show()
    }

    override fun onSensorChanged(event: SensorEvent?)
    {
        txt_xCoord.text = event!!.values[0].toString()
        txt_yCoord.text = event!!.values[1].toString()
        txt_zCoord.text = event!!.values[2].toString()
    }
}
