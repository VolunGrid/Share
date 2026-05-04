package share.mx

import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class ActivityPreview : AppCompatActivity() {

    private lateinit var viewPagerPreview: ViewPager2
    private var startX = 0f
    private var startY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_preview)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewPagerPreview = findViewById(R.id.viewPagerPreview)

        val items: ArrayList<PreviewItem> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("items_preview", PreviewItem::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<PreviewItem>("items_preview") ?: arrayListOf()
        }

        val posicion = intent.getIntExtra("posicion", 0)

        viewPagerPreview.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        viewPagerPreview.adapter = PreviewSliderAdapter(items)
        viewPagerPreview.setCurrentItem(posicion, false)

        viewPagerPreview.getChildAt(0).setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                }

                MotionEvent.ACTION_UP -> {
                    val diffX = event.x - startX
                    val diffY = event.y - startY

                    if (abs(diffY) > abs(diffX) && diffY > 180) {
                        finish()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }
}