package share.mx

import android.net.Uri


data class ArchivoItem(
    val nombre: String,
    val tipo: String,          // "Imagen", "Video", "Documento"
    val fecha: Long,
    val uri: Uri,
    val duracion: Long = 0L,
    val mimeType: String = "*/*"

)