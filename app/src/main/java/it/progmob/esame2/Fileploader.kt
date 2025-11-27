package it.progmob.esame2.network

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File

// -----------------------------
// INTERFACCIA API
// -----------------------------
interface UploadAPI {

    @Multipart
    @POST("/upload")  // <-- cambia con il tuo endpoint
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): retrofit2.Response<Void>
}

// -----------------------------
// CLASSE PER INVIARE FILE
// -----------------------------
object FileUploader {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://TUO_SERVER.com")  // <-- cambia!
        .client(OkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(UploadAPI::class.java)

    suspend fun upload(path: String): Boolean {
        val file = File(path)
        if (!file.exists()) return false

        val requestBody = RequestBody.create(
            "audio/m4a".toMediaTypeOrNull(),
            file
        )

        val multipart = MultipartBody.Part.createFormData(
            "file",
            file.name,
            requestBody
        )

        val response = api.uploadFile(multipart)
        return response.isSuccessful
    }
}
