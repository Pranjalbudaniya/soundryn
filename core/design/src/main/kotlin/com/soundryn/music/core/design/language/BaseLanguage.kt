package com.soundryn.music.core.design.language

object BaseLanguage {

    fun load(): ParseResult {
        val stream = javaClass.classLoader?.getResourceAsStream("design/m3_expressive.json")
            ?: javaClass.getResourceAsStream("/design/m3_expressive.json")
            ?: return ParseResult.Failure(listOf(DesignIssue("", "base language resource missing")))
        val text = stream.bufferedReader().use { it.readText() }
        return DesignParser.parse(text)
    }
}
