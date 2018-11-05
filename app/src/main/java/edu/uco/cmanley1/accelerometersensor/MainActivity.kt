package edu.uco.cmanley1.accelerometersensor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.sql.Timestamp

const val REQ_CODE_SAVE = 1

class MainActivity : Activity(), SensorEventListener
{
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor

    private lateinit var csvWriter: FileWriter
    private lateinit var db: FirebaseFirestore

    private var coordinateSets = ArrayList<CoordinateSet>()

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

        switch_saveLocal.setOnCheckedChangeListener { buttonView, isChecked ->
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

            var success = true
            var saveSuccess = true
            var csv: File? = null
            if(switch_saveLocal.isChecked)
            {
//                csv = File.createTempFile(Timestamp(System.currentTimeMillis()).toString(),
//                        "csv")
                csv = File(this.filesDir, "test.csv")
                csvWriter = FileWriter(csv)
            }

            for(coordinateSet in coordinateSets)
            {
                if(!switch_saveLocal.isChecked)
                {
                    db.collection("CoordinateSets")
                            .document(coordinateSet.timestamp.toString())
                            .set(coordinateSet)
                            .addOnFailureListener { ex: Exception ->
                                Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show()
                                success = false
                            }
                }
                else
                {
                    try
                    {
                        csvWriter.append(coordinateSet.x.toString())
                        csvWriter.append(",")
                        csvWriter.append(coordinateSet.y.toString())
                        csvWriter.append(",")
                        csvWriter.append(coordinateSet.z.toString())
                        csvWriter.append(",")
                        csvWriter.append(coordinateSet.timestamp.toString())
                        csvWriter.append("\n")
                    }
                    catch (ex: Exception)
                    {
                        Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show()
                        saveSuccess = false
                    }
                }
            }
            if(success)
                Toast.makeText(this, R.string.err_uploadSuccess, Toast.LENGTH_SHORT).show()
            if(saveSuccess)
            {
                try
                {
                    csvWriter.close()
                    Toast.makeText(this, csv.toString(), Toast.LENGTH_LONG).show()
//                    val saveIntent = Intent(Intent.ACTION_CREATE_DOCUMENT)
//                    saveIntent.addCategory(Intent.CATEGORY_OPENABLE)
//                    saveIntent.putExtra(Intent.EXTRA_STREAM, csv)
//                    saveIntent.type = "text/csv"
//
//                    startActivityForResult(saveIntent, REQ_CODE_SAVE)
                }
                catch(ex: Exception)
                {
                    Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show()
                }

            }
        }

    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
//    {
//        if(resultCode == Activity.RESULT_OK)
//        {
//            Toast.makeText(this, getString(R.string.err_filePath, data?.data?.toString()),
//                    Toast.LENGTH_LONG).show()
//            val pfd = contentResolver.openFileDescriptor(data?.data, "w")
//            val fileOutputStream = FileOutputStream(pfd.fileDescriptor)
//            val textContent = data!!.extras[Intent.EXTRA_STREAM].toString()
//            fileOutputStream.write(textContent.toByteArray())
//            fileOutputStream.close()
//            pfd.close()
//        }
//        else
//        {
//            Toast.makeText(this, R.string.err_saveFail, Toast.LENGTH_SHORT).show()
//        }
//    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int)
    {
        //Toast.makeText(this, accuracy.toString(), Toast.LENGTH_SHORT).show()
    }

    override fun onSensorChanged(event: SensorEvent?)
    {
        if(tgl_record.isChecked)
        {
            val x = event!!.values[0]
            val y = event!!.values[1]
            val z = event!!.values[2]

            txt_xCoord.text = x.toString()
            txt_yCoord.text = y.toString()
            txt_zCoord.text = z.toString()

            try
            {
                coordinateSets.add(CoordinateSet(x, y, z, Timestamp(System.currentTimeMillis())))
            }
            catch (ex: Exception)
            {
                Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show()
            }
        }
    }
}
