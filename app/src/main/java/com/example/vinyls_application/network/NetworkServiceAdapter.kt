package com.example.vinyls_application.network

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import com.example.vinyls_application.models.Album
import com.example.vinyls_application.models.AlbumDetail
import com.example.vinyls_application.models.Collector
import com.example.vinyls_application.models.Comment
import com.example.vinyls_application.models.Performer
import com.example.vinyls_application.models.Track

class NetworkServiceAdapter constructor(context: Context, queue: RequestQueue? = null) {
    companion object{
        // var (no const) para que las pruebas E2E puedan apuntar a un MockWebServer
        @VisibleForTesting
        var BASE_URL = "https://backvynils-q6yc.onrender.com/"
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
    }
    private val requestQueue: RequestQueue by lazy {
        // applicationContext keeps you from leaking the Activity or BroadcastReceiver if someone passes one in.
        queue ?: Volley.newRequestQueue(context.applicationContext)
    }
    fun getAlbums( onComplete:(resp:List<Album>)->Unit , onError: (error:VolleyError)->Unit){
        requestQueue.add(getRequest("albums",
            { response ->
                onComplete(parseAlbums(response))
            },
            {
                onError(it)
            }))
    }

    fun getAlbumDetail( id: Int, onComplete:(resp: AlbumDetail)-> Unit, onError: (error: VolleyError)->Unit){
        requestQueue.add(getRequest("albums/$id",
            { response ->
                onComplete(parseAlbumDetail(response))
            }, {
                onError(it)
            }

        ))
    }

    private fun getRequest(path:String, responseListener: Response.Listener<String>, errorListener: Response.ErrorListener): StringRequest {
        return StringRequest(Request.Method.GET, BASE_URL+path, responseListener,errorListener)
    }
}