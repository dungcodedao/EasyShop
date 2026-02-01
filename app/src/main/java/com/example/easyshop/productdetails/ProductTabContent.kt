package com.example.easyshop.productdetails

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProductTabContent(
    selectedTab: Int,
    description: String,
    specifications: Map<String, String>
) {
    when (selectedTab) {
        0 -> DescriptionTab(description)
        1 -> SpecificationsTab(specifications)
        2 -> ReviewsTab()
    }
}

@Composable
fun DescriptionTab(description: String) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    ) {
        Text(
            text = description,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            color = Color.Black,
            maxLines = if (isExpanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis
        )

        if (description.length > 200) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isExpanded) "Read Less" else "Read More",
                color = Color.Red,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun SpecificationsTab(specifications: Map<String, String>) {
    if (specifications.isNotEmpty()) {
        Column {
            specifications.forEach { (key, value) ->
                SpecificationRow(label = key, value = value)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.LightGray
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No specifications available",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun SpecificationRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 15.sp,
            color = Color.Gray,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
fun ReviewsTab() {
    Column {
        // Overall Rating Card
        OverallRatingCard()

        Spacer(modifier = Modifier.height(16.dp))

        // Sample Reviews
        ReviewCard(
            userName = "John Doe",
            rating = 5,
            comment = "Great product! Highly recommended. The quality is excellent and delivery was fast.",
            date = "2 days ago"
        )

        Spacer(modifier = Modifier.height(12.dp))

        ReviewCard(
            userName = "Jane Smith",
            rating = 4,
            comment = "Good value for money. Works as expected, but could be better packaged.",
            date = "1 week ago"
        )

        Spacer(modifier = Modifier.height(12.dp))

        ReviewCard(
            userName = "Mike Johnson",
            rating = 5,
            comment = "Perfect! Exactly what I was looking for. Fast shipping and great customer service.",
            date = "2 weeks ago"
        )
    }
}

// ========== REVIEW COMPONENTS ==========

@Composable
fun OverallRatingCard() {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color(0xFFFFF9C4)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Big Rating
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 24.dp)
            ) {
                Text(
                    text = "4.5",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF8F00)
                )
                Row {
                    repeat(5) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "120 reviews",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Right: Rating Breakdown
            Column(modifier = Modifier.weight(1f)) {
                RatingBar(stars = 5, count = 85)
                RatingBar(stars = 4, count = 25)
                RatingBar(stars = 3, count = 7)
                RatingBar(stars = 2, count = 2)
                RatingBar(stars = 1, count = 1)
            }
        }
    }
}

@Composable
fun RatingBar(stars: Int, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = "$stars",
            fontSize = 12.sp,
            modifier = Modifier.width(16.dp)
        )
        androidx.compose.material3.Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFFFC107),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        androidx.compose.material3.LinearProgressIndicator(
            progress = { count / 120f },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .padding(end = 8.dp),
            color = Color(0xFFFF8F00),
            trackColor = Color(0xFFEEEEEE)
        )
        Text(
            text = "$count",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.width(30.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
fun ReviewCard(userName: String, rating: Int, comment: String, date: String) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Red, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.first().toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = userName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        // Star Rating
                        Row {
                            repeat(5) { index ->
                                androidx.compose.material3.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (index < rating) Color(0xFFFFC107) else Color(0xFFE0E0E0),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = date,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = comment,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color.Black
            )
        }
    }
}