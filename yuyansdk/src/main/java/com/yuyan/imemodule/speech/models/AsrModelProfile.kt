package com.yuyan.imemodule.speech.models

data class AsrModelProfile(
    val id: String,
    val name: String,
    val description: String,
    val language: String,
    val size: String,
    val downloadUrl: String,
    val encoderFile: String,
    val decoderFile: String,
    val joinerFile: String,
    val tokensFile: String = "tokens.txt"
)
