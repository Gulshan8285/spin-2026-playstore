package com.spinwin.rewards.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.spinwin.rewards.data.model.CountryInfo
import com.spinwin.rewards.data.model.CountryRegistry
import com.spinwin.rewards.theme.BorderGlass
import com.spinwin.rewards.theme.InterFamily
import com.spinwin.rewards.theme.NeonPurple
import com.spinwin.rewards.theme.SoraFamily
import com.spinwin.rewards.theme.TextPrimary
import com.spinwin.rewards.theme.TextSecondary
import com.spinwin.rewards.theme.TextTertiary

/**
 * Worldwide Country Code & Currency Selector
 */
@Composable
fun CountryCodeButton(
    selectedCountry: CountryInfo,
    onCountrySelected: (CountryInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
            .clickable { showDialog = true }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = selectedCountry.flag,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = selectedCountry.dialCode,
                color = TextPrimary,
                fontFamily = SoraFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select country",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showDialog) {
        CountryPickerDialog(
            onDismiss = { showDialog = false },
            onSelect = {
                onCountrySelected(it)
                showDialog = false
            }
        )
    }
}

@Composable
fun CountryPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (CountryInfo) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            CountryRegistry.ALL_COUNTRIES
        } else {
            val q = searchQuery.trim().lowercase()
            CountryRegistry.ALL_COUNTRIES.filter {
                it.name.lowercase().contains(q) ||
                it.dialCode.contains(q) ||
                it.currencyCode.lowercase().contains(q) ||
                it.code.lowercase().contains(q)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141926))
                .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Country & Currency",
                            color = TextPrimary,
                            fontFamily = SoraFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Choose your region for local currency & payouts",
                            color = TextTertiary,
                            fontFamily = InterFamily,
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(onClick = onDismiss)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search field
                GlassSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search country, code (+1) or currency..."
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Country List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredCountries, key = { it.code }) { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .clickable { onSelect(country) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = country.flag, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = country.name,
                                        color = TextPrimary,
                                        fontFamily = InterFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Currency: ${country.currencyCode} (${country.currencySymbol})",
                                        color = TextTertiary,
                                        fontFamily = InterFamily,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonPurple.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = country.dialCode,
                                    color = NeonPurple,
                                    fontFamily = SoraFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
