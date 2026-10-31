package com.example.vinyls_application.models

data class AlbumDetail (
    val albumId:Int,
    val name:String,
    val cover:String,
    val releaseDate:String,
    val description:String,
    val genre:String,
    val recordLabel:String,
    val tracks: List<Track>,
    val performers: List<Performer>,
    val comments: List<Comment>
)