package com.example.easyshop.productdetails

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyshop.R
import com.example.easyshop.model.ReviewModel
import com.example.easyshop.ui.theme.WarningColor
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ProductTabContent(
    selectedTab: Int,
    description: String,
    specifications: Map<String, String>,
    productId: String
) {
    var productRating by remember { mutableStateOf(0f) }
    var reviewCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(productId) {
        Firebase.firestore.collection("data").document("stock")
            .collection("products").document(productId)
            .addSnapshotListener { snapshot: DocumentSnapshot?, _ ->
                val rating = snapshot?.getDouble("rating")?.toFloat() ?: 0f
                val count = snapshot?.getLong("reviewCount")?.toInt() ?: 0
                productRating = rating
                reviewCount = count
            }
    }

    when (selectedTab) {
        0 -> DescriptionTab(description)
        1 -> SpecificationsTab(specifications)
        2 -> ReviewsTab(productId, productRating, reviewCount)
    }
}

@Composable
fun DescriptionTab(description: String) {
    var isExpanded by remember { mutableStateOf(false) }
    val parsed = remember(description) { parseDescription(description) }
    val detailText = remember(parsed.detailParagraphs) { parsed.detailParagraphs.joinToString("\n\n") }
    val canExpand = detailText.length > 260 || parsed.detailParagraphs.size > 2

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (parsed.highlights.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Điểm nổi bật",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    parsed.highlights.take(5).forEach { item ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Chi tiết sản phẩm",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = detailText.ifBlank { description },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded || !canExpand) Int.MAX_VALUE else 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (canExpand) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) stringResource(R.string.read_less) else stringResource(R.string.read_more),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private data class ParsedDescription(
    val highlights: List<String>,
    val detailParagraphs: List<String>
)

private fun parseDescription(raw: String): ParsedDescription {
    val normalized = raw
        .replace(Regex("\\s+"), " ")
        .replace(" | ", "|")
        .trim()

    if (normalized.isBlank()) {
        return ParsedDescription(emptyList(), emptyList())
    }

    val parts = normalized
        .split("|")
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val shortHighlights = parts
        .filter { it.length in 6..72 }
        .take(6)

    val longParagraph = parts
        .filter { it.length > 72 }
        .joinToString(" ")
        .trim()

    val details = buildList {
        if (longParagraph.isNotBlank()) add(longParagraph)
        if (longParagraph.isBlank()) add(normalized)
    }

    return ParsedDescription(
        highlights = shortHighlights,
        detailParagraphs = details
    )
}

@Composable
fun SpecificationsTab(specifications: Map<String, String>) {
    if (specifications.isNotEmpty()) {
        val specEntries = specifications
            .filter { it.key.isNotBlank() && it.value.isNotBlank() }
            .toList()

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Bảng thông số kỹ thuật",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "${specEntries.size} mục",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            specEntries.forEachIndexed { index, (key, value) ->
                SpecificationRow(label = key, value = value, index = index)
            }
        }
    } else {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.no_specifications),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SpecificationRow(label: String, value: String, index: Int) {
    val key = label.removeSuffix(":").trim()
    val rowColor = if (index % 2 == 0) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = rowColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = shortSpecKey(key),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = value.trim(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

private fun shortSpecKey(label: String): String {
    val words = label
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase(Locale.getDefault())
        words.size == 1 -> words[0].take(2).uppercase(Locale.getDefault())
        else -> "SP"
    }
}

@Composable
fun ReviewsTab(productId: String, productRating: Float, totalReviews: Int) {
    val reviewList = remember { mutableStateListOf<ReviewModel>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(productId) {
        Firebase.firestore.collection("reviews")
            .whereEqualTo("productId", productId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot: QuerySnapshot?, e: FirebaseFirestoreException? ->
                if (e != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                snapshot?.let {
                    reviewList.clear()
                    reviewList.addAll(it.toObjects(ReviewModel::class.java))
                }
                isLoading = false
            }
    }

    val ratingCounts = remember(reviewList.size) {
        (1..5).associateWith { star -> reviewList.count { it.rating.roundToInt() == star } }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OverallRatingCard(
            averageRating = productRating.toDouble(),
            reviewCount = totalReviews,
            ratingCounts = ratingCounts
        )

        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }

        reviewList.forEach { review ->
            ReviewCard(
                userName = review.userName,
                rating = review.rating.toInt(),
                comment = review.comment,
                date = "Vừa xong" // In a real app, parse review.timestamp
            )
        }

        WriteReviewSection(
            productId = productId,
            onReviewAdded = { /* Firestore snapshot handles update */ }
        )
    }
}

@Composable
fun OverallRatingCard(
    averageRating: Double,
    reviewCount: Int,
    ratingCounts: Map<Int, Int>
) {
    val roundedAvg = averageRating.roundToInt().coerceIn(0, 5)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WarningColor.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 24.dp)) {
                Text(
                    text = String.format(Locale.getDefault(), "%.1f", averageRating),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = WarningColor
                )
                Row {
                    repeat(5) { index ->
                        Icon(
                            Icons.Default.Star,
                            null,
                            tint = if (index < roundedAvg) WarningColor else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "$reviewCount ${stringResource(R.string.reviews_count_suffix)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                for (star in 5 downTo 1) {
                    RatingBar(
                        stars = star,
                        count = ratingCounts[star] ?: 0,
                        totalCount = reviewCount
                    )
                }
            }
        }
    }
}

