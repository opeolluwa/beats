package com.beats.app


import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.beats.app.components.FileItem
import com.beats.app.components.FileListAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.util.Locale
//import com.beats.app.BuildConfig // Replace com.filesync.app with your actual namespace
class FIleManager : AppCompatActivity() {

    private var copiedFiles: List<File>? = null
    private val REQUEST_CODE_PICK_DESTINATION_DIRECTORY = 101
    private val REQUEST_CODE_STORAGE_PERMISSION = 100
    private val REQUEST_CODE_PICK_DIRECTORY = 1001
    private lateinit var btnCancelSelection: Button
    private lateinit var rvFileList: RecyclerView
    private lateinit var adapter: FileListAdapter
    private lateinit var btnDelete: Button
    private lateinit var btnRename: Button
    private lateinit var btnDestails: Button
    private lateinit var cardview: CardView
    private lateinit var viewall: TextView
    private lateinit var currentDirectory: File
    private val rootDirectory = Environment.getExternalStorageDirectory()

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        changeStatusBarColor("#5f71f2")


        btnCancelSelection = findViewById(R.id.btnCancelSelection)
        btnDestails = findViewById(R.id.btnDestails)
        btnDelete = findViewById(R.id.btnDelete)
        btnRename = findViewById(R.id.btnRename)
        rvFileList = findViewById(R.id.rvFileList)
        cardview = findViewById(R.id.cardView)
        viewall = findViewById(R.id.tv_view_all)

        if (!Environment.isExternalStorageManager()) {
            requestStoragePermission()
        }

