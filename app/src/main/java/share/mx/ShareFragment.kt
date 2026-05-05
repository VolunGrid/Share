package share.mx

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShareFragment : Fragment(R.layout.fragment_share) {

    companion object {
        private const val TAG = "ShareDebug"
        private const val PREFS = "share_prefs"
        private const val KEY_TREE_URIS = "tree_uris"
    }

    private lateinit var rvArchivos: RecyclerView
    private lateinit var adapter: GaleriaAdapter

    private val listaGaleria = mutableListOf<GaleriaItem>()
    private val archivosSeleccionados = linkedSetOf<ArchivoItem>()
    private val archivosCargados = mutableListOf<ArchivoItem>()

    private var modoSeleccion = false

    private lateinit var fabMotionLayout: MotionLayout
    private lateinit var fabMain: FloatingActionButton
    private lateinit var fabActualizar: FloatingActionButton
    private lateinit var fabCarpeta: FloatingActionButton
    private lateinit var fabEnviar: FloatingActionButton
    private lateinit var fabOverlay: View

    private val permisosLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedido = permisos.values.all { it }
        if (concedido) {
            cargarArchivosMixtos()
        } else if (isAdded) {
            Toast.makeText(requireContext(), "Permisos denegados", Toast.LENGTH_SHORT).show()
        }
    }

    private val carpetaLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null || !isAdded) return@registerForActivityResult

        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        try {
            requireContext().contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo persistir permiso para $uri", e)
        }

        guardarTreeUri(uri)
        cargarArchivosMixtos()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvArchivos = view.findViewById(R.id.rvArchivos)
        fabMotionLayout = view.findViewById(R.id.fabMotionLayout)
        fabMain = view.findViewById(R.id.fabMain)
        fabEnviar = view.findViewById(R.id.fabEnviar)
        fabCarpeta = view.findViewById(R.id.fabCarpeta)
        fabActualizar = view.findViewById(R.id.fabActualizar)
        fabOverlay = view.findViewById(R.id.fabOverlay)

        fabMain.setOnClickListener {
            toggleFabMenu()
        }

        fabOverlay.setOnClickListener {
            cerrarFabMenu()
        }

        fabEnviar.setOnClickListener {
            cerrarFabMenu()
            compartirSeleccionados()
        }

        fabCarpeta.setOnClickListener {
            cerrarFabMenu()
            carpetaLauncher.launch(null)
        }

        fabActualizar.setOnClickListener {
            cerrarFabMenu()
            cargarArchivosMixtos()
        }

        val layoutManager = GridLayoutManager(requireContext(), 3)
        rvArchivos.layoutManager = layoutManager
        rvArchivos.setHasFixedSize(true)
        rvArchivos.itemAnimator = null
        rvArchivos.setItemViewCacheSize(20)

        adapter = GaleriaAdapter(
            items = listaGaleria,
            seleccionados = archivosSeleccionados,
            enModoSeleccion = { modoSeleccion },
            onClickArchivo = { archivo ->
                if (modoSeleccion) {
                    toggleSeleccion(archivo)
                } else {
                    abrirArchivo(archivo)
                }
            },
            onLongClickArchivo = { archivo ->
                if (!modoSeleccion) modoSeleccion = true
                toggleSeleccion(archivo)
            }
        )

        rvArchivos.adapter = adapter

        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (adapter.getItemViewType(position)) {
                    0, 1 -> 3
                    else -> 1
                }
            }
        }

        fabMotionLayout.post {
            //fabMotionLayout.transitionToStart()
            fabMotionLayout.setProgress(0f)
        }

        verificarPermisosYCargar()
    }

    private fun toggleFabMenu() {
        if (fabMotionLayout.progress > 0.5f) {
            cerrarFabMenu()
        } else {
            abrirFabMenu()
        }
    }

    private fun abrirFabMenu() {
        fabMotionLayout.transitionToEnd()
    }

    private fun cerrarFabMenu() {
        fabMotionLayout.transitionToStart()
    }

    private fun verificarPermisosYCargar() {
        val permisos = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val imgGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED

            val videoGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED

            if (!imgGranted) permisos.add(Manifest.permission.READ_MEDIA_IMAGES)
            if (!videoGranted) permisos.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            val storageGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            if (!storageGranted) {
                permisos.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permisos.isEmpty()) {
            cargarArchivosMixtos()
        } else {
            permisosLauncher.launch(permisos.toTypedArray())
        }
    }

    private fun cargarArchivosMixtos() {
        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                val imagenes = obtenerImagenes()
                val videos = obtenerVideos()
                val documentos = obtenerDocumentosDesdeCarpetasPersistidas()

                (imagenes + videos + documentos)
                    .distinctBy { it.uri.toString() }
                    .sortedByDescending { it.fecha }
            }

            archivosCargados.clear()
            archivosCargados.addAll(resultado)
            reconstruirGaleria()
        }
    }

    private fun guardarTreeUri(uri: Uri) {
        val prefs = requireContext().getSharedPreferences(PREFS, 0)
        val actuales = obtenerTreeUrisGuardadas().toMutableSet()
        actuales.add(uri.toString())

        val jsonArray = JSONArray()
        actuales.forEach { jsonArray.put(it) }

        prefs.edit().putString(KEY_TREE_URIS, jsonArray.toString()).apply()
    }

    private fun obtenerTreeUrisGuardadas(): List<String> {
        val prefs = requireContext().getSharedPreferences(PREFS, 0)
        val raw = prefs.getString(KEY_TREE_URIS, null) ?: return emptyList()

        return try {
            val jsonArray = JSONArray(raw)
            MutableList(jsonArray.length()) { index -> jsonArray.getString(index) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun obtenerDocumentosDesdeCarpetasPersistidas(): List<ArchivoItem> {
        val uris = obtenerTreeUrisGuardadas()
        if (uris.isEmpty()) return emptyList()

        val lista = mutableListOf<ArchivoItem>()

        uris.forEach { uriString ->
            try {
                val treeUri = Uri.parse(uriString)
                val root = DocumentFile.fromTreeUri(requireContext(), treeUri) ?: return@forEach
                recorrerCarpeta(root, lista)
            } catch (e: Exception) {
                Log.e(TAG, "Error leyendo carpeta autorizada: $uriString", e)
            }
        }

        return lista
    }

    private fun recorrerCarpeta(dir: DocumentFile, lista: MutableList<ArchivoItem>) {
        dir.listFiles().forEach { file ->
            if (file.isDirectory) {
                recorrerCarpeta(file, lista)
            } else if (file.isFile) {
                val nombre = file.name ?: "Sin nombre"
                val mime = file.type ?: "*/*"

                if (
                    nombre.endsWith(".pdf", true) ||
                    nombre.endsWith(".doc", true) ||
                    nombre.endsWith(".docx", true) ||
                    nombre.endsWith(".xls", true) ||
                    nombre.endsWith(".xlsx", true) ||
                    nombre.endsWith(".ppt", true) ||
                    nombre.endsWith(".pptx", true) ||
                    nombre.endsWith(".zip", true) ||
                    nombre.endsWith(".rar", true) ||
                    nombre.endsWith(".txt", true) ||
                    nombre.endsWith(".csv", true) ||
                    nombre.endsWith(".apk", true) ||
                    mime.startsWith("text/")
                ) {
                    lista.add(
                        ArchivoItem(
                            nombre = nombre,
                            tipo = "Documento",
                            fecha = file.lastModified(),
                            uri = file.uri,
                            mimeType = mime
                        )
                    )
                }
            }
        }
    }

    private fun reconstruirGaleria() {
        val nuevaLista = mutableListOf<GaleriaItem>()

        var ultimoMes = ""
        var ultimoDia = ""

        val formatoMes = SimpleDateFormat("MMMM yyyy", Locale("es", "MX"))
        val formatoDia = SimpleDateFormat("dd 'de' MMMM", Locale("es", "MX"))

        for (archivo in archivosCargados) {
            val fecha = Date(archivo.fecha)
            val mesActual = formatoMes.format(fecha)
            val diaActual = formatoDia.format(fecha)

            if (mesActual != ultimoMes) {
                nuevaLista.add(
                    GaleriaItem.HeaderMes(
                        mesActual.replaceFirstChar { it.uppercase() }
                    )
                )
                ultimoMes = mesActual
                ultimoDia = ""
            }

            if (diaActual != ultimoDia) {
                nuevaLista.add(
                    GaleriaItem.HeaderDia(
                        diaActual.replaceFirstChar { it.uppercase() }
                    )
                )
                ultimoDia = diaActual
            }

            nuevaLista.add(GaleriaItem.ArchivoContenido(archivo))
        }

        listaGaleria.clear()
        listaGaleria.addAll(nuevaLista)
        adapter.notifyDataSetChanged()
    }

    private fun obtenerImagenes(): List<ArchivoItem> {
        val lista = mutableListOf<ArchivoItem>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.MIME_TYPE
        )

        requireContext().contentResolver.query(
            collection, projection, null, null, null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val nombre = cursor.getString(nameCol) ?: "Sin nombre"
                val fecha = cursor.getLong(dateCol) * 1000L
                val mime = cursor.getString(mimeCol) ?: "image/*"
                val uri = Uri.withAppendedPath(collection, id.toString())

                lista.add(
                    ArchivoItem(
                        nombre = nombre,
                        tipo = "Imagen",
                        fecha = fecha,
                        uri = uri,
                        mimeType = mime
                    )
                )
            }
        }

        return lista
    }

    private fun obtenerVideos(): List<ArchivoItem> {
        val lista = mutableListOf<ArchivoItem>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.MIME_TYPE
        )

        requireContext().contentResolver.query(
            collection, projection, null, null, null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val nombre = cursor.getString(nameCol) ?: "Sin nombre"
                val fecha = cursor.getLong(dateCol) * 1000L
                val duracion = cursor.getLong(durationCol)
                val mime = cursor.getString(mimeCol) ?: "video/*"
                val uri = Uri.withAppendedPath(collection, id.toString())

                lista.add(
                    ArchivoItem(
                        nombre = nombre,
                        tipo = "Video",
                        fecha = fecha,
                        uri = uri,
                        duracion = duracion,
                        mimeType = mime
                    )
                )
            }
        }

        return lista
    }

    private fun abrirArchivo(archivo: ArchivoItem) {
        when (archivo.tipo) {
            "Imagen", "Video" -> abrirPreview(archivo)
            else -> abrirDocumento(archivo)
        }
    }

    private fun abrirPreview(archivo: ArchivoItem) {
        val archivosPreview = ArrayList(
            listaGaleria
                .filterIsInstance<GaleriaItem.ArchivoContenido>()
                .filter { it.archivo.tipo == "Imagen" || it.archivo.tipo == "Video" }
                .map {
                    PreviewItem(
                        uri = it.archivo.uri.toString(),
                        tipo = it.archivo.tipo,
                        nombre = it.archivo.nombre,
                        duracion = it.archivo.duracion
                    )
                }
        )

        val posicionInicial = archivosPreview.indexOfFirst { it.uri == archivo.uri.toString() }

        val intent = Intent(requireContext(), ActivityPreview::class.java).apply {
            putParcelableArrayListExtra("items_preview", archivosPreview)
            putExtra("posicion", posicionInicial)
        }

        startActivity(intent)
    }

    private fun abrirDocumento(archivo: ArchivoItem) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(archivo.uri, archivo.mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Abrir archivo"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No hay app para abrir este archivo", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No se pudo abrir el archivo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleSeleccion(archivo: ArchivoItem) {
        if (archivosSeleccionados.contains(archivo)) {
            archivosSeleccionados.remove(archivo)
        } else {
            archivosSeleccionados.add(archivo)
        }

        if (archivosSeleccionados.isEmpty()) {
            modoSeleccion = false
        }

        adapter.notifyDataSetChanged()
    }

    private fun compartirSeleccionados() {
        if (archivosSeleccionados.isEmpty()) {
            Toast.makeText(requireContext(), "Selecciona al menos un archivo", Toast.LENGTH_SHORT).show()
            return
        }

        val uris = ArrayList<Uri>()
        archivosSeleccionados.forEach { uris.add(it.uri) }

        val mimeTypes = archivosSeleccionados.map { it.mimeType }.distinct()
        val tipoFinal = if (mimeTypes.size == 1) mimeTypes.first() else "*/*"

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = tipoFinal
        }

        startActivity(Intent.createChooser(shareIntent, "Compartir archivos"))
    }
}