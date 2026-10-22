// bxane
package com.vyonex.internal.primary.models.body

import com.vyonex.internal.primary.models.Context
import kotlinx.serialization.Serializable

@Serializable
internal data class AccountsListBody(
    val context: Context,
    val requestType: String = "ACCOUNTS_LIST_REQUEST_TYPE_CHANNEL_SWITCHER",
    val callCircumstance: String = "SWITCHING_USERS_FULL",
)
