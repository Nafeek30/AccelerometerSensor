package edu.uco.cmanley1.accelerometersensor

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import java.sql.Timestamp

class MainActivity : Activity(), SensorEventListener
{
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor

    private var db: FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()

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


        btn_test.setOnClickListener {
            /* test */
            val coord = CoordinateSet(0.0, 0.1, 0.2, Timestamp(System.currentTimeMillis()))
            db?.collection("Coordinates")?.document()?.set(coord)
                    ?.addOnSuccessListener {
                        Toast.makeText(this, "YEAHYAHBOYEE", Toast.LENGTH_SHORT).show()
                    }
                    ?.addOnFailureListener {
                        Toast.makeText(this, "nope.", Toast.LENGTH_SHORT).show()
                    }
        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int)
    {
        //Toast.makeText(this, accuracy.toString(), Toast.LENGTH_SHORT).show()
    }

    override fun onSensorChanged(event: SensorEvent?)
    {
        // figure out how many coordinate sets we send to Firebase / second
        // do logic
        // send all the things.
        // amen.
        txt_xCoord.text = event!!.values[0].toString()  //x
        txt_yCoord.text = event!!.values[1].toString()  //y
        txt_zCoord.text = event!!.values[2].toString()  //z
    }
}
