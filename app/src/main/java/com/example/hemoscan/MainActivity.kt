package com.example.hemoscan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class MainActivity : AppCompatActivity() {

    private var latestTmpUri: Uri? = null
    private var startScanButton: MaterialButton? = null
    private var uploadButton: MaterialButton? = null
    private var imagePlaceholder: ImageView? = null

    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null
    private var takePictureLauncher: ActivityResultLauncher<Uri>? = null
    private var galleryLauncher: ActivityResultLauncher<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        startScanButton = findViewById(R.id.ScanImageButton)
        uploadButton = findViewById(R.id.UploadImageButton)
        imagePlaceholder = findViewById(R.id.imageView)

        resetUiForInitialScan()

        startScanButton?.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }

        uploadButton?.setOnClickListener {
            galleryLauncher?.launch("image/*")
        }

        // Request Camera Permission
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to scan.", Toast.LENGTH_SHORT).show()
            }
        }

        // Gallery Selection
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                // handle selected image URI
            } ?: Toast.makeText(this, "No image selected.", Toast.LENGTH_SHORT).show()
        }

        // You also need to initialize takePictureLauncher for camera capture here
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                // Handle the image stored at latestTmpUri
                imagePlaceholder?.setImageURI(latestTmpUri)
            } else {
                Toast.makeText(this, "Failed to take picture.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetUiForInitialScan() {
        startScanButton?.visibility = View.VISIBLE
        uploadButton?.visibility = View.VISIBLE
        imagePlaceholder?.visibility = View.VISIBLE
        imagePlaceholder?.setImageResource(R.drawable.ic_scan)
        imagePlaceholder?.setColorFilter(ContextCompat.getColor(this, R.color.white))
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(this, "Camera access is needed to take pictures for scanning.", Toast.LENGTH_LONG).show()
                requestPermissionLauncher?.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher?.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        latestTmpUri = getTempFileUri()
        latestTmpUri?.let { uri ->
            takePictureLauncher?.launch(uri)
        } ?: run {
            Toast.makeText(this, "Could not create temporary file for image.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getTempFileUri(): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "JPEG_${timeStamp}.jpg"
        val imageFile = File(this.getExternalFilesDir(null), fileName)

        return try {
            FileProvider.getUriForFile(
                this,
                "${this.packageName}.fileprovider",
                imageFile
            )
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            Log.e("MainActivity", "Error getting temp file URI: ${e.message}")
            null
        }
    }
}
