package share.mx

import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class FotoGridAdapter(
    private val lista: List<ArchivoItem>
) : RecyclerView.Adapter<FotoGridAdapter.FotoViewHolder>() {

    class FotoViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_foto_simple, parent, false) as ImageView
        return FotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        val item = lista[position]

        val thumb = holder.itemView.context.contentResolver.loadThumbnail(
            item.uri,
            Size(300, 300),
            null
        )
        holder.imageView.setImageBitmap(thumb)
    }

    override fun getItemCount(): Int = lista.size
}