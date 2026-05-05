package share.mx

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import kotlin.math.roundToInt

class HomeFragment : Fragment() {

    private lateinit var tvStoragePercentage: TextView
    private lateinit var tvStorageInfo: TextView
    private lateinit var progressStorage: ProgressBar
    private lateinit var rvCategories: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        tvStoragePercentage = view.findViewById(R.id.tvStoragePercentage)
        tvStorageInfo = view.findViewById(R.id.tvStorageInfo)
        progressStorage = view.findViewById(R.id.progressStorage)
        rvCategories = view.findViewById(R.id.rvCategories)

        updateStorageInfo()
        setupCategoriesRecycler()

        return view
    }

    private fun updateStorageInfo() {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.blockSizeLong * stat.blockCountLong
        val availableBytes = stat.blockSizeLong * stat.availableBlocksLong
        val usedBytes = totalBytes - availableBytes

        val totalGB = totalBytes / (1024.0 * 1024.0 * 1024.0)
        val usedGB = usedBytes / (1024.0 * 1024.0 * 1024.0)
        val percentage = ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).roundToInt()

        tvStoragePercentage.text = String.format(Locale.getDefault(), "%d%%", percentage)
        tvStorageInfo.text = String.format(
            Locale.getDefault(),
            "%.1f GB de %.1f GB usado",
            usedGB,
            totalGB
        )
        progressStorage.progress = percentage
    }

    private fun getUsedStorageBytes(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.blockSizeLong * stat.blockCountLong
        val availableBytes = stat.blockSizeLong * stat.availableBlocksLong
        return totalBytes - availableBytes
    }

    private fun setupCategoriesRecycler() {
        val hasPermission = hasStoragePermission()
        val usedStorage = getUsedStorageBytes()

        // Imágenes - siempre funciona bien con READ_MEDIA_IMAGES
        val imageSize = if (hasPermission) {
            getTotalSizeFromUri(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        } else 0L

        // Videos - siempre funciona bien con READ_MEDIA_VIDEO
        val videoSize = if (hasPermission) {
            getTotalSizeFromUri(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        } else 0L

        // Downloads - solo Android 10+
        val downloadsSize = if (hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getDownloadsSize()
        } else 0L

        // Documents - suma MediaStore.Files + Downloads como refuerzo
        val rawDocumentSize = if (hasPermission) getDocumentSize() else 0L
        val detectedDocumentSize = rawDocumentSize + downloadsSize

        // Others - primero intenta MediaStore.Files, si da 0 usa residual
        val mediaStoreOtherSize = if (hasPermission) {
            getOtherSize()
        } else 0L

        val remainingAfterMedia = usedStorage - (imageSize + videoSize)

        val safeRemaining = if (remainingAfterMedia > 0L) remainingAfterMedia else 0L

        val estimatedDocumentSize = if (detectedDocumentSize > 0L) {
            detectedDocumentSize
        } else {
            (safeRemaining * 0.35).toLong()
        }

        val remainingAfterDocuments = safeRemaining - estimatedDocumentSize

        val calculatedOther = if (remainingAfterDocuments > 0L) remainingAfterDocuments else 0L

        val otherSize = when {
            mediaStoreOtherSize > 0L -> mediaStoreOtherSize
            calculatedOther > 0L -> calculatedOther
            else -> 0L
        }

        val documentSize = estimatedDocumentSize
        val categories = listOf(
            CategoryItem(
                "Document",
                formatSize(documentSize, "No disponible"),
                android.R.drawable.ic_menu_edit,
                R.color.cardDocumentBg,
                R.color.cardDocumentIcon
            ),
            CategoryItem(
                "Videos",
                formatSize(videoSize),
                android.R.drawable.ic_media_play,
                R.color.cardVideoBg,
                R.color.cardVideoIcon
            ),
            CategoryItem(
                "Image",
                formatSize(imageSize),
                android.R.drawable.ic_menu_gallery,
                R.color.cardImageBg,
                R.color.cardImageIcon
            ),
            CategoryItem(
                "Others",
                formatSize(otherSize, "No disponible"),
                android.R.drawable.ic_menu_agenda,
                R.color.cardOthersBg,
                R.color.cardOthersIcon
            )
        )

        categoryAdapter = CategoryAdapter(categories) { category ->
            Toast.makeText(requireContext(), category.name, Toast.LENGTH_SHORT).show()
        }

        rvCategories.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = categoryAdapter
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val imagesGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED

            val videosGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED

            imagesGranted && videosGranted
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getTotalSizeFromUri(uri: android.net.Uri): Long {
        var totalSize = 0L
        val projection = arrayOf(MediaStore.MediaColumns.SIZE)

        requireContext().contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            while (cursor.moveToNext()) {
                totalSize += cursor.getLong(sizeColumn)
            }
        }

        return totalSize
    }

    private fun getDocumentSize(): Long {
        var totalSize = 0L
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        val selection = """
            ${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR
            ${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR
            ${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR
            ${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR
            ${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR
            ${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR
            ${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR
            ${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR
            ${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR
            ${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR
            ${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR
            ${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR
            ${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR
            ${MediaStore.Files.FileColumns.MIME_TYPE} = ?
        """.trimIndent()

        val selectionArgs = arrayOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv",
            "application/rtf",
            "application/zip",
            "application/x-rar-compressed",
            "application/vnd.android.package-archive",
            "application/epub+zip"
        )

        requireContext().contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            while (cursor.moveToNext()) {
                totalSize += cursor.getLong(sizeColumn)
            }
        }

        return totalSize
    }

    private fun getOtherSize(): Long {
        var totalSize = 0L
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        requireContext().contentResolver.query(
            collection,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

            while (cursor.moveToNext()) {
                val size = cursor.getLong(sizeColumn)
                val mime = cursor.getString(mimeColumn) ?: ""

                val isImage = mime.startsWith("image/")
                val isVideo = mime.startsWith("video/")
                val isAudio = mime.startsWith("audio/")
                val isDocument = mime == "application/pdf" ||
                        mime == "application/msword" ||
                        mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
                        mime == "application/vnd.ms-excel" ||
                        mime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
                        mime == "application/vnd.ms-powerpoint" ||
                        mime == "application/vnd.openxmlformats-officedocument.presentationml.presentation" ||
                        mime == "text/plain" ||
                        mime == "text/csv" ||
                        mime == "application/rtf" ||
                        mime == "application/zip" ||
                        mime == "application/x-rar-compressed" ||
                        mime == "application/vnd.android.package-archive" ||
                        mime == "application/epub+zip"

                if (!isImage && !isVideo && !isAudio && !isDocument) {
                    totalSize += size
                }
            }
        }

        return totalSize
    }

    private fun getDownloadsSize(): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0L

        var totalSize = 0L
        val projection = arrayOf(MediaStore.MediaColumns.SIZE)

        requireContext().contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            while (cursor.moveToNext()) {
                totalSize += cursor.getLong(sizeColumn)
            }
        }

        return totalSize
    }

    private fun formatSize(sizeBytes: Long, fallbackLabel: String = "0 B"): String {
        if (sizeBytes <= 0L) return fallbackLabel

        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            sizeBytes >= gb -> String.format(Locale.getDefault(), "%.1f GB usado", sizeBytes / gb)
            sizeBytes >= mb -> String.format(Locale.getDefault(), "%.1f MB usado", sizeBytes / mb)
            sizeBytes >= kb -> String.format(Locale.getDefault(), "%.1f KB usado", sizeBytes / kb)
            else -> String.format(Locale.getDefault(), "%d B usado", sizeBytes)
        }
    }
}