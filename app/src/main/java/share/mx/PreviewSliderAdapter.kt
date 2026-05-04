package share.mx

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PreviewSliderAdapter(
    private val items: List<PreviewItem>
) : RecyclerView.Adapter<PreviewSliderAdapter.PreviewViewHolder>() {

    class PreviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgPreview: ImageView = view.findViewById(R.id.imgPreviewPage)
        val videoPreview: VideoView = view.findViewById(R.id.videoPreviewPage)
        val layoutVideoBadge: LinearLayout = view.findViewById(R.id.layoutVideoBadge)
        val txtDuracion: TextView = view.findViewById(R.id.txtDuracionPreview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_preview_page, parent, false)
        return PreviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        val item = items[position]

        holder.videoPreview.stopPlayback()
        holder.videoPreview.visibility = View.GONE
        holder.imgPreview.visibility = View.GONE
        holder.layoutVideoBadge.visibility = View.GONE

        if (item.tipo == "Imagen") {
            holder.imgPreview.visibility = View.VISIBLE

            Glide.with(holder.itemView.context)
                .load(item.uri)
                .into(holder.imgPreview)

        } else if (item.tipo == "Video") {
            holder.videoPreview.visibility = View.VISIBLE
            holder.layoutVideoBadge.visibility = View.VISIBLE
            holder.txtDuracion.text = formatearDuracion(item.duracion)

            val mediaController = MediaController(holder.itemView.context)
            mediaController.setAnchorView(holder.videoPreview)

            holder.videoPreview.setMediaController(mediaController)
            holder.videoPreview.setVideoURI(Uri.parse(item.uri))
            holder.videoPreview.setOnPreparedListener {
                holder.videoPreview.start()
            }
        }
    }

    override fun onViewRecycled(holder: PreviewViewHolder) {
        super.onViewRecycled(holder)
        holder.videoPreview.stopPlayback()
    }

    override fun getItemCount(): Int = items.size

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