package com.blockchain.home.presentation.activity.common

import androidx.compose.runtime.Composable
import com.blockchain.componentlib.tablerow.custom.ViewStyle
import com.blockchain.componentlib.tablerow.custom.ViewType
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.theme.AppTheme

// styles - use domain ones instead to map
// text
enum class ActivityTextTypography {
    Paragraph2, Caption1
}

@Composable
fun ActivityTextTypography.toComposable() = when (this) {
    ActivityTextTypography.Paragraph2 -> AppTheme.typography.paragraph2
    ActivityTextTypography.Caption1 -> AppTheme.typography.caption1
}

enum class ActivityTextColor {
    Title, Muted, Success, Error, Warning
}

@Composable
fun ActivityTextColor.toComposable() = when (this) {
    ActivityTextColor.Title -> AppTheme.colors.title
    ActivityTextColor.Muted -> AppTheme.colors.muted
    ActivityTextColor.Success -> AppTheme.colors.success
    ActivityTextColor.Error -> AppTheme.colors.error
    ActivityTextColor.Warning -> AppTheme.colors.warning
}

data class ActivityTextStyle(
    val typography: ActivityTextTypography,
    val color: ActivityTextColor,
    val strikethrough: Boolean = false
)

// tag
enum class ActivityTagStyle {
    Success, Warning
}

fun ActivityTagStyle.toTagType() = when (this) {
    ActivityTagStyle.Success -> TagType.Success()
    ActivityTagStyle.Warning -> TagType.Warning()
}

// component
sealed interface ActivityStackView {
    data class Text(
        val value: String,
        val style: ActivityTextStyle
    ) : ActivityStackView

    data class Tag(
        val value: String,
        val style: ActivityTagStyle
    ) : ActivityStackView
}

@Composable
fun ActivityStackView.toViewType() = when (this) {
    is ActivityStackView.Tag -> {
        ViewType.Tag(
            value = value,
            style = style.toTagType()
        )
    }
    is ActivityStackView.Text -> {
        ViewType.Text(
            value = value,
            style = ViewStyle.TextStyle(
                style = style.typography.toComposable(),
                color = style.color.toComposable(),
                strikeThrough = style.strikethrough
            )
        )
    }
}
