package minsapp.core

/** A WhatsApp feature Minsapp keeps out of reach. */
enum class Ban(val enabledByDefault: Boolean) {
    STATUS(true),
    CHANNELS(true),
    META_AI(true),
    COMMUNITIES(false),
}
