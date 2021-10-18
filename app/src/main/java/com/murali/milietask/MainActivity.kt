package com.murali.milietask

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.murali.milietask.CommonUtil.Companion.saveToInternalStorage
import com.murali.milietask.CommonUtil.Companion.toBitmap
import com.murali.milietask.widget.ProgressBarHandler
import kotlinx.coroutines.*
import java.net.URL

class MainActivity : AppCompatActivity() {

    var widthd = 200
    var height = 300
    var isNetworkConnected: Boolean = false
    var isFirstTime: Boolean = true
    var networkError: TextView? = null
    var tvDownload: TextView? = null
    var downloadButton: Button? = null
    var languageSelection: Spinner? = null
    var imageView: ImageView? = null
    private var sharedPref: SharedPreferences? = null

    @DelicateCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPref = getSharedPreferences(getString(R.string.preference_file_ame), Context.MODE_PRIVATE)

        tvDownload = findViewById(R.id.tvDownload)
        networkError = findViewById(R.id.network_error)
        imageView = findViewById(R.id.imageView)
        downloadButton = findViewById(R.id.button)
        languageSelection = findViewById(R.id.language_selection)

        val mProgressBarHandler = ProgressBarHandler(this)

        val keyName = resources.getString(R.string.preference_key_ame)
        val defaultValue = sharedPref?.getString(keyName, null)

        if (sharedPref?.contains(keyName) == true) {
            setDefaultValuesToUI(defaultValue)
        }

        languageSelection?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0 && defaultValue != null && isFirstTime) {
                    updateUIContent(defaultValue)
                } else if (position == 0) {
                    updateUIContent("en")
                } else if (position == 1) {
                    updateUIContent("hi")
                } else if (position == 2) {
                    updateUIContent("te")
                }
                isFirstTime = false
            }
        }

        downloadButton?.setOnClickListener {
            it.isEnabled = false // disabled button
            languageSelection?.isClickable = false
            mProgressBarHandler.show()

            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )

            widthd += 50
            height += 50

            val urlImage = URL("https://placekitten.com/g/$widthd/$height")
            // async task to download bitmap from url
            val result: Deferred<Bitmap?> = GlobalScope.async {
                urlImage.toBitmap()
            }

            GlobalScope.launch(Dispatchers.Main) {
                if (isNetworkConnected) {
                    // get the downloaded bitmap
                    val bitmap: Bitmap? = result.await()
                    // if image is downloaded then saving it into internal storage
                    bitmap?.apply {
                        // get saved bitmap from internal storage uri
                        val savedUri: Uri? = saveToInternalStorage(this@MainActivity)
                        // display saved bitmap to image view from internal storage
                        imageView?.setImageURI(savedUri)
                    }
                }
                it.isEnabled = true // enable button
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                languageSelection?.isClickable = true
                mProgressBarHandler.hide()
            }
        }
    }

    private fun updateUIContent(selectedLanguage: String?) {
        val resources = LocalizationHelper.changeLanguage(this@MainActivity, selectedLanguage)
        tvDownload?.text = resources.getString(R.string.description)
        networkError?.text = resources.getString(R.string.network_message)
        downloadButton?.text = resources.getString(R.string.download_txt)

        if (selectedLanguage.equals("en")) {
            languageSelection?.setSelection(0)
        } else if (selectedLanguage.equals("hi")) {
            languageSelection?.setSelection(1)
        } else if (selectedLanguage.equals("te")) {
            languageSelection?.setSelection(2)
        }
        sharedPref?.edit()?.putString(getString(R.string.preference_key_ame), selectedLanguage)?.apply()
    }

    private var networkBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val notConnected = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)
            if (notConnected) {
                isNetworkConnected = false
                findViewById<TextView>(R.id.network_error).visibility = View.VISIBLE
            } else {
                findViewById<TextView>(R.id.network_error).visibility = View.GONE
                isNetworkConnected = true
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(networkBroadcastReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(networkBroadcastReceiver)
    }

    private fun setDefaultValuesToUI(defaultValue: String?) {
        updateUIContent(defaultValue)
        val wrapper = ContextWrapper(this@MainActivity)
        // Initializing a new file. Will return a directory in internal storage
        val file = wrapper.getDir("cacheImages", Context.MODE_PRIVATE)
        val bitmap = BitmapFactory.decodeFile(file.listFiles().lastOrNull()?.getPath())
        // Display saved image from internal storage
        imageView?.setImageBitmap(bitmap)
    }
}
