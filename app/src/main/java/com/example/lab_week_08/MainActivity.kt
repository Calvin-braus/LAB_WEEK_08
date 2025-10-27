package com.example.lab_week_08

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker

class MainActivity : AppCompatActivity() {

    private val workManager by lazy {
        WorkManager.getInstance(applicationContext)
    }

    // Definisikan thirdRequest sebagai properti kelas agar bisa diakses nanti
    private lateinit var thirdRequest: OneTimeWorkRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Minta izin notifikasi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        // --- Definisikan semua 3 worker ---
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val id = "001" // ID ini bisa dipakai bersama

        val firstRequest = OneTimeWorkRequest
            .Builder(FirstWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(FirstWorker.INPUT_DATA_ID, id))
            .build()

        val secondRequest = OneTimeWorkRequest
            .Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(SecondWorker.INPUT_DATA_ID, id))
            .build()

        // Inisialisasi thirdRequest (tapi jangan di-enqueue dulu)
        thirdRequest = OneTimeWorkRequest
            .Builder(ThirdWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(ThirdWorker.INPUT_DATA_ID, id))
            .build()

        // --- Mulai Rantai Proses ---

        // 1. Mulai rantai pertama: FirstWorker -> SecondWorker
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .enqueue()

        // 2. Amati FirstWorker (hanya untuk toast)
        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("First process is done")
                }
            }

        // 3. Amati SecondWorker. Setelah selesai, jalankan NotificationService (S1)
        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("Second process is done")
                    // (Langkah 3 dari 5)
                    launchNotificationService()
                }
            }

        // 4. Amati ThirdWorker. Setelah selesai, jalankan SecondNotificationService (S2)
        workManager.getWorkInfoByIdLiveData(thirdRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("Third process is done")
                    // (Langkah 5 dari 5)
                    launchSecondNotificationService()
                }
            }
    }

    private fun getIdInputData(idKey: String, idValue: String) =
        Data.Builder()
            .putString(idKey, idValue)
            .build()

    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Fungsi ini sekarang dimodifikasi untuk menjalankan ThirdWorker (W3)
    // setelah S1 (service pertama) selesai
    private fun launchNotificationService() {
        // Amati S1
        NotificationService.trackingCompletion.observe(
            this
        ) { id ->
            showResult("Process for Notification Channel ID $id is done!")

            // (Langkah 4 dari 5)
            // SETELAH S1 SELESAI, JALANKAN W3
            workManager.enqueue(thirdRequest)
        }

        val serviceIntent = Intent(
            this,
            NotificationService::class.java
        ).apply {
            putExtra(EXTRA_ID, "001")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    // Fungsi BARU untuk menjalankan S2 (service kedua)
    private fun launchSecondNotificationService() {
        // Amati S2 (hanya untuk toast)
        SecondNotificationService.trackingCompletion.observe(
            this
        ) { id ->
            // Ini adalah akhir dari semua proses
            showResult("Process for Notification Channel ID $id is done! All processes complete.")
        }

        val serviceIntent = Intent(
            this,
            SecondNotificationService::class.java // Panggil service kedua
        ).apply {
            putExtra(EXTRA_ID, "002") // Kirim ID channel kedua
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    companion object {
        const val EXTRA_ID = "Id"
    }
}
