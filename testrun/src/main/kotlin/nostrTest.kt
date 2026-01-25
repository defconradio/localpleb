import com.example.nostr.models.NostrEnvelope
//import com.example.nostr.models.NostrEvent
import com.example.nostr.NostrDataSource
import com.example.nostr.NostrRepository
import com.example.nostr.models.Filter
import kotlinx.coroutines.runBlocking
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.flow.toList

fun main() = runBlocking {

    //test1 test the nostr data source
    println("Testing Nostr fetch in testrun...")
    val relayUrls = listOf("wss://relay.damus.io")
    val dataSource = NostrDataSource(
        HttpClient(CIO) {
            install(WebSockets)
        }
    )
    val repository = NostrRepository(dataSource)
    val filter = Filter(kinds = listOf(30018))
    try {
        val envelopes = repository.fetchEventsTillEose(relayUrls, listOf(filter)).toList()
        val products = envelopes.filter { it.kind == 30018 }
        println("Fetched product envelopes:")
        products.forEach { println(it) }
    } catch (e: Exception) {
        println("Error: \u001B[31m${e.message}\u001B[0m")
    }
}