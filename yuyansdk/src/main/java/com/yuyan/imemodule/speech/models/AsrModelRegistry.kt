package com.yuyan.imemodule.speech.models

object AsrModelRegistry {

    val profiles: List<AsrModelProfile> = listOf(
        ZipformerZhInt8Profile,
        XAsr480msZhEnPunctInt8Profile,
    )

    val default: AsrModelProfile = profiles.first()

    fun findById(id: String): AsrModelProfile? = profiles.find { it.id == id }

    fun profileOrDefault(id: String): AsrModelProfile =
        findById(id) ?: default.copy(id = id)
}
