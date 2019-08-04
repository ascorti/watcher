package cz.jtalas.model

import kotlinx.serialization.Serializable

/**
 * @author josef.talas@seznam.cz
 * @since 15.06.2019
 */
@Serializable
data class Command(val command: String)