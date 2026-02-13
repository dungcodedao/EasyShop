package com.example.easyshop.productdetails

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.easyshop.R

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
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (isExpanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis
        )

        if (description.length > 200) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isExpanded) stringResource(R.string.read_less) else stringResource(R.string.read_more),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { isExpanded = !isExpanded }.padding(vertical = 4.dp)
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
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    } else {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_specifications), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SpecificationRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(0.4f))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.6f))
    }
}

@Composable
fun ReviewsTab() {
    Column {
        OverallRatingCard()
        Spacer(Modifier.height(16.dp))

        ReviewCard("Nguyễn Văn A", 5, "Sản phẩm tuyệt vời! Rất đáng tiền. Chất lượng hoàn thiện tốt và giao hàng nhanh chóng.", "2 ngày trước")
        Spacer(Modifier.height(12.dp))
        ReviewCard("Trần Thị B", 4, "Đáng đồng tiền bát gạo. Hoạt động tốt như mong đợi, nhưng đóng gói có thể làm cẩn thận hơn.", "1 tuần trước")
        Spacer(Modifier.height(12.dp))
        ReviewCard("Lê Văn C", 5, "Hoàn hảo! Chính xác là những gì tôi đang tìm kiếm. Giao hàng thần tốc và dịch vụ khách hàng tốt.", "2 tuần trước")
    }
}

@Composable
fun OverallRatingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFC107).copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 24.dp)) {
                Text("4.5", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFF8F00))
                Row {
                    repeat(5) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("120 ${stringResource(R.string.reviews_count_suffix)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(modifier = Modifier.weight(1f)) {
                RatingBar(5, 85)
                RatingBar(4, 25)
                RatingBar(3, 7)
                RatingBar(2, 2)
                RatingBar(1, 1)
            }
        }
    }
}

@Composable
fun RatingBar(stars: Int, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text("$stars", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(16.dp))
        Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { count / 120f },
            modifier = Modifier.weight(1f).height(6.dp).padding(end = 8.dp),
            color = Color(0xFFFF8F00),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text("$count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(userName.first().toString(), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(userName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Row {
                            repeat(5) { index ->
                                Icon(Icons.Default.Star, null, tint = if (index < rating) Color(0xFFFFC107) else MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(14.dp))
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