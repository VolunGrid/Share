package share.mx

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ArchivoGridAdapter(
    private val lista: List<ArchivoItem>
) : RecyclerView.Adapter<ArchivoGridAdapter.ArchivoViewHolder>() {

    class ArchivoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgTipoIcono: ImageView = itemView.findViewById(R.id.imgTipoIcono)
        val imgPreview: ImageView = itemView.findViewById(R.id.imgPreview)
        val txtNombre: TextView = itemView.findViewById(R.id.txtNombre)
        val txtFecha: TextView = itemView.findViewById(R.id.txtFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchivoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_archivo_grid, parent, false)
        return ArchivoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArchivoViewHolder, position: Int) {
        val item = lista[position]
        val fechaFormateada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            .format(Date(item.fecha))

        holder.txtNombre.text = item.nombre
        holder.txtFecha.text = "${item.tipo} • $fechaFormateada"

        when (item.tipo) {
            "Imagen" -> {
                holder.imgTipoIcono.setImageResource(R.drawable.baseline_arrow_forward_ios_24)
                holder.imgPreview.setImageURI(item.uri)
            }
            "Música" -> {
                holder.imgTipoIcono.setImageResource(R.drawable.baseline_arrow_forward_ios_24)
                holder.imgPreview.setImageResource(R.drawable.baseline_arrow_forward_ios_24)
            }
            "Video" -> {
                holder.imgTipoIcono.setImageResource(R.drawable.baseline_arrow_forward_ios_24)
                holder.imgPreview.setImageResource(R.drawable.baseline_arrow_forward_ios_24)
            }
            "Documento" -> {
                holder.imgTipoIcono.setImageResource(R.drawable.baseline_arrow_forward_ios_24)
                holder.imgPreview.setImageResource(R.drawable.baseline_arrow_forward_ios_24)
            }
            else -> {
                holder.imgTipoIcono.setImageResource(R.drawable.baseline_arrow_forward_ios_24)
                holder.imgPreview.setImageResource(R.drawable.baseline_arrow_forward_ios_24)
            }
        }
    }

    override fun getItemCount(): Int = lista.size
}
