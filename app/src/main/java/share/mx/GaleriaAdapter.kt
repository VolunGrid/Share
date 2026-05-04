package share.mx

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class GaleriaAdapter(
    private val items: List<GaleriaItem>,
    private val seleccionados: Set<ArchivoItem>,
    private val enModoSeleccion: () -> Boolean,
    private val onClickArchivo: (ArchivoItem) -> Unit,
    private val onLongClickArchivo: (ArchivoItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_MES = 0
        const val TYPE_DIA = 1
        const val TYPE_ARCHIVO = 2
    }

    class MesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMes: TextView = view.findViewById(R.id.txtHeaderMes)
    }

    class DiaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtDia: TextView = view.findViewById(R.id.txtHeaderDia)
    }

    class ArchivoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgPreview: ImageView = view.findViewById(R.id.imgPreview)
        val layoutVideoInfo: LinearLayout = view.findViewById(R.id.layoutVideoInfo)
        val txtDuracion: TextView = view.findViewById(R.id.txtDuracion)
        val overlaySeleccion: View = view.findViewById(R.id.overlaySeleccion)
        val imgCheckSeleccion: ImageView = view.findViewById(R.id.imgCheckSeleccion)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is GaleriaItem.HeaderMes -> TYPE_MES
            is GaleriaItem.HeaderDia -> TYPE_DIA
            is GaleriaItem.ArchivoContenido -> TYPE_ARCHIVO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TYPE_MES -> MesViewHolder(inflater.inflate(R.layout.item_header_mes, parent, false))
            TYPE_DIA -> DiaViewHolder(inflater.inflate(R.layout.item_header_dia, parent, false))
            else -> ArchivoViewHolder(inflater.inflate(R.layout.item_foto_simple, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is GaleriaItem.HeaderMes -> {
                (holder as MesViewHolder).txtMes.text = item.titulo
            }

            is GaleriaItem.HeaderDia -> {
                (holder as DiaViewHolder).txtDia.text = item.titulo
            }

            is GaleriaItem.ArchivoContenido -> {
                val archivoHolder = holder as ArchivoViewHolder
                val context = archivoHolder.itemView.context
                val archivo = item.archivo
                val estaSeleccionado = seleccionados.contains(archivo)

                archivoHolder.overlaySeleccion.visibility =
                    if (estaSeleccionado) View.VISIBLE else View.GONE

                archivoHolder.imgCheckSeleccion.visibility =
                    if (estaSeleccionado) View.VISIBLE else View.GONE

                archivoHolder.itemView.setOnClickListener {
                    onClickArchivo(archivo)
                }

                archivoHolder.itemView.setOnLongClickListener {
                    onLongClickArchivo(archivo)
                    true
                }

                when (archivo.tipo) {
                    "Imagen" -> {
                        archivoHolder.layoutVideoInfo.visibility = View.GONE

                        Glide.with(context)
                            .load(archivo.uri)
                            .centerCrop()
                            .override(300, 300)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(archivoHolder.imgPreview)
                    }

                    "Video" -> {
                        archivoHolder.layoutVideoInfo.visibility = View.VISIBLE
                        archivoHolder.txtDuracion.text = formatearDuracion(archivo.duracion)

                        Glide.with(context)
                            .load(archivo.uri)
                            .centerCrop()
                            .override(300, 300)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(archivoHolder.imgPreview)
                    }

                    "Documento" -> {
                        archivoHolder.layoutVideoInfo.visibility = View.GONE

                        val icono = when {
                            archivo.mimeType.equals("application/pdf", true) ->
                                android.R.drawable.ic_menu_save

                            archivo.mimeType.contains("word", true) ||
                                    archivo.nombre.endsWith(".doc", true) ||
                                    archivo.nombre.endsWith(".docx", true) ->
                                android.R.drawable.ic_menu_edit

                            archivo.mimeType.contains("excel", true) ||
                                    archivo.mimeType.contains("spreadsheet", true) ||
                                    archivo.nombre.endsWith(".xls", true) ||
                                    archivo.nombre.endsWith(".xlsx", true) ->
                                android.R.drawable.ic_menu_agenda

                            archivo.mimeType.contains("zip", true) ||
                                    archivo.nombre.endsWith(".zip", true) ->
                                android.R.drawable.ic_menu_upload

                            else ->
                                android.R.drawable.ic_menu_report_image
                        }

                        archivoHolder.imgPreview.setImageResource(icono)
                        archivoHolder.imgPreview.scaleType = ImageView.ScaleType.CENTER_INSIDE
                        archivoHolder.imgPreview.setPadding(24, 24, 24, 24)
                    }

                    else -> {
                        archivoHolder.layoutVideoInfo.visibility = View.GONE
                        archivoHolder.imgPreview.setImageResource(android.R.drawable.ic_menu_report_image)
                    }
                }

                if (archivo.tipo != "Documento") {
                    archivoHolder.imgPreview.scaleType = ImageView.ScaleType.CENTER_CROP
                    archivoHolder.imgPreview.setPadding(0, 0, 0, 0)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)

        if (holder is ArchivoViewHolder) {
            Glide.with(holder.itemView.context).clear(holder.imgPreview)
        }
    }

    private fun formatearDuracion(duracionMs: Long): String {
        val totalSegundos = duracionMs / 1000
        val horas = totalSegundos / 3600
        val minutos = (totalSegundos % 3600) / 60
        val segundos = totalSegundos % 60

        return if (horas > 0) {
            String.format("%d:%02d:%02d", horas, minutos, segundos)
        } else {
            String.format("%d:%02d", minutos, segundos)
        }
    }
}