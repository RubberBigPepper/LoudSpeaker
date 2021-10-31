package ru.sberdigitalauto.loudspeaker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.CheckBox
import kotlinx.coroutines.InternalCoroutinesApi

class MainActivity : AppCompatActivity() {

    private val audioDataManager = AudioDataManager()

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
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing){
            audioDataManager.stop()
        }
    }
}