# "Throw Dev a Bone" Billing Implementation

## Overview
The app includes an optional in-app purchase feature called "Throw Dev a Bone" that allows users to support the developer with a one-time $0.99 contribution.

## UI Location
The feature is accessed via a heart icon in the top app bar:
- **Unpurchased state**: Unfilled heart icon (FavoriteBorder)
- **Purchased state**: Filled heart icon (Favorite) with "Thank you!" tooltip

## Technical Implementation

### Components
1. **BillingRepository**: Handles Google Play Billing client connection and purchase flow
2. **ThrowDevABoneUseCase**: Business logic for initiating purchases
3. **PreferencesManager**: Persists purchase state using DataStore
4. **MainScreen/MainViewModel**: UI integration in the app bar

### Product Configuration
- **Product ID**: `throw_dev_a_bone`
- **Type**: In-app purchase (non-consumable)
- **Price**: $0.99 USD

## Important: Sideloading Limitations

When installing the app via APK (sideloading), the billing feature will not work because:

1. **Google Play Services Required**: The billing client requires the app to be installed through Google Play Store
2. **Service Connection Fails**: `BillingClient.startConnection()` will fail when sideloaded
3. **No Visible Error**: The app handles this gracefully - clicking the heart simply does nothing

### Testing Billing

To properly test billing functionality:

1. **Upload to Google Play Console** (Internal Testing track)
2. **Configure the product** in Play Console with ID `throw_dev_a_bone`
3. **Add test accounts** to the testing program
4. **Install via Play Store** testing link
5. **Use test payment methods** provided by Google

### Current Behavior When Sideloaded

```
User taps heart icon → 
BillingRepository.launchBillingFlow() called →
isConnected check fails (false) →
Function returns early →
No UI change or error message
```

## UI Flow (When Properly Installed via Play Store)

```
┌─────────────────────────────────────┐
│  Templates                 ♡        │  <- Unfilled heart
├─────────────────────────────────────┤
│                                     │
│         [Template List]             │
│                                     │
└─────────────────────────────────────┘
              ↓ User taps heart
┌─────────────────────────────────────┐
│        Google Play Dialog           │
│                                     │
│    Throw Dev a Bone                │
│    Support the developer            │
│    $0.99                           │
│                                     │
│  [CANCEL]           [BUY]          │
└─────────────────────────────────────┘
              ↓ Purchase complete
┌─────────────────────────────────────┐
│  Templates                 ♥        │  <- Filled heart
├─────────────────────────────────────┤
│                                     │
│         [Template List]             │
│                                     │
└─────────────────────────────────────┘
```

## Error Handling

The implementation includes robust error handling:
- Automatic reconnection on billing service disconnect
- Purchase acknowledgment to prevent refunds
- Persistent storage of purchase state
- Query purchases on app start to restore state

## Security Considerations

- Purchase verification happens server-side (Google Play)
- Purchase tokens are acknowledged to complete transactions
- Local storage only stores a boolean flag, not sensitive data

## Future Improvements

1. Add visual feedback when billing service is unavailable
2. Implement a "Restore Purchases" option
3. Add analytics to track conversion rates
4. Consider adding a thank you dialog after purchase