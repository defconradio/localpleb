// TODO: Dependency Injection in Repositories: The ViewModels are already using Hilt for dependency injection. As the app grows, you could also have Hilt provide the repositories themselves, rather than creating them manually inside the Android...ViewModel files (e.g., ProductRepository(repository)). This is a minor point but can make testing and dependency management even cleaner in larger projects.
package com.example.data.repository

import com.example.data.uiModels.ProductShippingZoneUiModel
import com.example.data.uiModels.ProductUiModel
import com.example.nostr.NostrRepository
import com.example.nostr.models.EventMessage
import com.example.nostr.models.EoseMessage
import com.example.nostr.models.Filter
import com.example.nostr.models.NostrEnvelope
import com.example.nostr.models.NoticeMessage
import com.example.nostr.models.OkMessage
import com.example.nostr.models.AuthMessage
import com.example.nostr.models.ClosedMessage
import com.example.nostr.models.ProductContent
import kotlinx.coroutines.Dispatchers
//import com.example.pleb2.uiModels.NostrEnvelopeUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.iterator

class ProductRepository(
    private val nostrRepository: NostrRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    data class ProductData(
        val products: List<ProductUiModel>,
        val tags: Set<String>,
        val eoseHit: Boolean = false
    )

    private data class InternalProductData(
        val products: List<NostrEnvelope>,
        val tags: Set<String>,
        val eoseHit: Boolean = false
    )

    data class ProductUiModelWithEose(
        val product: ProductUiModel?,
        val eoseHit: Boolean
    )

    fun getProducts(relayUrls: List<String>): Flow<ProductData> {
        val productFilters = listOf(Filter(kinds = listOf(30018)))
        return subscribeToProductsInternal(relayUrls, productFilters).map { internalData ->
            ProductData(
                products = internalData.products.mapNotNull { mapToProductUiModel(it) },
                tags = internalData.tags,
                eoseHit = internalData.eoseHit
            )
        }.flowOn(Dispatchers.IO)
    }

    private fun subscribeToProductsInternal(relayUrls: List<String>, customProductFilters: List<Filter>): Flow<InternalProductData> {
        return flow {
            // 1. Emit an initial empty state immediately. This ensures the UI has something to display
            // and prevents it from getting stuck if no events are received.
            // emit(ProductData(products = emptyList(), tags = emptySet()))

            val products = mutableMapOf<String, NostrEnvelope>()
            val eoseReceivedFrom = mutableSetOf<String>()
            val deletionFilter = Filter(kinds = listOf(5))
            val allFilters = customProductFilters + deletionFilter

            // This is a persistent subscription
            nostrRepository.subscribeToEvents(relayUrls, allFilters)
                .collect { message ->
                    when (message) {
                        is EoseMessage -> {
                            eoseReceivedFrom.add(message.relayUrl)
                            // Optional: Check if all relays have sent EOSE.
                            // If so, and if products are empty, you can be certain there are no results.
                            // The initial emit already handles the UI state for this.
                            println("[ProductRepository] EOSE received from ${message.relayUrl}. Total EOSEs: ${eoseReceivedFrom.size}/${relayUrls.size}")
                            if (eoseReceivedFrom.size >= relayUrls.size) {
                                val currentProducts = products.values.toList().sortedByDescending { it.created_at }
                                val currentTags = currentProducts
                                    .flatMap { p -> p.tags }
                                    .filter { it.size > 1 && it[0] == "t" }
                                    .map { it[1].lowercase() }
                                    .toSet()
                                emit(InternalProductData(products = currentProducts, tags = currentTags, eoseHit = true))
                            }
                        }
                        is EventMessage -> {
                            val event = message.event
                            var listChanged = false
                            if (event.kind == 5) {
                                // Deletion Event
                                val eventIdToDelete = event.tags.find { it.size > 1 && it[0] == "e" }?.getOrNull(1)
                                if (eventIdToDelete != null) {
                                    var productIdToRemove: String? = null
                                    for ((productId, productEvent) in products) {
                                        if (productEvent.id == eventIdToDelete) {
                                            productIdToRemove = productId
                                            break
                                        }
                                    }
                                    if (productIdToRemove != null) {
                                        products.remove(productIdToRemove)
                                        listChanged = true
                                    }
                                }
                            } else {
                                // Regular Product Event
                                try {
                                    val productContent = json.decodeFromString<ProductContent>(event.content)
                                    val productId = productContent.id ?: event.id // Use content ID or event ID
                                    val existingProduct = products[productId]

                                    if (existingProduct == null || event.created_at > existingProduct.created_at) {
                                        products[productId] = event
                                        listChanged = true
                                    }
                                } catch (e: Exception) {
                                    println("[ProductRepository] Failed to parse product content, skipping event ${event.id}. Error: ${e.message}")
                                    // By catching the exception, we just continue to the next event in the flow.
                                }
                            }

                            if (listChanged) {
                                val currentProducts = products.values.toList().sortedByDescending { it.created_at }
                                val currentTags = currentProducts
                                    .flatMap { p -> p.tags }
                                    .filter { it.size > 1 && it[0] == "t" }
                                    .map { it[1].lowercase() }
                                    .toSet()
                                emit(InternalProductData(products = currentProducts, tags = currentTags))
                            }
                        }
                        is NoticeMessage -> {
                            // Handle notice
                            println("[ProductRepository] Notice from ${message.relayUrl}: ${message.message}")
                        }
                        is OkMessage -> {
                            // Handle OK
                            //println("[ProductRepository] OK from ${message.relayUrl}: ${message.eventId} - ${message.status}. ${message.message}")
                            println("[ProductRepository] Received ok from ${message.relayUrl}: ${message.message}")
                        }
                        is AuthMessage -> {
                            println("[ProductRepository] AUTH from ${message.relayUrl}: ${message.challenge}")
                        }
                        is ClosedMessage -> {
                            println("[ProductRepository] CLOSED from ${message.relayUrl}: ${message.subscriptionId}. ${message.message}")
                        }
                        else -> {
                            // This branch should not be reached if all message types are handled
                            println("[ProductRepository] Received an unknown message type: $message")
                        }
                    }
                }
        }.debounce(500) // Batch emissions to every 500ms
    }

    fun getProduct(id: String, relayUrls: List<String>): Flow<ProductUiModel?> {
        val filters = listOf(Filter(kinds = listOf(30018), ids = listOf(id)))
        return subscribeToProductsInternal(relayUrls, filters).map { productData ->
            productData.products.firstOrNull()?.let { mapToProductUiModel(it) }
        }.flowOn(Dispatchers.IO)
    }

    fun getProductByEventIdWithEose(relayUrls: List<String>, eventId: String): Flow<ProductUiModelWithEose> {
        val filters = listOf(Filter(kinds = listOf(30018), ids = listOf(eventId)))
        return subscribeToProductsInternal(relayUrls, filters).map { productData ->
            ProductUiModelWithEose(
                product = productData.products.firstOrNull()?.let { mapToProductUiModel(it) },
                eoseHit = productData.eoseHit
            )
        }.flowOn(Dispatchers.IO)
    }

    /*
    fun getProductByEventId(relayUrls: List<String>, eventId: String): Flow<ProductData> {
        val filters = listOf(Filter(kinds = listOf(30018), ids = listOf(eventId)))
        return subscribeToProductsInternal(relayUrls, filters)
    }
    */

    fun getProductsByAuthor(relayUrls: List<String>, authors: List<String>): Flow<List<ProductUiModel>> {
        val filters = listOf(Filter(kinds = listOf(30018), authors = authors))
        return subscribeToProductsInternal(relayUrls, filters).map { productData ->
            productData.products.mapNotNull { event ->
                mapToProductUiModel(event)
            }
        }.flowOn(Dispatchers.IO)
    }

    /*
    fun getProductsByAuthor(relayUrls: List<String>, authors: List<String>): Flow<ProductData> {
        val filters = listOf(Filter(kinds = listOf(30018), authors = authors))
        return subscribeToProductsInternal(relayUrls, filters)
    }
    */

    fun mapToProductUiModel(event: NostrEnvelope): ProductUiModel? {
        // --- JSON validation logic ---
        val isValidJson = try {
            json.parseToJsonElement(event.content)
            true
        } catch (_: Exception) {
            println("ProductRepository.kt - ProductContent -  Invalid JSON: ${event.id}")
            false
        }
        if (!isValidJson) return null
        // --- ProductContent parsing logic ---
        val productContent = try {
            json.decodeFromString(ProductContent.serializer(), event.content)
        } catch (_: Exception) {
            // Fallback: try to handle 'title' as 'name' and other schema changes
            val fallbackElement = json.parseToJsonElement(event.content)
            val fallbackJson = fallbackElement as? JsonObject
            if (fallbackJson == null) {
                println("ProductRepository.kt - ProductContent - Fallback: Not a JsonObject - ${event.id}")
                return null
            }

            // --- Stricter Validation: Ensure required fields exist ---
            val productId = fallbackJson["id"]?.let { it as? JsonPrimitive }?.contentOrNull
            if (productId == null) {
                println("ProductRepository.kt - ProductContent - Fallback: Missing 'id' field in content - ${event.id}")
                return null
            }

            val name = (fallbackJson["name"]?.let { it as? JsonPrimitive }?.contentOrNull)
                ?: (fallbackJson["title"]?.let { it as? JsonPrimitive }?.contentOrNull)

            val price = fallbackJson["price"]?.let { it as? JsonPrimitive }?.contentOrNull?.toFloatOrNull() ?: 0f
            val quantity = fallbackJson["quantity"]?.let { it as? JsonPrimitive }?.contentOrNull?.toIntOrNull()
            val description = fallbackJson["description"]?.let { it as? JsonPrimitive }?.contentOrNull
            val images = fallbackJson["images"]?.let { it as? JsonArray }?.mapNotNull { img ->
                (img as? JsonPrimitive)?.contentOrNull
            }
            val currency = fallbackJson["currency"]?.let { it as? JsonPrimitive }?.contentOrNull ?: ""
            val specs = fallbackJson["specs"]?.let { it as? JsonArray }?.mapNotNull { specArr ->
                (specArr as? JsonArray)?.mapNotNull { it2 ->
                    (it2 as? JsonPrimitive)?.contentOrNull
                }
            } ?: emptyList()
            ProductContent(
                id = productId,
                stall_id = fallbackJson["stall_id"]?.let { it as? JsonPrimitive }?.contentOrNull ?: "",
                name = name ?: return null,
                description = description,
                images = images,
                currency = currency,
                price = price,
                quantity = quantity,
                specs = specs,
                shipping = emptyList() // fallback does not parse shipping
            )
        }
        // --- UI Model mapping logic ---
        val formattedCreatedAt = try {
            val date = Date(event.created_at * 1000)
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
        } catch (_: Exception) {
            null
        }
        return ProductUiModel(
            envelope = event,
            event_id = event.id,
            pubkey = event.pubkey,
            created_at = event.created_at,
            kind = event.kind,
            tags = event.tags,
            content = event.content,
            sig = event.sig ?: "",
            product_id = productContent.id,
            stall_id = productContent.stall_id,
            name = productContent.name,
            description = productContent.description,
            images = productContent.images,
            currency = productContent.currency,
            price = productContent.price,
            quantity = productContent.quantity,
            specs = productContent.specs,
            shipping = productContent.shipping.map { pz ->
                ProductShippingZoneUiModel(
                    id = pz.id,
                    cost = pz.cost
                )
            },
            formattedCreatedAt = formattedCreatedAt,
            relayUrl = event.relayUrl
        )
    }
}