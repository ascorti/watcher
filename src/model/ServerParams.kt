package cz.jtalas.model

import kotlinx.serialization.Serializable

/**
 * @author josef.talas@seznam.cz
 * @since 03.08.2019
 */
@Serializable
data class ServerParams(val publicAddress: String)