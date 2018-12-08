package edu.uco.cmanley1.accelerometersensor

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileInputStream
import java.sql.Timestamp

const val TAG = "local"

class MainActivity : Activity(), SensorEventListener
{
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var file: File

    private var coordinateSets = ArrayList<CoordinateSet>()
    private var hasUploadedOrSaved = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

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

        if(saveData())
        {
            try {
                val stream = FileInputStream(file)
                val ref = storage.reference.child(file.name)
                val uploadTask = ref.putStream(stream)
                val urlTask = uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot,
                        Task<Uri>> { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                        }
                    }
                    return@Continuation ref.downloadUrl
                }).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val csv = CSV(file.name, System.currentTimeMillis(), task.result.toString())

                        try {
                            db.collection(getString(R.string.collectionPath2)).document(file.name)
                                    .set(csv)
                                    .addOnSuccessListener {
                                        hasUploadedOrSaved = true
                                    }
                                    .addOnFailureListener { ex: Exception ->
                                        Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show()
                                        success = false
                                    }
                        }
                        catch(ex: Exception)
                        {
                            Log.d(TAG, ex.toString())
                        }
                    } else {
                        Toast.makeText(this, R.string.err_uploadError, Toast.LENGTH_LONG).show()
                        success = false
                    }
                }
            }
            catch(ex: Exception)
            {
                Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show()
                success = false
            }
        }
//        val document =  object {
//            var coordinateSets = ArrayList<CoordinateSet>()
//        }
//
//        document.coordinateSets = coordinateSets
//
//        db.collection(getString(R.string.collectionPath))
//            .document(System.currentTimeMillis().toString())
//            .set(document)
//            .addOnSuccessListener {
//                hasUploadedOrSaved = true
//            }
//            .addOnFailureListener { ex: Exception ->
//                Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show()
//                success = false
//            }

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
            file = File(dir, getString(R.string.file_name,
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
