package com.blockchain.componentlib.button

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.R

@Composable
fun FixedSizeButtonContent(
    state: ButtonState,
    text: String,
    textColor: Color,
    textAlpha: Float,
    modifier: Modifier = Modifier,
    @DrawableRes loadingIconResId: Int = R.drawable.ic_loading
) {
    Box(modifier) {
        if (state == ButtonState.Loading) {
            ButtonLoadingIndicator(
                modifier = Modifier.align(Alignment.Center),
                loadingIconResId = loadingIconResId
            )
        }
        Text(
            text = text,
            color = textColor,
            modifier = Modifier.alpha(textAlpha)
        )
    }
}