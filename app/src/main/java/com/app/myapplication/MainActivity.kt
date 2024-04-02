package com.app.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var imageView1: ImageView
    private lateinit var imageView2: ImageView
    private lateinit var imageCardView1: CardView
    private lateinit var imageCardView2: CardView
    private lateinit var proceedButton: Button
    private lateinit var validateButton: Button
    private lateinit var validationLayout: RelativeLayout
    private lateinit var editTextAadhaar: EditText
    private var isImage1Captured = false
    private var isImage2Captured = false

    private var currentImageView: ImageView? = null
    private lateinit var currentImageUri: Uri

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_IMAGE_CROP = 2
        private const val REQUEST_PERMISSION_CODE = 101
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                dispatchTakePictureIntent()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView1 = findViewById(R.id.imageView1)
        imageView2 = findViewById(R.id.imageView2)
        imageCardView1 = findViewById(R.id.imageCardView1)
        imageCardView2 = findViewById(R.id.imageCardView2)
        proceedButton = findViewById(R.id.proceedButton)
        validateButton = findViewById(R.id.validateButton)
        validationLayout = findViewById(R.id.validationLayout)
        editTextAadhaar = findViewById(R.id.editTextAadhaar)

        // Disable proceedbtn initially
        proceedButton.isEnabled = false

        imageView1.setOnClickListener {
            currentImageView = imageView1
            checkPermissionAndDispatchTakePictureIntent()
        }

        imageView2.setOnClickListener {
            currentImageView = imageView2
            checkPermissionAndDispatchTakePictureIntent()
        }

        proceedButton.setOnClickListener {
            // Hide ImageView, Button, and CardView
            imageView1.visibility = View.GONE
            imageView2.visibility = View.GONE
            imageCardView1.visibility = View.GONE
            imageCardView2.visibility = View.GONE
            proceedButton.visibility = View.GONE
            validationLayout.visibility = View.VISIBLE
        }

        validateButton.setOnClickListener {
            val aadhaarNumber = editTextAadhaar.text.toString()
            if (validateAadhaarId(aadhaarNumber)) {
                // Show Aadhar ID validation UI
                Toast.makeText(this, "valid Aadhar ID", Toast.LENGTH_SHORT).show()
            } else {
                // Show error message if Aadhar ID is invalid
                Toast.makeText(this, "Invalid Aadhar ID", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun validateAadhaarId(aadhaarNumber: String): Boolean {
        // Call the validate function from AadhaarUtil class
        return AadhaarUtil.isAadhaarNumberValid(aadhaarNumber)
    }

    private fun checkPermissionAndDispatchTakePictureIntent() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_PERMISSION_CODE
            )
        } else {
            dispatchTakePictureIntent()
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val imageFile = createImageFile()
            currentImageUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                imageFile
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageUri)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun createImageFile(): File {
        val storageDir = getExternalFilesDir(null)
        return File.createTempFile(
            "JPEG_${UUID.randomUUID()}",
            ".jpg",
            storageDir
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            currentImageUri.let { uri ->
                // Customizing crop options
                val options = UCrop.Options()
                options.setCompressionQuality(70) // Set compression quality
                options.setHideBottomControls(true) // Hide bottom controls
                options.setToolbarTitle("Crop Image") // Set toolbar title

                // Resize the grid on the screen by adjusting the aspect ratio
                val aspectRatioWidth = 4f
                val aspectRatioHeight = 3f
                UCrop.of(uri, Uri.fromFile(createImageFile()))
                    .withAspectRatio(aspectRatioWidth, aspectRatioHeight)
                    .withOptions(options) // Apply custom options
                    .start(this, REQUEST_IMAGE_CROP)

                // Mark the corresponding image as captured
                if (currentImageView == imageView1) {
                    isImage1Captured = true
                } else if (currentImageView == imageView2) {
                    isImage2Captured = true
                }

                // Enable proceedButton if both images are captured
                if (isImage1Captured && isImage2Captured) {
                    proceedButton.isEnabled = true
                }
            }
        } else if (requestCode == REQUEST_IMAGE_CROP && resultCode == RESULT_OK) {
            val croppedUri = UCrop.getOutput(data!!)
            // Load and display cropped image
            currentImageView?.setImageURI(croppedUri)

            // Remove the blur effect
            currentImageView?.alpha = 1f
        }
    }




    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            }
        }
    }
}
