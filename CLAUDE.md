# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EasyShop is a Vietnamese e-commerce Android app built with Jetpack Compose and Firebase. It has two user roles (customer and admin), an AI shopping assistant, real-time chat between customers and admins, and bank transfer payment verification via SePay.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run a single unit test class
./gradlew testDebugUnitTest --tests "com.example.easyshop.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean assembleDebug
```

## Secrets & Configuration

API keys and tokens are stored in `local.properties` (not committed to git) and exposed via `BuildConfig`:
- `GEMINI_API_KEY`, `GEMINI_BASE_URL`, `GEMINI_MODEL` — Google Gemini AI
- `BEEKNOEE_API_KEY`, `BEEKNOEE_BASE_URL`, `BEEKNOEE_MODEL` — Beeknoee OpenAI-compatible fallback
- `SEPAY_TOKEN` — SePay payment verification API
- `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_UPLOAD_PRESET` — Image hosting

The admin secret code is hardcoded in `AppConfig.kt`.

## Architecture

**Single-module app** (`com.example.easyshop`) with no dependency injection framework. Dependencies are manually instantiated (Firebase singletons, companion object patterns).

### Package Structure

- `screen/` — Top-level screen composables that own Scaffold/bottom bars (HomeScreen, LoginScreen, PaymentScreen, etc.)
- `pages/` — Page composables embedded within screens (HomePage, CartPage, ProfilePage, ProductDetailsPage, etc.)
- `viewmodel/` — ViewModels using `StateFlow` for reactive state. No Hilt; ViewModels use default constructor params for Firebase instances
- `model/` — Firestore data classes (ProductModel, UserModel, OrderModel, PromoCodeModel, etc.)
- `repository/` — Data access layer (ProductRepository for Cloudinary uploads, ChatRepository for Firestore chat, AiRepository for AI chat)
- `components/` — Reusable Compose UI components (BannerView, CategoriesView, CartItemView, ProductItemView, etc.)
- `admin/` — Admin-only screens and ViewModels (dashboard, order management, user management, analytics, promo codes)
- `ai/` — AI chat feature (AiRepository, AIChatViewModel, AIChatScreen)
- `chat/` — Customer-admin real-time chat (ChatRepository, ChatWithShopScreen)
- `services/` — FCM push notifications (FcmService, FcmSender, NotificationHelper)
- `network/` — Retrofit client for SePay API
- `util/` — Utilities (GlobalNavigation, ConnectivityObserver, FormValidation, CloudinaryUploader)
- `ui/theme/` — Material3 theme (Color, Type, Theme)

### Navigation

String-based routes defined in `AppNavigation.kt`. No type-safe navigation args — values are passed as URL path segments and retrieved via `it.arguments?.getString()`. The `GlobalNavigation` object holds a reference to NavController for use outside composables.

### Key Data Flow

1. **Firestore collections**: `users`, `orders`, `data/stock/products`, `data/stock/categories`, `data/banners`, `promoCodes`, `notifications`, `chats`, `ai_chats`
2. **Auth**: Firebase Auth (email/password + Google Sign-In). Role stored in Firestore `users` doc (`"user"` or `"admin"`). Admin role auto-assigned for `@easyshop.com` Google emails or via secret code at signup.
3. **Cart**: Stored as `cartItems: Map<String, Long>` (productId → quantity) in the user's Firestore document
4. **Favorites**: Stored locally in SharedPreferences with optimistic UI, synced to Firestore subcollection `users/{uid}/favorites`
5. **Orders**: Atomic Firestore transaction that checks stock, decrements inventory, creates order doc, and clears cart
6. **AI Chat**: Gemini API (primary, free tier) with automatic fallback to Beeknoee (OpenAI-compatible). Product search uses token-based scoring with Vietnamese diacritics normalization.
7. **Push Notifications**: FCM v1 HTTP API using Service Account JSON (in `res/raw/fcm_service_account.json`). Sent via `FcmSender` to specific users, all admins, or broadcast.

### Payment Flow

CheckoutViewModel handles the full flow: cart → address selection → promo code → payment method selection → order placement. For bank transfers, SePay API polls for transaction confirmation. Mock payment dialog available for testing.

## Conventions

- **Language**: All user-facing strings are in Vietnamese. Code comments are predominantly in Vietnamese.
- **Opt-in**: `ExperimentalMaterial3Api` is globally opted-in via `freeCompilerArgs` in build.gradle.kts
- **Image loading**: Coil with global disk/memory cache configured in `EasyShopApplication`
- **Error translation**: `AppUtil.translateSystemError()` converts Firebase/payment error messages to Vietnamese
- **Notification pattern**: Dual notification — in-app banner (`NotifBannerController`) + system notification (`NotificationHelper`)
- **AppState**: `ScreenState` enum (LOADING, SUCCESS, ERROR, EMPTY) used across ViewModels for loading states
- **Testing**: Minimal — only placeholder unit and instrumented tests exist. MockK and coroutines-test are available as test dependencies.
