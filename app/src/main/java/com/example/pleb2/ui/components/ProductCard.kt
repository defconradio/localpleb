package com.example.pleb2.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pleb2.R
import com.example.data.uiModels.ProductUiModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.text.style.TextOverflow
import com.example.nostr.verify
import android.util.Log

@Composable
fun ProductCard(
    model: ProductUiModel,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)? = null, // Optional edit action, only used in MyStallScreen
    onDeleteClick: (() -> Unit)? = null,
    onVerificationResult: (Boolean) -> Unit
) {
    val isVerified  = true
    //val isVerified = remember(model.event_id) { model.envelope?.verify() ?: false }
    if (isVerified) {
        Log.i("ProductCard", "Verification passed for event: ${model.event_id}")
    }

    onVerificationResult(isVerified)
    //val borderColor = if (isVerified) MaterialTheme.colorScheme.outline else Color.Red
    val borderColor = if (isVerified) Color.Transparent else Color.Red


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .border(1.dp, borderColor, shape = CardDefaults.shape) // Use theme outline color
            .clickable(onClickLabel = "View product details") { onClick() },
        //colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Use theme surface color
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            //verticalAlignment = Alignment.CenterVertically
        ) {
            // Product image
            val imageUrl = model.images?.firstOrNull()?.takeIf { !it.isNullOrBlank() } ?: ""
            if (imageUrl.isNotBlank()) {
                coil.compose.AsyncImage(
                    model = imageUrl,
                    placeholder = painterResource(id = R.drawable.no_image),
                    error = painterResource(id = R.drawable.no_image),
                    contentDescription = model.name,
                    modifier = Modifier
                        .size(150.dp)
                        .aspectRatio(1f), // Ensures the image is a perfect square
                    contentScale = ContentScale.Crop // Revert to Crop for full area fill
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.no_image),
                    contentDescription = "Placeholder image",
                    modifier = Modifier
                        .size(150.dp)
                        .aspectRatio(1f), // Ensures the image is a perfect square
                    contentScale = ContentScale.Crop // Revert to Crop for full area fill
                )
            }
          Spacer(modifier = Modifier.width(12.dp))
            // Product details
            Column(modifier = Modifier.weight(1f)) {
                // Remove line breaks (\n) from the description if present
                val cleanDescription = model.description?.replace("\n", " ")
                // Always reserve space for 3 lines, even if text is short
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp) // Reduced height for less separation between lines
                ) {
                    Text(
                        text = model.name + ", " + (cleanDescription ?: ""),
                        maxLines = 2,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.TopStart),
                        lineHeight = 18.sp // Slightly less than default for tighter lines
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                ) {
                    Text(
                        text = "${model.price ?: ""} ${model.currency}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                     //   .height(20.dp)
                ) {
                    // Format created_at (Long) to a readable relative time string (Today, Yesterday, X days ago)
                    val lastTime = if (model.created_at > 0L) {
                        val now = System.currentTimeMillis() / 1000 // seconds
                        val daysAgo = ((now - model.created_at) / 86400).toInt()
                        when (daysAgo) {
                            0 -> "Today"
                            1 -> "Yesterday"
                            else -> "$daysAgo days ago"
                        }
                    } else ""
                    if (model.quantity != null || lastTime.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (lastTime.isNotEmpty()) {
                                Text(
                                    text = lastTime,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (model.quantity != null) {
                                Text(
                                    text = "Stock: ${model.quantity}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                ) {
                    // LocalBitcoins-style rating system row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "98%",
                            fontSize = 12.sp,
                            color = Color(0xFF388E3C), // green
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " / 765 trades",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Clock",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "2 min",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column {
                onEditClick?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
                onDeleteClick?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}