@Composable
fun RatingBar(stars: Int, count: Int, totalCount: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text("$stars", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(16.dp))
        Icon(Icons.Default.Star, null, tint = WarningColor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = {
                if (totalCount == 0) 0f else count / totalCount.toFloat()
            },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .padding(end = 8.dp),
            color = WarningColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(30.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun WriteReviewSection(productId: String, onReviewAdded: () -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var accountName by remember(currentUser?.uid) {
        mutableStateOf(currentUser?.displayName?.trim().orEmpty())
    }
    var reviewText by remember { mutableStateOf("") }
    var selectedRating by remember { mutableIntStateOf(0) }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        try {
            val userDoc = Firebase.firestore.collection("users").document(uid).get().await()
            val firestoreName = userDoc.getString("name").orEmpty().trim()
            if (firestoreName.isNotBlank()) {
                accountName = firestoreName
            }
        } catch (_: Exception) { }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Viết đánh giá", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Đánh giá của bạn:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (index < selectedRating) WarningColor else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { if (!isSubmitting) selectedRating = index + 1 }
                    )
                }
            }

            OutlinedTextField(
                value = reviewText,
                onValueChange = { reviewText = it },
                label = { Text("Nội dung đánh giá") },
                placeholder = { Text("Chia sẻ trải nghiệm của bạn về sản phẩm...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                enabled = !isSubmitting
            )

            Button(
                onClick = {
                    val user = currentUser ?: return@Button
                    isSubmitting = true
                    val reviewId = Firebase.firestore.collection("reviews").document().id
                    val review = ReviewModel(
                        id = reviewId,
                        productId = productId,
                        userId = user.uid,
                        userName = accountName.ifBlank { user.email ?: "Người dùng" },
                        rating = selectedRating.toFloat(),
                        comment = reviewText.trim(),
                        timestamp = Timestamp.now()
                    )

                    Firebase.firestore.collection("reviews").document(reviewId).set(review)
                        .addOnSuccessListener {
                            // Update Product Rating in background
                            updateProductRating(productId, selectedRating.toFloat())
                            reviewText = ""
                            selectedRating = 0
                            isSubmitting = false
                            onReviewAdded()
                        }
                        .addOnFailureListener {
                            isSubmitting = false
                        }
                },
                enabled = selectedRating > 0 && reviewText.trim().length >= 5 && !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Gửi đánh giá", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun updateProductRating(productId: String, newRating: Float) {
    val productRef = Firebase.firestore.collection("data").document("stock")
        .collection("products").document(productId)

    Firebase.firestore.runTransaction { transaction: com.google.firebase.firestore.Transaction ->
        val snapshot = transaction.get(productRef)
        val currentRating = snapshot.getDouble("rating") ?: 0.0
        val currentCount = snapshot.getLong("reviewCount") ?: 0L

        val newCount = currentCount + 1
        val updatedRating = ((currentRating * currentCount) + newRating) / newCount

        transaction.update(productRef, "rating", updatedRating)
        transaction.update(productRef, "reviewCount", newCount)
    }.addOnSuccessListener {
        // Success
    }
}

@Composable
fun ReviewCard(userName: String, rating: Int, comment: String, date: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            userName.firstOrNull()?.toString() ?: "U",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(userName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Row {
                            repeat(5) { index ->
                                Icon(
                                    Icons.Default.Star,
                                    null,
                                    tint = if (index < rating) WarningColor else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Text(comment, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}