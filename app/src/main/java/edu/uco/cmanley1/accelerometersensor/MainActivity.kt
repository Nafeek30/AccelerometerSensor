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
import java.io.File
import java.sql.Timestamp

class MainActivity : Activity(), SensorEventListener
{
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor

    private lateinit var db: FirebaseFirestore

    private var coordinateSets = ArrayList<CoordinateSet>()
    private var hasUploadedOrSaved = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let{
            accelerometer = it
        }

        grp_delays.setOnCheckedChangeListener { _, checkedId ->
            if(tgl_record.isChecked)
            {
                sensorManager.unregisterListener(this, accelerometer)
                when (checkedId)
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

        tgl_record.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked)
            {
                if(hasUploadedOrSaved)
                {
                    coordinateSets.clear()
                    hasUploadedOrSaved = false
                }

                when (grp_delays.checkedRadioButtonId)
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
            else
                sensorManager.unregisterListener(this, accelerometer)
        }

        switch_saveLocal.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked)
            {
                Toast.makeText(this, R.string.msg_saveLocally,
                        Toast.LENGTH_LONG).show()
                btn_upload.text = getString(R.string.lbl_save)
            }
            else
            {
                Toast.makeText(this, R.string.msg_cloudOnly,
                        Toast.LENGTH_LONG).show()
                btn_upload.text = getString(R.string.lbl_upload)
            }
        }

        btn_upload.setOnClickListener {
            if(coordinateSets.size == 0)
            {
                Toast.makeText(this, R.string.err_emptyData, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(!switch_saveLocal.isChecked)
                uploadData()
            else
                saveData()
        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int)
    {
        //Toast.makeText(this, accuracy.toString(), Toast.LENGTH_SHORT).show()
    }

    override fun onSensorChanged(event: SensorEvent?)
    {
        if(tgl_record.isChecked && event != null)
        {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            txt_xCoord.text = x.toString()
            txt_yCoord.text = y.toString()
            txt_zCoord.text = z.toString()

            recordData(CoordinateSet(x, y, z, System.currentTimeMillis()))
        }
    }

    private fun recordData(coordinateSet: CoordinateSet)
    {
        try
        {
            coordinateSets.add(coordinateSet)
        }
        catch (ex: Exception)
        {
            Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun uploadData() : Boolean
    {
        var success = true

        for(coordinateSet in coordinateSets)
        {
            db.collection(getString(R.string.collectionPath))
                .document(Timestamp(coordinateSet.timestamp).toString())
                .set(coordinateSet)
                    .addOnSuccessListener {
                        hasUploadedOrSaved = true
                    }
                    .addOnFailureListener { ex: Exception ->
                        Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show()
                        success = false
                    }
        }

        if(success)
            Toast.makeText(this, R.string.err_uploadSuccess, Toast.LENGTH_SHORT).show()
        return success
    }

    private fun saveData() : Boolean
    {
        var success = true

        try
        {
            val path = getExternalFilesDir(null)
            val dir = File(path, getString(R.string.dir_data))

            if (!dir.exists())
                dir.mkdirs()
            val file = File(dir, getString(R.string.file_name,
                    Timestamp(System.currentTimeMillis()).toString()))

            for (coordinateSet in coordinateSets) {
                file.appendText(coordinateSet.x.toString())
                file.appendText(",")
                file.appendText(coordinateSet.y.toString())
                file.appendText(",")
                file.appendText(coordinateSet.z.toString())
                file.appendText(",")
                file.appendText(coordinateSet.timestamp.toString())
                file.appendText("\n")
            }

            Toast.makeText(this, getString(R.string.err_saveSuccess, file.toString()),
                    Toast.LENGTH_LONG).show()
            hasUploadedOrSaved = true
        }
        catch(ex: Exception)
        {
            Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show()
            success = false
        }

        return success
    }
}
