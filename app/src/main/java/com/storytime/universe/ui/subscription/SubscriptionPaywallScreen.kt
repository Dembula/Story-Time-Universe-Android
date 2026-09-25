package com.storytime.universe.ui.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.R
import com.storytime.universe.data.AppConfig
import com.storytime.universe.data.billing.BillingService
import com.storytime.universe.data.billing.PaywallContext
import com.storytime.universe.data.billing.StoreProducts
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.findActivity
import com.storytime.universe.ui.AppState
import com.storytime.universe.ui.theme.StColors
import com.storytime.universe.ui.util.openUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class PackageModel { SUBSCRIPTION, PPV }

/**
 * Native package / plan / PPV unlock paywall (iOS `SubscriptionPaywallView` + web package model step).
 */
@Composable
fun SubscriptionPaywallScreen(
    appState: AppState,
    context: PaywallContext,
    onDismiss: () -> Unit,
) {
    val androidContext = LocalContext.current
    val activity = remember { androidContext.findActivity() }
    val scope = rememberCoroutineScope()

    val isPpvUnlock = context is PaywallContext.Ppv
    var packageModel by remember {
        mutableStateOf(if (appState.isPayPerViewAccount) PackageModel.PPV else PackageModel.SUBSCRIPTION)
    }
    var showYearly by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf(StoreProducts.STANDARD_MONTHLY) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val playProducts by BillingService.productDetails.collectAsState()
    val ppvProduct by BillingService.ppvProductDetails.collectAsState()
    val purchaseInFlight by BillingService.purchaseInFlight.collectAsState()
    val hasPlaySubs = playProducts.isNotEmpty()
    val hasPlayPpv = ppvProduct != null

    val offers = StoreProducts.offers(showYearly)

    LaunchedEffect(Unit) {
        runCatching { BillingService.refreshProducts() }
        selectedId = if (showYearly) StoreProducts.STANDARD_YEARLY else StoreProducts.STANDARD_MONTHLY
    }
    LaunchedEffect(showYearly) {
        selectedId = if (showYearly) StoreProducts.STANDARD_YEARLY else StoreProducts.STANDARD_MONTHLY
    }

    fun priceLabel(offer: StoreProducts.PlanOffer): String {
        val playPrice = BillingService.formattedPrice(offer.productId)
        return if (playPrice != null) {
            playPrice + if (offer.isYearly) " / year" else " / month"
        } else {
            StoreProducts.formatZar(offer.listPriceZar) + if (offer.isYearly) " / year" else " / month"
        }
    }

    fun ppvPriceLabel(): String =
        BillingService.formattedPrice(StoreProducts.PPV_UNLOCK)
            ?: StoreProducts.formatZar(StoreProducts.PPV_PRICE_ZAR)

    fun finishSuccess(message: String) {
        scope.launch {
            successMessage = message
            appState.refreshSubscription()
            delay(800)
            onDismiss()
        }
    }

    fun selectPpvModel() {
        errorMessage = null
        scope.launch {
            busy = true
            try {
                ViewerApi.selectViewerPackage(viewerModel = "PPV", plan = "PPV_FILM", startTrial = false)
                appState.refreshSubscription()
                finishSuccess("Pay Per View is ready. Unlock titles for ${StoreProducts.formatZar(StoreProducts.PPV_PRICE_ZAR)} each.")
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Could not switch to Pay Per View."
            } finally {
                busy = false
            }
        }
    }

    fun subscribePlan() {
        errorMessage = null
        val offer = offers.firstOrNull { it.productId == selectedId } ?: return
        scope.launch {
            busy = true
            try {
                if (hasPlaySubs && activity != null) {
                    val purchase = BillingService.launchPurchase(activity, offer.productId).getOrElse { throw it }
                    runCatching { BillingService.acknowledgeAndActivate(purchase, offer.productId) }
                        .onFailure { errorMessage = it.localizedMessage }
                    finishSuccess("You're on ${offer.displayName}. Welcome to Story Time.")
                } else {
                    // Ensure package is subscription-shaped, then web checkout.
                    runCatching {
                        ViewerApi.selectViewerPackage(
                            viewerModel = "SUBSCRIPTION",
                            plan = offer.planCode,
                            startTrial = false,
                        )
                    }
                    openUrl(androidContext, AppConfig.PACKAGE_ONBOARDING_URL)
                }
            } catch (e: Exception) {
                val msg = e.localizedMessage.orEmpty()
                if (!msg.contains("cancelled", ignoreCase = true)) {
                    errorMessage = msg.ifBlank { "Could not start checkout." }
                }
            } finally {
                busy = false
            }
        }
    }

    fun unlockTitle() {
        val ppv = context as? PaywallContext.Ppv ?: return
        errorMessage = null
        scope.launch {
            busy = true
            try {
                if (hasPlayPpv && activity != null) {
                    val purchase = BillingService.launchPurchase(activity, StoreProducts.PPV_UNLOCK).getOrElse { throw it }
                    runCatching { BillingService.acknowledgeAndActivatePpv(purchase, ppv.contentId) }
                        .onFailure { activateErr ->
                            errorMessage = activateErr.localizedMessage
                            val web = runCatching { ViewerApi.requestPpvAccess(ppv.contentId) }.getOrNull()
                            web?.checkoutUrl?.let { openUrl(androidContext, it) }
                            return@launch
                        }
                    appState.markPpvUnlockSucceeded()
                    finishSuccess("Unlocked for ${StoreProducts.PPV_ACCESS_DAYS} days.")
                } else {
                    val result = ViewerApi.requestPpvAccess(ppv.contentId)
                    when {
                        result.alreadyOwned == true -> {
                            appState.markPpvUnlockSucceeded()
                            finishSuccess("Already unlocked — enjoy watching.")
                        }
                        !result.checkoutUrl.isNullOrEmpty() -> openUrl(androidContext, result.checkoutUrl)
                        else -> errorMessage = result.error ?: "Could not start PPV checkout."
                    }
                }
            } catch (e: Exception) {
                val msg = e.localizedMessage.orEmpty()
                if (!msg.contains("cancelled", ignoreCase = true)) {
                    errorMessage = msg.ifBlank { "Could not unlock this title." }
                }
            } finally {
                busy = false
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(StColors.Background)
            .background(
                Brush.linearGradient(
                    listOf(StColors.Accent.copy(alpha = 0.22f), Color.Transparent, StColors.AccentGold.copy(alpha = 0.1f))
                )
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(navTitle(context), color = StColors.Foreground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, enabled = !busy && !purchaseInFlight) {
                    Icon(Icons.Filled.Close, "Close", tint = StColors.Accent)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Image(painterResource(R.drawable.app_logo), null, modifier = Modifier.size(88.dp))
                Spacer(Modifier.height(12.dp))
                Text(headline(context), color = StColors.Foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(subheadline(context, hasPlaySubs, hasPlayPpv), color = StColors.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
            }

            if (isPpvUnlock) {
                PpvUnlockCard(priceLabel = ppvPriceLabel(), title = (context as PaywallContext.Ppv).title)
                Button(
                    onClick = { unlockTitle() },
                    enabled = !busy && !purchaseInFlight,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StColors.Accent, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    if (busy || purchaseInFlight) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Pay · ${ppvPriceLabel()} · Unlock", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Model picker: Subscription vs PPV
                Text("Account type", color = StColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                ModelCard(
                    title = "Subscription",
                    subtitle = "Unlimited catalogue · Basic / Standard / Premium",
                    icon = Icons.Filled.Subscriptions,
                    selected = packageModel == PackageModel.SUBSCRIPTION,
                    onSelect = { packageModel = PackageModel.SUBSCRIPTION },
                )
                ModelCard(
                    title = "Pay Per View",
                    subtitle = "${StoreProducts.formatZar(StoreProducts.PPV_PRICE_ZAR)} per title · ${StoreProducts.PPV_ACCESS_DAYS}-day access · 1 profile",
                    icon = Icons.Filled.Movie,
                    selected = packageModel == PackageModel.PPV,
                    onSelect = { packageModel = PackageModel.PPV },
                )

                if (packageModel == PackageModel.SUBSCRIPTION) {
                    Row(
                        Modifier.fillMaxWidth().clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).padding(3.dp),
                    ) {
                        BillingTab("Monthly", !showYearly, Modifier.weight(1f)) { showYearly = false }
                        BillingTab("Yearly — Save ~17%", showYearly, Modifier.weight(1f)) { showYearly = true }
                    }
                    offers.forEach { offer ->
                        PlanCard(
                            offer = offer,
                            priceLabel = priceLabel(offer),
                            selected = selectedId == offer.productId,
                            onSelect = { selectedId = offer.productId },
                        )
                    }
                    Button(
                        onClick = { subscribePlan() },
                        enabled = !busy && !purchaseInFlight,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StColors.Accent, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    ) {
                        if (busy || purchaseInFlight) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            val offer = offers.firstOrNull { it.productId == selectedId }
                            Text(
                                if (hasPlaySubs) "Subscribe · ${offer?.displayName ?: "Plan"}"
                                else "Continue to checkout",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                } else {
                    Text(
                        "No monthly fee. Browse freely, then tap Pay on any title to unlock it for ${StoreProducts.PPV_ACCESS_DAYS} days.",
                        color = StColors.Muted,
                        fontSize = 13.sp,
                    )
                    Button(
                        onClick = { selectPpvModel() },
                        enabled = !busy,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StColors.Accent, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Continue with Pay Per View", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            errorMessage?.let {
                Text(it, color = Color(0xFFFF5A5A), fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            successMessage?.let {
                Text(it, color = StColors.Accent, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            if (!isPpvUnlock) {
                TextButton(onClick = { openUrl(androidContext, AppConfig.CHANGE_PLAN_URL) }) {
                    Text("Manage / change plan on website", color = StColors.AccentGold, fontSize = 13.sp)
                }
            }

            Text(
                if (isPpvUnlock) {
                    "One-time unlock. Access lasts ${StoreProducts.PPV_ACCESS_DAYS} days from payment. Payment is handled by Google Play when the product is live, otherwise on story-time.online."
                } else {
                    "Subscriptions auto-renew unless cancelled. PPV accounts unlock titles individually for ${StoreProducts.formatZar(StoreProducts.PPV_PRICE_ZAR)} each."
                },
                color = StColors.Muted.copy(alpha = 0.8f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun ModelCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) StColors.Accent else StColors.Border), RoundedCornerShape(16.dp))
            .background(if (selected) StColors.Accent.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.04f))
            .clickable { onSelect() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, tint = if (selected) StColors.Accent else StColors.Muted, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = StColors.Foreground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, color = StColors.Muted, fontSize = 12.sp)
        }
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(if (selected) StColors.Accent else Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun PpvUnlockCard(priceLabel: String, title: String?) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title ?: "This title", color = StColors.Foreground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            "Pay $priceLabel once to unlock streaming for ${StoreProducts.PPV_ACCESS_DAYS} days on this Pay Per View account.",
            color = StColors.Muted,
            fontSize = 14.sp,
        )
        listOf(
            "Full HD playback & downloads for this title",
            "Access expires after ${StoreProducts.PPV_ACCESS_DAYS} days",
            "Buy again anytime to re-unlock",
        ).forEach { feature ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Check, null, tint = StColors.Accent, modifier = Modifier.size(16.dp))
                Text(feature, color = StColors.Muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun BillingTab(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(CircleShape).background(if (active) StColors.Accent else Color.Transparent).clickable { onClick() }.padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (active) Color.Black else StColors.Muted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun PlanCard(
    offer: StoreProducts.PlanOffer,
    priceLabel: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val border = if (selected) StColors.Accent else if (offer.highlight) StColors.AccentGold.copy(alpha = 0.45f) else StColors.Border
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(BorderStroke(if (selected) 2.dp else 1.dp, border), RoundedCornerShape(16.dp))
            .background(if (selected) StColors.Accent.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.04f))
            .clickable { onSelect() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(offer.displayName, color = StColors.Foreground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (offer.highlight) {
                        Text(
                            "Popular",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(CircleShape).background(StColors.AccentGold).padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                Text(priceLabel, color = StColors.AccentGold, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                Modifier.size(26.dp).clip(CircleShape).background(if (selected) StColors.Accent else Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
        offer.features.forEach { feature ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Check, null, tint = StColors.Accent, modifier = Modifier.size(16.dp))
                Text(feature, color = StColors.Muted, fontSize = 13.sp)
            }
        }
    }
}

private fun navTitle(context: PaywallContext) = when (context) {
    PaywallContext.Subscribe -> "Choose a Plan"
    PaywallContext.Reactivate -> "Reactivate"
    PaywallContext.ChangePlan -> "Change Plan"
    is PaywallContext.Ppv -> "Unlock Title"
}

private fun headline(context: PaywallContext) = when (context) {
    PaywallContext.Subscribe -> "Subscribe or go Pay Per View"
    PaywallContext.Reactivate -> "Reactivate your access"
    PaywallContext.ChangePlan -> "Change your plan"
    is PaywallContext.Ppv -> context.title?.let { "Unlock $it" } ?: "Unlock this title"
}

private fun subheadline(context: PaywallContext, hasPlaySubs: Boolean, hasPlayPpv: Boolean): String = when (context) {
    is PaywallContext.Ppv ->
        if (hasPlayPpv) "One-time payment via Google Play. Access lasts ${StoreProducts.PPV_ACCESS_DAYS} days."
        else "Pay ${StoreProducts.formatZar(StoreProducts.PPV_PRICE_ZAR)} to unlock for ${StoreProducts.PPV_ACCESS_DAYS} days (checkout on story-time.online until Play product is live)."
    PaywallContext.ChangePlan ->
        "Switch between Subscription plans or Pay Per View."
    else ->
        if (hasPlaySubs) "Payment is handled securely by Google Play for subscriptions."
        else "Pick Subscription (Basic / Standard / Premium) or Pay Per View (${StoreProducts.formatZar(StoreProducts.PPV_PRICE_ZAR)} per title)."
}
