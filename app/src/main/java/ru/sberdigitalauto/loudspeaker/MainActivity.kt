package ru.sberdigitalauto.loudspeaker

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import kotlinx.coroutines.InternalCoroutinesApi
import ru.sberdigitalauto.loudspeaker.io.WavManager

class MainActivity : AppCompatActivity() {

    private val audioDataManager = AudioDataManager()
    private val SELECT_WAV_RESULT = 943035

    @InternalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<CheckBox>(R.id.checkBoxStartStop).setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked)
                audioDataManager.start()
            else
                audioDataManager.stop()
        }

        findViewById<CheckBox>(R.id.checkBoxAEC).setOnCheckedChangeListener { buttonView, isChecked ->
            audioDataManager.enableAEC = isChecked
        }

        findViewById<Button>(R.id.btnOpenWAV).setOnClickListener{
            val intent: Intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(Intent.createChooser(intent, "Select a file"),
                SELECT_WAV_RESULT, null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing){
            audioDataManager.stop()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            data?.data?.let { file ->
                val wavManager = WavManager()
                wavManager.convertFile(file, this)
            }
        }
    }
}