        adapter = FileListAdapter(
            this,
            onItemClick = { fileItem ->
                if (fileItem.isDirectory) {
                    navigateToDirectory(fileItem)
                } else {
                    openFile(fileItem.file)
                }
            },
            onDeleteClick = {
                // Handle deletion completion here if needed
            },
            onRenameClick = { fileItem, newName ->
                val oldFile = File(fileItem.path)
                val newFile = File(oldFile.parent, newName)

                try {
                    if (oldFile.exists() && oldFile.isFile && !newFile.exists()) {
                        val success = oldFile.renameTo(newFile)
                        if (success) {
                            val message =
                                "File renamed successfully: ${oldFile.path} to ${newFile.path}"
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        } else {
                            val errorMessage = "Failed to rename: Unknown error"
                            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorMessage = when {
                            !oldFile.exists() -> "Failed to rename: File does not exist"
                            !oldFile.isFile -> "Failed to rename: Not a file"
                            newFile.exists() -> "Failed to rename: File with the new name already exists"
                            else -> "Failed to rename: Unknown error"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("FileRename", "Failed to rename: ${e.message}", e)
                    Toast.makeText(this, "Failed to rename: Unknown error", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        )




        displayStorageInfo()
        displayFileSizes()









        rvFileList.layoutManager = LinearLayoutManager(this)
        rvFileList.adapter = adapter

        currentDirectory = rootDirectory
        listFiles(currentDirectory)

        btnCancelSelection.setOnClickListener {
            adapter.cancelSelectionMode()
        }
        btnRename.setOnClickListener {
            adapter.renameSelectedItems()
        }
        btnDestails.setOnClickListener {
            adapter.showDetailsForSelectedItem()
        }
        btnDelete.setOnClickListener {
            adapter.deleteSelectedItems()
        }

        val btnSelectAll: Button = findViewById(R.id.btnSelectALl)
        btnSelectAll.setOnClickListener {
            adapter.selectAllItems()
        }

        viewall.setOnClickListener {
            val ll = findViewById<LinearLayout>(R.id.ll)
            if (cardview.visibility == View.VISIBLE) {
                cardview.visibility = View.GONE
                ll.visibility = View.VISIBLE
                viewall.text = "Go to dashboard"
            } else {
                cardview.visibility = View.VISIBLE
                ll.visibility = View.GONE
                viewall.text = "view all"
            }
        }


        val btnMove: Button = findViewById(R.id.btnMove)
        btnMove.setOnClickListener {
            pickDestinationDirectory()
        }


        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val path = Environment.getExternalStorageDirectory()
        val stat = StatFs(path.path)
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val percentage = 100 - ((availableBlocks.toFloat() / totalBlocks.toFloat()) * 100)

// Set initial progress to 0
        progressBar.progress = 0

// Animate progress from 0 to final percentage
        val animator = ObjectAnimator.ofInt(progressBar, "progress", 0, percentage.toInt())
        animator.duration = 2000 // You can adjust the duration as needed
        animator.start()


    }


    fun onCopyButtonClick(view: View) {
        val selectedFiles = adapter.getSelectedItems().map { it.file }
        copyFiles(selectedFiles)
        copiedFiles = adapter.getSelectedItems().map { it.file }
    }

    fun onPasteButtonClick(view: View) {
        if (copiedFiles != null) {
            pickDestinationDirectory()
        } else {
            Toast.makeText(this, "No files copied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestStoragePermission() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }

    private fun openFilePickerForDestinationDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, REQUEST_CODE_PICK_DESTINATION_DIRECTORY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_DESTINATION_DIRECTORY && resultCode == Activity.RESULT_OK) {
            handleDestinationDirectoryResult(data)
        } else if (requestCode == REQUEST_CODE_PICK_DIRECTORY && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            // Convert URI to File or obtain directory path from the URI
            // Pass the selected directory to the moveSelectedItems or pasteSelectedItems method in your adapter
            val selectedDirectory = uri?.let { documentUri ->
                DocumentFile.fromTreeUri(this, documentUri)?.let { documentFile ->
                    File(documentFile.uri.path!!)
                }
            }
            selectedDirectory?.let {
                // Pass the selected directory to the adapter to move or paste the selected items
                adapter.moveSelectedItems(it) // or fileAdapter.pasteSelectedItems(it)
            }
        }
    }


    private fun handleDestinationDirectoryResult(data: Intent?) {
        val uri = data?.data
        if (uri != null) {
            val destinationDirectory = DocumentFile.fromTreeUri(this, uri)?.let { documentFile ->
                // Use content resolver to resolve DocumentFile to File
                File(documentFile.uri.path)
            }
            if (destinationDirectory != null) {
                copiedFiles?.let { files ->
                    pasteFiles(files, destinationDirectory)
                }
            } else {
                Toast.makeText(this, "Failed to access selected directory", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(this, "No directory selected", Toast.LENGTH_SHORT).show()
        }
    }


    private fun copyFiles(filesToCopy: List<File>) {
        val destinationDirectory = getDestinationDirectory()
        filesToCopy.forEach { file ->
            val destinationFile = File(destinationDirectory, file.name)
            try {
                FileInputStream(file).use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to copy files", Toast.LENGTH_SHORT).show()
                return
            }
        }
        Toast.makeText(this, "Files copied successfully", Toast.LENGTH_SHORT).show()
    }

    private fun pasteFiles(filesToPaste: List<File>, destinationDirectory: File) {
        try {
            filesToPaste.forEach { file ->
                val destinationFile = File(destinationDirectory, file.name)
                FileInputStream(file).use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            Toast.makeText(this, "Files pasted successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to paste files: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun navigateToDirectory(directory: FileItem) {
        currentDirectory = File(directory.path)
        listFiles(currentDirectory)
    }

    private fun listFiles(directory: File) {
        val files = directory.listFiles()
        val fileItems = files?.map { file ->
            FileItem(file.name, file.absolutePath, file.isDirectory, file)
        }
        adapter.submitList(fileItems)
    }

    private fun openFile(file: File) {
        //val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", file)
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        val mimeType = getMimeType(file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(intent)
    }

    private fun getMimeType(file: File): String {
        val extension = file.extension.lowercase(Locale.getDefault())
        return when (extension) {
            "jpg", "jpeg", "png", "gif" -> "image/*"
            "mp3", "wav", "ogg" -> "audio/*"
            "mp4", "3gp", "avi" -> "video/*"
            else -> "*/*"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted, proceed with your app logic
            } else {
                // Permissions denied, show a message or handle the situation accordingly
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var backPressedOnce = false

    override fun onBackPressed() {
        if (adapter.isSelectionModeActive()) {
            adapter.cancelSelectionMode()
            return
        }

        if (currentDirectory != rootDirectory) {
            currentDirectory = currentDirectory.parentFile ?: rootDirectory
            listFiles(currentDirectory)
            return
        }

        if (backPressedOnce) {
            super.onBackPressed()
            return
        }

        this.backPressedOnce = true
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()

        android.os.Handler().postDelayed({
            backPressedOnce = false
        }, 2000) // Time window in milliseconds to press back button again to exit
    }


    private fun getDestinationDirectory(): File {
        return Environment.getExternalStorageDirectory()
    }


    private fun displayStorageInfo() {
        //    val totalStorageTextView = findViewById<TextView>(R.id.totalStorageTextView)
        val availableStorageTextView = findViewById<TextView>(R.id.storageInfoTextView)

        val totalSpace = getTotalInternalStorage()
        val availableSpace = getAvailableInternalStorage()

        val totalSpaceGB = convertBytesToGB(totalSpace)
        val availableSpaceGB = convertBytesToGB(availableSpace)

        //  totalStorageTextView.text = "Total Storage: $totalSpaceGB GB"
        availableStorageTextView.text = "$availableSpaceGB GB free of $totalSpaceGB GB"
    }


    private fun displayFileSizes() {

        val loadingiv = findViewById<ImageView>(R.id.loadingiv)
        val loadingiv2 = findViewById<ImageView>(R.id.loadingiv2)
        val loadingiv3 = findViewById<ImageView>(R.id.loadingiv3)
        val loadingiv4 = findViewById<ImageView>(R.id.loadingiv4)
        val loadingiv5 = findViewById<ImageView>(R.id.loadingiv5)
        val loadingiv6 = findViewById<ImageView>(R.id.loadingiv6)

        Glide.with(this)
            .load(R.drawable.loading)
            .into(loadingiv)

        Glide.with(this)
            .load(R.drawable.loading)
            .into(loadingiv2)

        Glide.with(this)
            .load(R.drawable.loading)
            .into(loadingiv3)

        Glide.with(this)
            .load(R.drawable.loading)
            .into(loadingiv4)

        Glide.with(this)
            .load(R.drawable.loading)
            .into(loadingiv5)

        Glide.with(this)
            .load(R.drawable.loading)
            .into(loadingiv6)


        val imagesTextView = findViewById<TextView>(R.id.imagesTextView)
        val ZippedTextView = findViewById<TextView>(R.id.ZippedTextView)
        val unknownTextView = findViewById<TextView>(R.id.unknownTextView)
        val musicTextView = findViewById<TextView>(R.id.musicTextView)
        val videosTextView = findViewById<TextView>(R.id.videosTextView)
        val documentsTextView = findViewById<TextView>(R.id.documentsTextView)

        // Start a coroutine
        CoroutineScope(Dispatchers.IO).launch {
            val storageDirectory = Environment.getExternalStorageDirectory()

            var totalImagesSize = 0L
            var totalZippedSize = 0L
            var totalMusicSize = 0L
            var totalVideosSize = 0L
            var totalDocumentsSize = 0L
            var totalOtherSize = 0L

            calculateDirectorySizes(storageDirectory) { file ->
                when {
                    file.isImageFile() -> totalImagesSize += file.length()
                    file.isZippedFile() -> totalZippedSize += file.length()
                    file.isMusicFile() -> totalMusicSize += file.length()
                    file.isVideoFile() -> totalVideosSize += file.length()
                    file.isDocumentFile() -> totalDocumentsSize += file.length()
                    else -> totalOtherSize += file.length()
                }
            }

            // Convert bytes to gigabytes
            val gb = 1024 * 1024 * 1024 // bytes in a gigabyte
            val imagesGb = totalImagesSize.toDouble() / gb
            val musicGb = totalMusicSize.toDouble() / gb
            val zippedGb = totalZippedSize.toDouble() / gb
            val videosGb = totalVideosSize.toDouble() / gb
            val documentsGb = totalDocumentsSize.toDouble() / gb

            val totalStorageSize = storageDirectory.totalSpace
            val totalKnownSize =
                totalImagesSize + totalZippedSize + totalMusicSize + totalVideosSize + totalDocumentsSize
            val totalUnknownSize = totalStorageSize - totalKnownSize

// Convert bytes to gigabytes
            val unknownGb = totalUnknownSize.toDouble() / gb

            // Update UI on the main thread
            withContext(Dispatchers.Main) {

                loadingiv.visibility = View.INVISIBLE
                loadingiv2.visibility = View.INVISIBLE
                loadingiv3.visibility = View.INVISIBLE
                loadingiv4.visibility = View.INVISIBLE
                loadingiv5.visibility = View.INVISIBLE
                loadingiv6.visibility = View.INVISIBLE

                imagesTextView.text = String.format("%.2f GB", imagesGb)
                imagesTextView.visibility = View.VISIBLE

                ZippedTextView.text = String.format("%.2f GB", zippedGb)
                ZippedTextView.visibility = View.VISIBLE

                musicTextView.text = String.format("%.2f GB", musicGb)
                musicTextView.visibility = View.VISIBLE

                videosTextView.text = String.format("%.2f GB", videosGb)
                videosTextView.visibility = View.VISIBLE

                documentsTextView.text = String.format("%.2f GB", documentsGb)
                documentsTextView.visibility = View.VISIBLE

                unknownTextView.text = String.format("%.2f GB", unknownGb / 3)
                unknownTextView.visibility = View.VISIBLE


            }
        }
    }


    private fun calculateDirectorySizes(directory: File, action: (File) -> Unit) {
        val files = directory.listFiles()
        files?.forEach { file ->
            if (file.isFile) {
                action.invoke(file)
            } else if (file.isDirectory) {
                calculateDirectorySizes(file, action)
            }
        }
    }


    private fun File.isImageFile(): Boolean {
        val extension = this.extension.lowercase(Locale.getDefault())
        return extension == "jpg" || extension == "jpeg" || extension == "png" || extension == "gif" || extension == "bmp"
    }

    private fun File.isMusicFile(): Boolean {
        val extension = this.extension.lowercase(Locale.getDefault())
        return extension == "mp3" || extension == "wav" || extension == "ogg" || extension == "m4a" || extension == "flac"
    }

    private fun File.isZippedFile(): Boolean {
        val extension = this.extension.lowercase(Locale.getDefault())
        return extension == "pdf"
    }

    private fun File.isVideoFile(): Boolean {
        val extension = this.extension.lowercase(Locale.getDefault())
        return extension == "mp4" || extension == "avi" || extension == "mkv" || extension == "mov" || extension == "wmv"
    }

    private fun File.isDocumentFile(): Boolean {
        // Add more document file extensions as needed
        val extension = this.extension.lowercase(Locale.getDefault())
        return extension == "pdf" || extension == "doc" || extension == "docx" || extension == "txt" || extension == "xls" || extension == "xlsx"
    }

    private fun getTotalInternalStorage(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        return stat.totalBytes
    }

    private fun getAvailableInternalStorage(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        return stat.availableBytes
    }

    private fun convertBytesToGB(bytes: Long): String {
        val gigabyte = 1024 * 1024 * 1024.0
        val formatter = DecimalFormat("0.##")
        return formatter.format(bytes / gigabyte)
    }


    private fun changeStatusBarColor(colorCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = android.graphics.Color.parseColor(colorCode)
        }
    }

    private fun pickDestinationDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, REQUEST_CODE_PICK_DIRECTORY)
    }


}




