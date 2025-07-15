package com.jhone.app_inventory.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    // NUEVO: Propiedad para almacenar el rol del usuario ("admin" o "asesor")
    var userRole by mutableStateOf<String?>(null)
        private set

    init {
        // Si el usuario ya está logueado, se obtiene su rol
        if (auth.currentUser != null) {
            fetchUserRole()
        }
    }

    fun fetchUserRole() {
        val uid = auth.currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    userRole = document.getString("role")
                }
            }
            .addOnFailureListener {
                // Puedes registrar el error o asignar un rol por defecto
                userRole = "asesor"
            }
    }

    // Funciones de signIn, signUp y signOut (ya existentes)
    fun signIn(email: String, password: String, onComplete: (error: String?) -> Unit) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email.lowercase(), password).await()
                fetchUserRole() // Obtiene el rol después de iniciar sesión
                onComplete(null)
            } catch (e: Exception) {
                onComplete(e.message)
            }
        }
    }

    fun signUp(email: String, password: String, onComplete: (error: String?) -> Unit) {
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email.lowercase(), password).await()
                // Asigna el rol: si es el admin, "admin"; de lo contrario, "asesor"
                val role = if (email.lowercase() == "maylustore.truj@gmail.com") "admin" else "asesor"
                val uid = auth.currentUser?.uid
                val userData = mapOf("email" to email, "role" to role)
                uid?.let {
                    FirebaseFirestore.getInstance().collection("users")
                        .document(it)
                        .set(userData)
                        .await()
                }
                fetchUserRole() // Obtiene el rol para actualizar la variable
                onComplete(null)
            } catch (e: Exception) {
                onComplete(e.message)
            }
        }
    }

    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    fun signOut() {
        auth.signOut()
        userRole = null // Reiniciamos el rol al cerrar sesión
    }
}