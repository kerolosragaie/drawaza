package com.kerollosragaie.drawaza

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.core.view.iterator
import androidx.lifecycle.lifecycleScope
import com.kerollosragaie.drawaza.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var mImageButtonCurrentPaint: ImageButton? = null
    private var drawingView: DrawingView? = null

    //For progress dialog:
    private var progressDialog:Dialog?=null

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
            binding.ivBackground.setImageResource(android.R.color.transparent)
            binding.ivBackground.visibility = View.INVISIBLE
        }
        binding.ibUndo.setOnClickListener {
            drawingView?.onClickUndo()
        }

        binding.ibRedo.setOnClickListener {
            drawingView?.onClickRedo()
        }
        binding.ibSaveImage.setOnClickListener {
            if (isReadStorageAllowed()) {
                showProgressDialog()
                appLogo()
                lifecycleScope.launch {
                    saveBitmapFile(getBitmapFromView(binding.flDrawingViewContainer))
                }
            } else {
                cancelProgressDialog()
                showRationalDialog("Warning!",
                    "Ops you just denied the permission.",
                    positiveName = "Settings",
                    positiveFun = {
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri: Uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }, negativeFun = {})
            }
        }

        binding.ibMore.setOnClickListener {
            //TODO: add about page
        }
    }

    //Storage permission
    private fun requestStoragePermission() {
        binding.ivBackground.visibility = View.VISIBLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
        ) {
            //Opens settings auto if canceled the permission before
            showRationalDialog("Warning!",
                "Ops you just denied the permission.",
                positiveName = "Settings",
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
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    //Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return result == PackageManager.PERMISSION_GRANTED
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
        binding.ivBackground.visibility = View.INVISIBLE
        showRationalDialog(
            "Warning!",
            "Are you sure you want to create new page?" +
                    "Current progress will be lost.",
            positiveName = "Yes",
            negativeName = "No",
            positiveFun = {
                drawingView?.onClickCreateNewPage()
                drawingView?.setSizeForBrush(6.toFloat())
                drawingView?.setColor("#000000")
                binding.ivBackground.setImageResource(android.R.color.transparent)

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
     *Save image as bitmap
     * */
    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(
            view.width, view.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    //the file to store it:
                    val file =
                        File(
                            externalCacheDir?.absoluteFile.toString() +
                                    File.separator + "DRAWAZA_" + System.currentTimeMillis() / 1000 + ".png"
                        )
                    val fileOutput = FileOutputStream(file)
                    fileOutput.write(bytes.toByteArray())
                    fileOutput.close()

                    result = file.absolutePath

                    runOnUiThread {
                        appLogo()
                        cancelProgressDialog()

                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Image saved successfully.",
                                Toast.LENGTH_SHORT
                            ).show()
                            shareImage(result)
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong while saving the image.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    /**
     * Shows rationale dialog for displaying why the app needs permission
     * Only shown if the user has denied the permission request previously
     */
    private fun showRationalDialog(
        title: String,
        message: String,
        dialogIcon: Int = R.drawable.ic_warning,
        positiveName:String = "Ok",
        negativeName:String = "Cancel",
        positiveFun: () -> Unit,
        negativeFun: () -> Unit
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveName) { dialog, _ ->
                dialog.dismiss()
                positiveFun()
            }.setCancelable(false)
            .setIcon(dialogIcon)
            .setNegativeButton(negativeName) { dialog, _ ->
                dialog.dismiss()
                negativeFun()
            }
        builder.create().show()
    }

    //To show and hide progress dialog:
    private fun showProgressDialog(){
        progressDialog = Dialog(this@MainActivity)
        progressDialog?.setContentView(R.layout.progress_dialog)
        progressDialog?.setCancelable(false)
        progressDialog?.show()
    }

    private fun cancelProgressDialog(){
        if(progressDialog != null){
            progressDialog?.dismiss()
            progressDialog = null
        }
    }

    //To show or hide app logo:
    private fun appLogo(){
        if(binding.appLogo.clLogo.visibility == View.VISIBLE){
            binding.appLogo.clLogo.visibility = View.INVISIBLE
        }else{
            if(binding.ivBackground.isVisible){
                binding.appLogo.tvAppName.setTextColor(Color.WHITE)
            }else{
                binding.appLogo.tvAppName.setTextColor(Color.BLACK)
            }
            binding.appLogo.clLogo.visibility = View.VISIBLE
        }
    }

    //For sharing option:
    private fun shareImage(result:String){
        MediaScannerConnection.scanFile(this, arrayOf(result),null){
            path,uri->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent,"Share"))
        }
    }

}