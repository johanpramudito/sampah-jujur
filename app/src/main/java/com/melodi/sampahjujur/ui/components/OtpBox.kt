package com.melodi.sampahjujur.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import com.melodi.sampahjujur.ui.theme.SampahJujurTheme

/**
 * Reusable OTP input box component
 *
 * A single digit input field styled as a circular box with green border when filled.
 * Used in phone authentication flows for entering verification codes.
 *
 * @param value Current value of this OTP box (single digit string)
 * @param onValueChange Callback when value changes, receives new value
 * @param modifier Optional modifier for customization
 */
@Composable
fun OtpBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
            .size(50.dp)
            .border(
                width = 2.dp,
                color = if (value.isNotBlank()) PrimaryGreen else Color.LightGray,
                shape = CircleShape
            )
            .background(Color.White, CircleShape),
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "",
                        color = Color.LightGray,
                        fontSize = 20.sp
                    )
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )
                }
                // Keep innerTextField invisible but functional for text input
                Box(modifier = Modifier.size(0.dp)) {
                    innerTextField()
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun OtpBoxPreview() {
    SampahJujurTheme {
        Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            OtpBox(value = "5", onValueChange = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OtpBoxEmptyPreview() {
    SampahJujurTheme {
        Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            OtpBox(value = "", onValueChange = {})
        }
    }
}
