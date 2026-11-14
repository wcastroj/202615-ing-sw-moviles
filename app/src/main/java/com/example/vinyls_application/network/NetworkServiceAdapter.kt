package com.example.vinyls_application.network

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import com.example.vinyls_application.models.Album
import com.example.vinyls_application.models.AlbumDetail
import com.example.vinyls_application.models.Band
import com.example.vinyls_application.models.Comment
import com.example.vinyls_application.models.Musician
import com.example.vinyls_application.models.Performer
import com.example.vinyls_application.models.PerformerBase
import com.example.vinyls_application.models.PerformerPrize
import com.example.vinyls_application.models.PerformerType
import com.example.vinyls_application.models.Track
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.Int
import kotlin.String
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * @param queue cola de Volley; en las pruebas se pasa un mock.
 * @param dispatcher hilo donde se hace el trabajo de CPU (parsear JSON, y en los repositorios unir u
 * ordenar listas). Por defecto [Dispatchers.Default], fuera del hilo principal. En las pruebas
 * unitarias se pasa un TestDispatcher, y en las E2E con IdlingResource, Dispatchers.Main.
 */
class NetworkServiceAdapter(
    context: Context,
    queue: RequestQueue? = null,
    val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    companion object{
        @VisibleForTesting
        var BASE_URL = com.example.vinyls_application.BuildConfig.BACKEND_URL

        /**
         * Tiempo máximo de espera por intento. El valor por defecto de Volley (2,5 s) es muy corto
         * con un backend o un dispositivo lento: la petición se abandona y la app muestra error.
         */
        const val REQUEST_TIMEOUT_MS = 10_000

        var instance: NetworkServiceAdapter? = null
        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: NetworkServiceAdapter(context).also {
                    instance = it
                }
            }

        fun parseAlbums(response: String): List<Album> {
            val resp = JSONArray(response)
            val albums = mutableListOf<Album>()

            if (resp.length() == 0) {
                return albums as List<Album>
            }

            for (i in 0 until resp.length()) {
                val album = resp.getJSONObject(i)

                albums.add(
                    Album(
                        albumId = album.getInt("id"),
                        cover = album.getString("cover"),
                        name = album.getString("name"),
                        genre = album.getString("genre"),
                        recordLabel = album.getString("recordLabel"),
                        releaseDate = album.getString("releaseDate"),
                        description = album.getString("description")
                    )
                )
            }

            return albums as ArrayList<Album>
        }
        fun parseBands(response: String): List<Performer> {
            val resp = JSONArray(response)
            val performers = mutableListOf<Performer>()

            if (resp.length() == 0) {
                return performers as List<Performer>
            }

            for (i in 0 until resp.length()) {
                val performer = resp.getJSONObject(i)

                performers.add(
                    Performer(
                        id = performer.getInt("id"),
                        name = performer.getString("name"),
                        image = performer.getString("image"),
                        description = performer.getString("description"),
                        type = PerformerType.BAND,
                        creationDate = performer.getString("creationDate")
                    )
                )
            }

            return performers as ArrayList<Performer>
        }
        fun parseMusicians(response: String): List<Performer> {
            val resp = JSONArray(response)
            val performers = mutableListOf<Performer>()

            if (resp.length() == 0) {
                return performers as List<Performer>
            }

            for (i in 0 until resp.length()) {
                val performer = resp.getJSONObject(i)

                performers.add(
                    Performer(
                        id = performer.getInt("id"),
                        name = performer.getString("name"),
                        image = performer.getString("image"),
                        description = performer.getString("description"),
                        type = PerformerType.MUSICIAN,
                        birthDate = performer.getString("birthDate")
                    )
                )
            }

            return performers as ArrayList<Performer>
        }

        fun parseAlbumDetail(response: String): AlbumDetail {
            val resp = JSONObject(response)
            val tracks = mutableListOf<Track>()
            val tracksJson = resp.getJSONArray("tracks")

            for (i in 0 until tracksJson.length()) {
                val t = tracksJson.getJSONObject(i)
                tracks.add(
                    Track(
                        trackId = t.getInt("id"),
                        name = t.getString("name"),
                        duration = t.getString("duration")
                    )
                )
            }

            val performers = mutableListOf<Performer>()
            val performersJson = resp.getJSONArray("performers")
            for (i in 0 until performersJson.length()) {
                val p = performersJson.getJSONObject(i)
                performers.add(
                    Performer(
                        id = p.getInt("id"),
                        name = p.getString("name"),
                        image = p.getString("image"),
                        description = p.getString("description"),
                        // Los performers de un álbum vienen mezclados: los músicos traen birthDate
                        type = if (p.has("birthDate")) PerformerType.MUSICIAN else PerformerType.BAND,
                        birthDate = if (p.has("birthDate")) p.getString("birthDate") else null,
                        creationDate = if (p.has("creationDate")) p.getString("creationDate") else null
                    )
                )
            }
            val comments = mutableListOf<Comment>()
            val commentsJson = resp.getJSONArray("comments")

            for (i in 0 until commentsJson.length()) {
                val c = commentsJson.getJSONObject(i)
                comments.add(
                    Comment(
                        commentId = c.getInt("id"),
                        description = c.getString("description"),
                        rating = c.getString("rating")
                    )
                )
            }

            val albumDetail = AlbumDetail(
                albumId = resp.getInt("id"),
                name = resp.getString("name"),
                cover = resp.getString("cover"),
                releaseDate = resp.getString("releaseDate"),
                description = resp.getString("description"),
                genre = resp.getString("genre"),
                recordLabel = resp.getString("recordLabel"),
                tracks = tracks,
                performers = performers,
                comments = comments
            )
            return albumDetail
        }

        fun parsePerformer(response: String): PerformerBase {
            val performer = JSONObject(response)

            val id = performer.getInt("id")
            val name = performer.getString("name")
            val image = performer.getString("image")
            val description = performer.getString("description")

            val albums = parseAlbums(performer.getJSONArray("albums").toString())
            val prizes = parsePrizes(performer.getJSONArray("performerPrizes"))

            return if (performer.has("creationDate")) {
                Band(
                    id = id,
                    name = name,
                    image = image,
                    description = description,
                    albums = albums,
                    performerPrizes = prizes,
                    creationDate = performer.getString("creationDate")
                )
            } else {
                Musician(
                    id = id,
                    name = name,
                    image = image,
                    description = description,
                    albums = albums,
                    performerPrizes = prizes,
                    birthDate = performer.getString("birthDate")
                )
            }
        }

        private fun parsePrizes(resp: JSONArray): List<PerformerPrize> {
            val prizes = mutableListOf<PerformerPrize>()

            if (resp.length() == 0) {
                return prizes as List<PerformerPrize>
            }

            for (i in 0 until resp.length()) {
                val prize = resp.getJSONObject(i)

                prizes.add(
                    PerformerPrize(
                        id = prize.getInt("id"),
                        premiationDate = prize.getString("premiationDate"),
                    )
                )
            }

            return prizes as ArrayList<PerformerPrize>
        }
    }

    private val requestQueue: RequestQueue by lazy {
        // applicationContext keeps you from leaking the Activity or BroadcastReceiver if someone passes one in.
        queue ?: Volley.newRequestQueue(context.applicationContext)
    }

    // ---------------------------------------------------------------------------------------------
    // API con corrutinas
    //
    // Cada función:
    //  1. Suspende (no bloquea) mientras Volley hace la petición en su propio hilo de red.
    //  2. Cuando llega la respuesta, parsea el JSON dentro de withContext(dispatcher), es decir,
    //     FUERA del hilo principal. Antes ese parseo ocurría en el listener de Volley, que corre
    //     en el hilo principal, y era el punto de riesgo de ANR.
    //  3. Devuelve el modelo ya listo, o lanza la VolleyError si la petición falló.
    // ---------------------------------------------------------------------------------------------

    suspend fun getAlbums(): List<Album> = withContext(dispatcher) {
        parseAlbums(getString("albums"))
    }

    suspend fun getAlbumDetail(id: Int): AlbumDetail = withContext(dispatcher) {
        parseAlbumDetail(getString("albums/$id"))
    }

    suspend fun getBands(): List<Performer> = withContext(dispatcher) {
        parseBands(getString("bands"))
    }

    suspend fun getMusicians(): List<Performer> = withContext(dispatcher) {
        parseMusicians(getString("musicians"))
    }

    suspend fun getPerformer(id: Int, type: PerformerType): PerformerBase = withContext(dispatcher) {
        parsePerformer(getString(performerPath(id, type)))
    }

    /** La respuesta del POST no se parsea a un modelo, así que no hace falta cambiar de hilo. */
    suspend fun postComment(albumId: Int, comment: String, rating: Int): JSONObject =
        postJson("albums/$albumId/comments", commentBody(comment, rating))

    /**
     * Convierte una petición GET de Volley (basada en callbacks) en una función suspend.
     *
     * - suspendCancellableCoroutine pausa la corrutina sin bloquear el hilo.
     * - El listener de éxito la reanuda con el String y el de error con una excepción.
     * - Si la corrutina se cancela (por ejemplo, se destruye el ViewModel), también se cancela
     *   la petición en Volley para no procesar una respuesta que ya nadie necesita.
     */
    private suspend fun getString(path: String): String = suspendCancellableCoroutine { cont ->
        val request = getRequest(
            path,
            { response -> cont.resume(response) },
            { error -> cont.resumeWithException(error) }
        )
        requestQueue.add(request)
        cont.invokeOnCancellation { request.cancel() }
    }

    /** Igual que [getString], pero para un POST con cuerpo JSON. */
    private suspend fun postJson(path: String, body: JSONObject): JSONObject =
        suspendCancellableCoroutine { cont ->
            val request = postRequest(
                path,
                body,
                { response -> cont.resume(response) },
                { error -> cont.resumeWithException(error) }
            )
            requestQueue.add(request)
            cont.invokeOnCancellation { request.cancel() }
        }

    private fun performerPath(id: Int, type: PerformerType) =
        if (type == PerformerType.BAND) "bands/$id" else "musicians/$id"

    private fun commentBody(comment: String, rating: Int): JSONObject {
        val postParams = JSONObject()
        postParams.put("description", comment)
        postParams.put("rating", rating)
        val collectorObj = JSONObject()
        collectorObj.put("id", 100)
        postParams.put("collector", collectorObj)
        return postParams
    }

    private fun getRequest(path:String, responseListener: Response.Listener<String>, errorListener: Response.ErrorListener): StringRequest {
        return StringRequest(Request.Method.GET, BASE_URL+path, responseListener,errorListener)
            .apply { retryPolicy = DefaultRetryPolicy(REQUEST_TIMEOUT_MS, 1, 1f) }
    }

    private fun postRequest(
        path: String,
        body: JSONObject,
        responseListener: Response.Listener<JSONObject>,
        errorListener: Response.ErrorListener
    ): JsonObjectRequest {
        return JsonObjectRequest(Request.Method.POST, BASE_URL + path, body, responseListener, errorListener)
            .apply { retryPolicy = DefaultRetryPolicy(REQUEST_TIMEOUT_MS, 1, 1f) }
    }
}