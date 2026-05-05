package share.mx
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import share.mx.ArchivoItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ArchivoAdapter(
    private val lista: List<ArchivoItem>
) : RecyclerView.Adapter<ArchivoAdapter.ArchivoViewHolder>() {

    class ArchivoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgTipo: ImageView = itemView.findViewById(R.id.imgTipo)
        val txtNombre: TextView = itemView.findViewById(R.id.txtNombre)
        val txtInfo: TextView = itemView.findViewById(R.id.txtInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchivoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_archivo, parent, false)
        return ArchivoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArchivoViewHolder, position: Int) {
        val item = lista[position]
        val fechaFormateada = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(item.fecha))

        holder.txtNombre.text = item.nombre
        holder.txtInfo.text = "${item.tipo} • $fechaFormateada"

        holder.imgTipo.setImageResource(
            when (item.tipo) {
                "Imagen" -> R.drawable.baseline_arrow_forward_ios_24
                "Música" -> R.drawable.baseline_arrow_forward_ios_24
                "Video" -> R.drawable.baseline_arrow_forward_ios_24
                "Documento" -> R.drawable.baseline_arrow_forward_ios_24
                else -> R.drawable.baseline_arrow_forward_ios_24
            }
        )
    }

    override fun getItemCount(): Int = lista.size
}