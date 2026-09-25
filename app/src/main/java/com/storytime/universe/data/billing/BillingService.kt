package com.storytime.universe.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.storytime.universe.data.network.ViewerApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google Play Billing orchestration — Android analogue of iOS `StoreService` (StoreKit 2).
 * When products are not yet created in Play Console, [productDetails] stays empty and the
 * paywall falls back to the web package page (PayFast) so signup still works.
 */
object BillingService : PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null
    private var appContext: Context? = null

    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails: StateFlow<List<ProductDetails>> = _productDetails

    private val _ppvProductDetails = MutableStateFlow<ProductDetails?>(null)
    val ppvProductDetails: StateFlow<ProductDetails?> = _ppvProductDetails

    private val _purchaseInFlight = MutableStateFlow(false)
    val purchaseInFlight: StateFlow<Boolean> = _purchaseInFlight

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private var pendingPurchase: CompletableDeferred<Result<Purchase>>? = null

    fun init(context: Context) {
        if (billingClient != null) return
        appContext = context.applicationContext
        billingClient = BillingClient.newBuilder(context.applicationContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
        startConnection()
    }

    private fun startConnection() {
        val client = billingClient ?: return
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Products load on demand via refreshProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Will reconnect on next purchase / refresh attempt.
            }
        })
    }

    suspend fun ensureReady(): Boolean {
        val client = billingClient ?: return false
        if (client.isReady) return true
        return suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (cont.isActive) {
                        cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    if (cont.isActive) cont.resume(false)
                }
            })
        }
    }

    suspend fun refreshProducts() {
        if (!ensureReady()) {
            _productDetails.value = emptyList()
            _ppvProductDetails.value = null
            return
        }
        val client = billingClient ?: return

        // Subscriptions
        val subList = StoreProducts.allSubscriptionIds.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val subParams = QueryProductDetailsParams.newBuilder().setProductList(subList).build()
        val subs = suspendCancellableCoroutine { cont ->
            client.queryProductDetailsAsync(subParams) { billingResult, detailsResult ->
                if (cont.isActive) {
                    cont.resume(
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            detailsResult.productDetailsList
                        } else {
                            emptyList()
                        }
                    )
                }
            }
        }
        _productDetails.value = subs

        // PPV one-time unlock
        val ppvList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(StoreProducts.PPV_UNLOCK)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val ppvParams = QueryProductDetailsParams.newBuilder().setProductList(ppvList).build()
        val ppv = suspendCancellableCoroutine { cont ->
            client.queryProductDetailsAsync(ppvParams) { billingResult, detailsResult ->
                if (cont.isActive) {
                    cont.resume(
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            detailsResult.productDetailsList.firstOrNull()
                        } else {
                            null
                        }
                    )
                }
            }
        }
        _ppvProductDetails.value = ppv
    }

    fun detailsFor(productId: String): ProductDetails? =
        if (productId == StoreProducts.PPV_UNLOCK) _ppvProductDetails.value
        else _productDetails.value.firstOrNull { it.productId == productId }

    fun formattedPrice(productId: String): String? {
        val details = detailsFor(productId) ?: return null
        if (productId == StoreProducts.PPV_UNLOCK) {
            return details.oneTimePurchaseOfferDetails?.formattedPrice
        }
        val offer = details.subscriptionOfferDetails?.firstOrNull() ?: return null
        return offer.pricingPhases.pricingPhaseList.firstOrNull()?.formattedPrice
    }

    /**
     * Launches the Play purchase sheet. Returns the completed purchase, or failure.
     * Callers must then [acknowledgeAndActivate] / [acknowledgeAndActivatePpv].
     */
    suspend fun launchPurchase(activity: Activity, productId: String): Result<Purchase> {
        _lastError.value = null
        if (!ensureReady()) {
            val err = "Google Play Billing is unavailable on this device."
            _lastError.value = err
            return Result.failure(IllegalStateException(err))
        }
        val details = detailsFor(productId) ?: run {
            refreshProducts()
            detailsFor(productId)
        }
        if (details == null) {
            val err = "This product is not available in Google Play yet."
            _lastError.value = err
            return Result.failure(IllegalStateException(err))
        }

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
        if (productId != StoreProducts.PPV_UNLOCK) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken.isNullOrEmpty()) {
                val err = "No offer available for this plan."
                _lastError.value = err
                return Result.failure(IllegalStateException(err))
            }
            productParamsBuilder.setOfferToken(offerToken)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
            .build()

        val deferred = CompletableDeferred<Result<Purchase>>()
        pendingPurchase = deferred
        _purchaseInFlight.value = true

        val launchResult = billingClient!!.launchBillingFlow(activity, flowParams)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseInFlight.value = false
            pendingPurchase = null
            val err = launchResult.debugMessage.ifBlank { "Could not open Google Play checkout." }
            _lastError.value = err
            return Result.failure(IllegalStateException(err))
        }
        return deferred.await()
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        val deferred = pendingPurchase
        pendingPurchase = null
        _purchaseInFlight.value = false
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchase = purchases?.firstOrNull()
                if (purchase != null) {
                    deferred?.complete(Result.success(purchase))
                } else {
                    deferred?.complete(Result.failure(IllegalStateException("No purchase returned.")))
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                deferred?.complete(Result.failure(IllegalStateException("Purchase cancelled.")))
            }
            else -> {
                val err = result.debugMessage.ifBlank { "Purchase failed (${result.responseCode})." }
                _lastError.value = err
                deferred?.complete(Result.failure(IllegalStateException(err)))
            }
        }
    }

    /** Acknowledge with Play + attach to the signed-in viewer (production Google activate API). */
    suspend fun acknowledgeAndActivate(purchase: Purchase, productId: String) {
        val client = billingClient
        if (client != null && client.isReady && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            suspendCancellableCoroutine { cont ->
                client.acknowledgePurchase(params) { _ ->
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        }

        ViewerApi.activateGoogleSubscription(
            productId = productId,
            purchaseToken = purchase.purchaseToken,
            orderId = purchase.orderId,
            packageName = purchase.packageName,
            planCode = StoreProducts.planCode(productId),
        )
    }

    /** Consume PPV unlock + attach title access on the server. */
    suspend fun acknowledgeAndActivatePpv(purchase: Purchase, contentId: String) {
        val client = billingClient
        if (client != null && client.isReady) {
            val params = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            suspendCancellableCoroutine { cont ->
                client.consumeAsync(params) { _, _ ->
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        }

        ViewerApi.activateGooglePpv(
            contentId = contentId,
            productId = StoreProducts.PPV_UNLOCK,
            purchaseToken = purchase.purchaseToken,
            orderId = purchase.orderId,
            packageName = purchase.packageName,
        )
    }

    suspend fun restorePurchases(): List<Purchase> {
        if (!ensureReady()) return emptyList()
        val client = billingClient ?: return emptyList()
        return suspendCancellableCoroutine { cont ->
            client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
            ) { result, purchases ->
                if (cont.isActive) {
                    cont.resume(
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases else emptyList()
                    )
                }
            }
        }
    }

    val hasPlayProducts: Boolean get() = _productDetails.value.isNotEmpty()
    val hasPpvPlayProduct: Boolean get() = _ppvProductDetails.value != null
}
