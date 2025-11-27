package it.progmob.esame2


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inizializza l'istanza di Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Riferimenti alle view
        val email = findViewById<EditText>(R.id.editEmail)
        val password = findViewById<EditText>(R.id.editPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoRegister = findViewById<Button>(R.id.btnGoRegister)

        // Listener per il pulsante di registrazione: avvia RegisterActivity
        btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Listener per il pulsante di login
        btnLogin.setOnClickListener {
            val userEmail = email.text.toString().trim()
            val userPassword = password.text.toString().trim()

            // Controllo che i campi non siano vuoti
            if (userEmail.isEmpty() || userPassword.isEmpty()) {
                Toast.makeText(this, "Inserisci email e password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Esegui il login con Firebase
            auth.signInWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Login riuscito: avvia MainActivity
                        Toast.makeText(this, "Accesso effettuato", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish() // Chiudi LoginActivity
                    } else {
                        // Login fallito: mostra un messaggio di errore
                        Toast.makeText(this, "Errore: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}