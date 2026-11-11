package id.upnyk.legacyvault.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.upnyk.legacyvault.ui.theme.EyebrowStyle
import id.upnyk.legacyvault.ui.theme.MonoStyle
import id.upnyk.legacyvault.ui.theme.VaultAccent
import id.upnyk.legacyvault.ui.theme.VaultBorder
import id.upnyk.legacyvault.ui.theme.VaultBrand
import id.upnyk.legacyvault.ui.theme.VaultError
import id.upnyk.legacyvault.ui.theme.VaultHairline
import id.upnyk.legacyvault.ui.theme.VaultInfo
import id.upnyk.legacyvault.ui.theme.VaultOnPrimary
import id.upnyk.legacyvault.ui.theme.VaultPrimary
import id.upnyk.legacyvault.ui.theme.VaultRaised
import id.upnyk.legacyvault.ui.theme.VaultSurface
import id.upnyk.legacyvault.ui.theme.VaultText
import id.upnyk.legacyvault.ui.theme.VaultTextFaint
import id.upnyk.legacyvault.ui.theme.VaultTextMuted
import id.upnyk.legacyvault.ui.theme.VaultWarning

object VaultSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

enum class PillTone { OK, WARN, ERROR, INFO, NEUTRAL }

fun PillTone.color(): Color = when (this) {
    PillTone.OK -> VaultAccent
    PillTone.WARN -> VaultWarning
    PillTone.ERROR -> VaultError
    PillTone.INFO -> VaultInfo
    PillTone.NEUTRAL -> VaultTextMuted
}

/** Garis pemisah tipis. Dipakai menggantikan kartu bertumpuk agar layar lebih lapang. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(VaultHairline))
}

/** Label kategori kecil di atas judul. */
@Composable
fun Eyebrow(text: String, tint: Color = VaultBrand) {
    Text(text.uppercase(), style = EyebrowStyle, color = tint)
}

@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    borderColor: Color = VaultBorder,
    padding: Int = 16,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(VaultSurface, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(padding.dp),
        content = content,
    )
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = VaultText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = VaultTextMuted)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(VaultSpacing.sm))
            trailing()
        }
    }
}

/** Status tanpa kotak: titik + teks. Mengurangi kotak-kotak di layar. */
@Composable
fun StatusPill(label: String, tone: PillTone, modifier: Modifier = Modifier, keepCase: Boolean = false) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.size(6.dp).background(tone.color(), RoundedCornerShape(999.dp)))
        Spacer(Modifier.width(6.dp))
        Text(
            if (keepCase) label else label.uppercase(),
            style = EyebrowStyle,
            color = tone.color(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MonoBlock(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 6,
    tint: Color = VaultTextMuted,
) {
    Text(
        text = text,
        style = MonoStyle,
        color = tint,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun LabeledValue(label: String, value: String, tint: Color = VaultText) {
    Text(label.uppercase(), style = EyebrowStyle, color = VaultTextFaint)
    Spacer(Modifier.height(3.dp))
    Text(value, style = MaterialTheme.typography.bodyMedium, color = tint)
}

@Composable
fun VaultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
) {
    val interaction = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        singleLine = singleLine,
        minLines = minLines,
        enabled = enabled,
        visualTransformation = visualTransformation,
        interactionSource = interaction,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = when {
                    !enabled -> VaultBorder.copy(alpha = 0.5f)
                    focused -> VaultBrand
                    else -> VaultHairline
                },
                shape = RoundedCornerShape(8.dp),
            ),
        colors = TextFieldDefaults.colors(
            focusedTextColor = VaultText,
            unfocusedTextColor = VaultText,
            focusedContainerColor = VaultRaised,
            unfocusedContainerColor = VaultRaised,
            disabledContainerColor = VaultRaised,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedLabelColor = VaultBrand,
            unfocusedLabelColor = VaultTextMuted,
            cursorColor = VaultBrand,
            focusedPlaceholderColor = VaultTextFaint,
            unfocusedPlaceholderColor = VaultTextFaint,
        ),
    )
}

/** Satu-satunya tombol terisi per layar. */
@Composable
fun VaultButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    tone: PillTone = PillTone.OK,
) {
    val container = if (tone == PillTone.OK) VaultPrimary else tone.color()
    val content = if (tone == PillTone.OK) VaultOnPrimary else VaultOnPrimary
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(46.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.20f),
            disabledContentColor = content.copy(alpha = 0.45f),
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun VaultOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(46.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = VaultText,
            disabledContentColor = VaultTextFaint.copy(alpha = 0.55f),
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun InfoLine(text: String, tone: PillTone = PillTone.NEUTRAL) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = tone.color())
}

@Composable
fun EmptyState(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VaultHairline, RoundedCornerShape(8.dp))
            .padding(VaultSpacing.md),
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = VaultTextMuted)
    }
}

@Composable
fun MetricRow(label: String, value: String, valueTint: Color = VaultText) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = VaultTextMuted)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = valueTint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Deret metrik sejajar untuk ringkasan status di beranda. */
@Composable
fun MetricGrid(cells: List<Triple<String, String, Color>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
        cells.forEach { (label, value, tint) ->
            Column(Modifier.weight(1f)) {
                Text(label.uppercase(), style = EyebrowStyle, color = VaultTextFaint, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Baris navigasi modul. Dipakai di drawer dan beranda agar tidak ada dua sumber. */
@Composable
fun NavRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: String? = null,
    active: Boolean = false,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (active) VaultRaised else Color.Transparent,
                RoundedCornerShape(8.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    !enabled -> VaultTextMuted
                    active -> VaultBrand
                    else -> VaultText
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(VaultSpacing.sm))
            Text(
                trailing,
                style = EyebrowStyle,
                color = if (active) VaultBrand else VaultTextMuted,
            )
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = VaultTextFaint,
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * Bagian "Detail kripto" yang tertutup secara default.
 * Semua hash/ciphertext mentah tinggal di sini, bukan tersebar di badan layar.
 */
@Composable
fun Disclosure(
    label: String,
    modifier: Modifier = Modifier,
    initiallyOpen: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by remember { mutableStateOf(initiallyOpen) }
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = EyebrowStyle,
                color = if (open) VaultBrand else VaultTextMuted,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = if (open) "Tutup detail" else "Buka detail",
                tint = if (open) VaultBrand else VaultTextMuted,
                modifier = Modifier.size(16.dp).rotate(if (open) 180f else 0f),
            )
        }
        AnimatedVisibility(visible = open) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = VaultSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                content = content,
            )
        }
    }
}
