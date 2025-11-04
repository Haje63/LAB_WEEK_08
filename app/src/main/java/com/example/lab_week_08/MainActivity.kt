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
import androidx.work.Data
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.lab_week_08.R
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker // Import ThirdWorker


class MainActivity : AppCompatActivity() {
    private val workManager = WorkManager.getInstance(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v,
                                                                             insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,
                systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {

                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val id = "001"
        val id2 = "002" // ID for third worker and second service

        val firstRequest = OneTimeWorkRequest
            .Builder(FirstWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(FirstWorker
                .INPUT_DATA_ID, id)
            ).build()

        val secondRequest = OneTimeWorkRequest
            .Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(SecondWorker
                .INPUT_DATA_ID, id)
            ).build()

        // Create the request for the ThirdWorker
        val thirdRequest = OneTimeWorkRequest
            .Builder(ThirdWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(ThirdWorker
                .INPUT_DATA_ID, id2) // Use new ID
            ).build()

        // 1. Start the first chain (FirstWorker -> SecondWorker)
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .enqueue()

        // Observe FirstWorker
        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("First process is done")
                }
            }

        // 2. Observe SecondWorker
        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("Second process is done")
                    // 3. Launch NotificationService after SecondWorker is done
                    launchNotificationService()
                }
            }

        // 3. Observe NotificationService
        NotificationService.trackingCompletion.observe(
            this) { Id ->
            showResult("Process for Notification Channel ID $Id is done!")
            // 4. Enqueue ThirdWorker after NotificationService is done
            workManager.enqueue(thirdRequest)
        }

        // 4. Observe ThirdWorker
        workManager.getWorkInfoByIdLiveData(thirdRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("Third process is done")
                    // 5. Launch SecondNotificationService after ThirdWorker is done
                    launchSecondNotificationService()
                }
            }

        // 5. Observe SecondNotificationService
        SecondNotificationService.trackingCompletionSecond.observe(
            this) { Id ->
            showResult("Process for Second Notification Channel ID $Id is done!")
        }
    }

    private fun getIdInputData(idKey: String, idValue: String) =
        Data.Builder()
            .putString(idKey, idValue)
            .build()

    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // This launches the FIRST service
    private fun launchNotificationService() {
        val serviceIntent = Intent(
            this,
            NotificationService::class.java
        ).apply {
            putExtra(EXTRA_ID, "001")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    // This launches the SECOND service
    private fun launchSecondNotificationService() {
        val serviceIntent = Intent(
            this,
            SecondNotificationService::class.java
        ).apply {
            // Use the new EXTRA_ID for the second service
            putExtra(EXTRA_ID_SECOND, "002")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    companion object {
        const val EXTRA_ID = "Id"
        const val EXTRA_ID_SECOND = "Id2" // Add new constant
    }
}