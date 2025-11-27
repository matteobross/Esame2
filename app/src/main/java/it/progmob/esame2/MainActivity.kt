package it.progmob.esame2


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        val welcomeText = findViewById<TextView>(R.id.txtWelcome)
        val logoutBtn = findViewById<Button>(R.id.btnLogout)
        val btnAvanti = findViewById<Button>(R.id.btnAvanti)

                //va alla newrecactivity
        btnAvanti.setOnClickListener {
            val intent = Intent(this, NewRecActivity::class.java)
            startActivity(intent)
        }

        // Mostra email dell’utente loggato
        val userEmail = auth.currentUser?.email
        welcomeText.text = "Benvenuto: $userEmail"

        logoutBtn.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
