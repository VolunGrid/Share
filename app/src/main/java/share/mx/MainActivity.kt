package share.mx

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.net.Socket
import org.json.JSONObject
import android.util.Log

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // ------ INICIO PRUEBA DE SOCKET (SE EJECUTA SOLA AL ABRIR LA APP) ------
        GlobalScope.launch(Dispatchers.IO) {
            try {

                val socket = Socket("192.168.1.75", 6881)

                val jsonEnvio = JSONObject().apply {
                    put("action", "get_peers")
                }

                val salida = PrintWriter(socket.outputStream, true)
                salida.println(jsonEnvio.toString())

                val entrada = socket.inputStream.bufferedReader()
                val respuestaServidor = entrada.readLine()

                Log.d("P2P_TEST", "📥 Respuesta de Python: $respuestaServidor")

                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("P2P_TEST", "❌ Error de conexión: ${e.message}")
            }
        }
        // ------ FIN PRUEBA DE SOCKET ------


        try {
            val mainView = findViewById<android.view.View>(R.id.main)
            val btnContinuar = findViewById<ImageButton>(R.id.btnContinuar)

            if (mainView != null) {
                ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    insets
                }
            }

            btnContinuar?.setOnClickListener {
                val intent = Intent(this, ScreenPermission::class.java)
                startActivity(intent)
            }

            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } catch (e: Exception) {
            println("Aviso: No se cargaron los elementos visuales, pero el socket sigue intentando.")
        }
    }
}