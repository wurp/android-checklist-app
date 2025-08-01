package com.checklist.app.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.checklist.app.data.preferences.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : PurchasesUpdatedListener {
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()
    
    private val _hasPurchased = MutableStateFlow(false)
    val hasPurchased: StateFlow<Boolean> = _hasPurchased.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val PRODUCT_ID = "throw_dev_a_bone"
    
    init {
        connectToBillingService()
        // Load saved purchase state
        coroutineScope.launch {
            preferencesManager.hasPurchased.collect { hasPurchased ->
                _hasPurchased.value = hasPurchased
            }
        }
    }
    
    private fun connectToBillingService() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isConnected.value = true
                    queryPurchases()
                }
            }
            
            override fun onBillingServiceDisconnected() {
                _isConnected.value = false
                // Try to reconnect
                connectToBillingService()
            }
        })
    }
    
    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
            
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            }
        }
    }
    
    private fun handlePurchases(purchases: List<Purchase>) {
        val hasBonePurchase = purchases.any { purchase ->
            purchase.products.contains(PRODUCT_ID) && 
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        _hasPurchased.value = hasBonePurchase
        
        // Save purchase state
        coroutineScope.launch {
            preferencesManager.setHasPurchased(hasBonePurchase)
        }
        
        // Acknowledge purchases
        purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach { purchase ->
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(params) { }
            }
    }
    
    suspend fun launchBillingFlow(activity: Activity) {
        if (!_isConnected.value) return
        
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
            
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && 
                productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
                
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()
                    
                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
        }
    }
    
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        }
    }
}