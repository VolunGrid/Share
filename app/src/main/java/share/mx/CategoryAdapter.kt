package share.mx

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val categories: List<CategoryItem>,
    private val onItemClick: (CategoryItem) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardContainer: LinearLayout = itemView.findViewById(R.id.cardContainer)
        val ivIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvSize: TextView = itemView.findViewById(R.id.tvCategorySize)
        val iconBackground: ImageView = itemView.findViewById(R.id.ivCategoryIcon)

        fun bind(item: CategoryItem) {
            tvName.text = item.name
            tvSize.text = item.sizeUsed
            ivIcon.setImageResource(item.iconRes)

            // Color de fondo de la tarjeta
            cardContainer.setBackgroundColor(
                ContextCompat.getColor(itemView.context, item.backgroundColor)
            )

            // Color del ícono
            ivIcon.setColorFilter(
                ContextCompat.getColor(itemView.context, item.iconColor)
            )

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_card, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount() = categories.size
}