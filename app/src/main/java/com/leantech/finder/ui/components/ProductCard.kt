package com.leantech.finder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.leantech.finder.domain.model.Product

@Composable
fun ProductCard(product: Product, index: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val jade = Color(0xFF3D8361)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlatformBadge(product.marketplace)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "#${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                product.salesText?.takeIf { it.isNotBlank() }?.let { sales ->
                    InfoChip(text = sales)
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = product.displayTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (product.titleRu != null && product.titleRu != product.title) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                product.priceCny?.let { price ->
                    Text(
                        text = "¥ ${"%.2f".format(price)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.width(10.dp))
                }
                product.salesCount?.let { count ->
                    InfoChip(
                        text = "продаж: $count",
                        container = jade.copy(alpha = 0.14f),
                        content = jade,
                    )
                }
                if (product.seller.isFactory) {
                    Spacer(Modifier.width(8.dp))
                    InfoChip(
                        text = "фабрика/производитель",
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            product.sellerDescription?.takeIf { it.isNotBlank() }?.let { description ->
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Spacer(Modifier.height(10.dp))
                SectionTitle("О продавце")
                Spacer(Modifier.height(6.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = product.itemUrl,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { openLink(context, product.itemUrl) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Товар", maxLines = 1)
                }
                product.seller.shopUrl?.let { shopUrl ->
                    OutlinedButton(
                        onClick = { openLink(context, shopUrl) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.height(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Продавец", maxLines = 1)
                    }
                }
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(product.itemUrl))
                    },
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.height(18.dp))
                }
            }
        }
    }
}
