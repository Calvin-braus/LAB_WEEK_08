package com.example.lab_week_08

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker

class MainActivity : AppCompatActivity() { // [cite: 80]

    // Create an instance of a work manager
    // Work manager manages all your requests and workers
    // it also sets up the sequence for all your processes
    private val workManager by lazy { // [cite: 82]
        WorkManager.getInstance(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) { // [cite: 83]
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // [cite: 83]

        // Create a constraint of which your workers are bound to.
        // Here the workers cannot execute the given process if
        // there's no internet connection
        val networkConstraints = Constraints.Builder() // [cite: 86]
            .setRequiredNetworkType(NetworkType.CONNECTED) // [cite: 87]
            .build()

        val Id = "001" // [cite: 88]

        // Create a one time work request that includes
        // all the constraints and inputs needed for the worker
        // This request is created for the FirstWorker class
        val firstRequest = OneTimeWorkRequest // [cite: 96]
            .Builder(FirstWorker::class.java) // [cite: 97]
            .setConstraints(networkConstraints) // [cite: 97]
            .setInputData( // [cite: 97]
                getIdInputData(
                    FirstWorker.INPUT_DATA_ID, Id // [cite: 98]
                )
            )
            .build() // [cite: 99]

        // This request is created for the SecondWorker class
        val secondRequest = OneTimeWorkRequest // [cite: 101]
            .Builder(SecondWorker::class.java) // [cite: 102]
            .setConstraints(networkConstraints) // [cite: 102]
            .setInputData( // [cite: 103]
                getIdInputData(
                    SecondWorker.INPUT_DATA_ID, Id // [cite: 104]
                )
            )
            .build() // [cite: 105]

        // Sets up the process sequence from the work manager instance
        // Here it starts with FirstWorker, then SecondWorker
        workManager.beginWith(firstRequest) // [cite: 107]
            .then(secondRequest) // [cite: 113]
            .enqueue() // [cite: 113]

        // Here, we receive the output and displaying the result as a toast message
        // Here we're observing the returned LiveData and getting the
        // state result of the worker (Can be SUCCEEDED, FAILED, or CANCELLED)
        workManager.getWorkInfoByIdLiveData(firstRequest.id) // [cite: 127]
            .observe(this) { info -> // [cite: 128]
                if (info.state.isFinished) { // [cite: 129]
                    showResult("First process is done") // [cite: 132]
                }
            }

        workManager.getWorkInfoByIdLiveData(secondRequest.id) // [cite: 133]
            .observe(this) { info -> // [cite: 134]
                if (info.state.isFinished) { // [cite: 135]
                    showResult("Second process is done") // [cite: 136]
                }
            }
    } // [cite: 139]

    // Build the data into the correct format before passing it to the worker as input
    private fun getIdInputData(IdKey: String, IdValue: String): Data = // [cite: 142]
        Data.Builder() // [cite: 143]
            .putString(IdKey, IdValue) // [cite: 144]
            .build() // [cite: 145]

    // Show the result as toast
    private fun showResult(message: String) { // [cite: 147]
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show() // [cite: 150]
    } // [cite: 148]
} // [cite: 149]