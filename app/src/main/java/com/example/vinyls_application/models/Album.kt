package com.example.vinyls_application.models

data class Album (

    /** [NEW_ALBUM_ID] mientras el álbum no se ha creado; el backend asigna el id real. */
    val albumId:Int = NEW_ALBUM_ID,
    val name:String,
    val cover:String,
    val releaseDate:String,
    val description:String,
    val genre:String,
    val recordLabel:String
) {
    companion object {
        const val NEW_ALBUM_ID = 0
    }
}
