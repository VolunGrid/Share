package share.mx

import android.os.Build
import android.provider.MediaStore
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
        val context = holder.itemView.context

        try {
            // Verificamos si el celular tiene Android 10 (API 29) o más nuevo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val thumb = context.contentResolver.loadThumbnail(
                    item.uri,
                    Size(300, 300),
                    null
                )
                holder.imageView.setImageBitmap(thumb)
            } else {
                // Fallback para celulares con Android 9 o más viejitos
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, item.uri)
                holder.imageView.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int = lista.size
}