package com.soundryn.music.core.domain

data class TrackKey(
    val sourceId: SourceId,
    val itemId: String,
) {
    fun encode(): String {
        val escapedItem = itemId
            .replace("%", "%25")
            .replace("/", "%2F")
        return "${sourceId.value}/$escapedItem"
    }

    companion object {
        private val SOURCE_ID_REGEX = Regex("^[a-z0-9._-]+$")

        fun of(sourceId: String, itemId: String): Outcome<TrackKey> {
            if (!SOURCE_ID_REGEX.matches(sourceId)) {
                return Outcome.Failure(DomainError.Parse("Invalid sourceId: '$sourceId'"))
            }
            if (itemId.isEmpty()) {
                return Outcome.Failure(DomainError.Parse("itemId must not be empty"))
            }
            return Outcome.Success(TrackKey(SourceId(sourceId), itemId))
        }

        fun parse(text: String): Outcome<TrackKey> {
            val slashIndex = text.indexOf('/')
            if (slashIndex == -1) {
                return Outcome.Failure(DomainError.Parse("Missing separator '/'"))
            }
            val sourcePart = text.substring(0, slashIndex)
            val itemPart = text.substring(slashIndex + 1)
            if (!SOURCE_ID_REGEX.matches(sourcePart)) {
                return Outcome.Failure(DomainError.Parse("Invalid sourceId in key: '$sourcePart'"))
            }
            if (itemPart.isEmpty()) {
                return Outcome.Failure(DomainError.Parse("Empty item ID in key"))
            }

            val sb = StringBuilder(itemPart.length)
            var i = 0
            while (i < itemPart.length) {
                val c = itemPart[i]
                if (c == '%') {
                    if (i + 2 >= itemPart.length) {
                        return Outcome.Failure(DomainError.Parse("Incomplete escape sequence at index $i"))
                    }
                    val hex = itemPart.substring(i + 1, i + 3)
                    when (hex.uppercase()) {
                        "25" -> {
                            sb.append('%')
                            i += 3
                        }
                        "2F" -> {
                            sb.append('/')
                            i += 3
                        }
                        else -> {
                            return Outcome.Failure(DomainError.Parse("Invalid escape sequence %$hex at index $i"))
                        }
                    }
                } else {
                    sb.append(c)
                    i++
                }
            }
            return Outcome.Success(TrackKey(SourceId(sourcePart), sb.toString()))
        }
    }
}
