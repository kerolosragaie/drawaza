package com.kerollosragaie.drawaza

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.iterator
import com.kerollosragaie.drawaza.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var mImageButtonCurrentPaint: ImageButton? = null
    private var drawingView: DrawingView? = null

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                //now when i get data (result.data) and then assign it to imageview background
                binding.ivBackground.setImageURI(result.data!!.data)
            }
        }

    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                //val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    //open gallery and get an image from it:
                    val pickIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                }
                /* * used rationalDialog instead of this code:
                else {
                if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                Toast.makeText(
                this@MainActivity,
                "Ops you just denied the permission.",
                Toast.LENGTH_SHORT
                ).show()
                }
                }*/
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawingView = binding.drawingView
        drawingView?.setSizeForBrush(6.toFloat())

        val linearLayoutPaintColors: LinearLayout = binding.llPaintColors
        for (singleColor in linearLayoutPaintColors) {
            singleColor.setOnClickListener {
                paintClicked(it)
            }
        }
        mImageButtonCurrentPaint = linearLayoutPaintColors[2] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        binding.ibBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        binding.ibAddImage.setOnClickListener {
            requestStoragePermission()
        }

        binding.ibNewPage.setOnClickListener {
            createNewPage()
        }
        binding.ibRemoveImage.setOnClickListener {
            binding.ivBackground.setImageResource(R.drawable.blank_page)
        }
        binding.ibUndo.setOnClickListener {
            drawingView?.onClickUndo()
        }

        binding.ibRedo.setOnClickListener {
            drawingView?.onClickRedo()
        }
    }

    //Storage permission
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
        ) {
            //Opens settings auto if canceled the permission before
            showRationalDialog("Warning!",
                "Ops you just denied the permission.",
                positiveFun = {
                    val intent =
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri: Uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }, negativeFun = {})
        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            requestPermission.launch(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            )
        }
    }

    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(6.toFloat())
            brushDialog.dismiss()
        }

        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(14.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    // A function for color selection
    /**
     * Method is called when color is clicked from pallet_normal.
     *
     * @param view ImageButton on which click took place.
     */
    private fun paintClicked(view: View) {
        if (view !== mImageButtonCurrentPaint) {
            // Update the color
            val imageButton = view as ImageButton
            // Here the tag is used for swaping the current color with previous color.
            // The tag stores the selected view
            val colorTag = imageButton.tag.toString()
            // The color is set as per the selected tag here.
            drawingView?.setColor(colorTag)
            // Swap the backgrounds for last active and currently active image button.
            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_normal
                )
            )

            //Current view is updated with selected view in the form of ImageButton.
            mImageButtonCurrentPaint = view
        }
    }


    /**Reset every thing and creates new page:*/
    private fun createNewPage() {

        showRationalDialog(
            "Warning!",
            "Are you sure you want to create new page?" +
                    "Current progress will be lost.",
            positiveFun = {
                drawingView?.onClickCreateNewPage()
                drawingView?.setSizeForBrush(6.toFloat())
                drawingView?.setColor("#000000")
                binding.ivBackground.setImageResource(R.drawable.ratatouille_bg)

                mImageButtonCurrentPaint!!.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_normal)
                )
                mImageButtonCurrentPaint = binding.llPaintColors[2] as ImageButton
                mImageButtonCurrentPaint!!.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
                )
            },
            negativeFun = {}
        )

    }

    /**
     * Shows rationale dialog for displaying why the app needs permission
     * Only shown if the user has denied the permission request previously
     */
    private fun showRationalDialog(
        title: String,
        message: String,
        dialogIcon: Int = R.drawable.ic_warning,
        positiveFun: () -> Unit,
        negativeFun: () -> Unit
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Ok") { dialog, _ ->
                dialog.dismiss()
                positiveFun()
            }.setCancelable(false)
            .setIcon(dialogIcon)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                negativeFun()
            }
        builder.create().show()
    }
}