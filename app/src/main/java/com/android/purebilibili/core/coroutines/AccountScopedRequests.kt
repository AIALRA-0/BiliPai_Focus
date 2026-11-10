package com.android.purebilibili.core.coroutines

import com.android.purebilibili.core.store.AccountSessionIdentity
import com.android.purebilibili.core.store.TokenManager
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/** Main-thread owner for requests whose cached results belong to the current app account. */
internal class AccountScopedRequests(
    private val parentScope: CoroutineScope,
    private val identities: StateFlow<AccountSessionIdentity> = TokenManager.accountIdentity,
) {
    var identity: AccountSessionIdentity = identities.value
        private set

    private var requests = newScope()

    private fun newScope() = CoroutineScope(
        parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job])
    )

    fun isCurrent(requestedIdentity: AccountSessionIdentity = identity): Boolean =
        requestedIdentity == identities.value

    fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        val requestedIdentity = identity
        return requests.launch(context) {
            if (!isCurrent(requestedIdentity)) return@launch
            block()
        }
    }

    fun observe(onAccountChanged: () -> Unit): Job = parentScope.launch {
        // Base ViewModels can install this observer before subclass state is initialized.
        yield()
        identities.collect { next ->
            if (next == identity) return@collect
            requests.cancel()
            // Old finally/rollback blocks must finish before the new account state is reset.
            requests.coroutineContext[Job]?.join()
            if (next != identities.value) return@collect
            identity = next
            requests = newScope()
            onAccountChanged()
        }
    }
}
