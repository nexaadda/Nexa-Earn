package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WithdrawalMethod
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyanSecondary

@Composable
fun WithdrawalMethodLogo(
    method: WithdrawalMethod,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    when (method) {
        WithdrawalMethod.ESEWA_WALLET -> ESewaLogo(size = size, modifier = modifier)
        WithdrawalMethod.BANK_ACCOUNT -> BankAccountLogo(size = size, modifier = modifier)
        WithdrawalMethod.SIM_RECHARGE -> SimCardLogo(size = size, modifier = modifier)
    }
}

@Composable
fun ESewaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val eSewaGreen = Color(0xFF60BB46)
    val eSewaDarkGreen = Color(0xFF3E8E29)
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, eSewaGreen.copy(alpha = 0.7f)),
        color = Color.Transparent,
        modifier = modifier.size(size)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(eSewaGreen, eSewaDarkGreen)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "e",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.54f).sp
            )
        }
    }
}

@Composable
fun BankAccountLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val bankBlue = Color(0xFF0284C7)
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CyanSecondary.copy(alpha = 0.6f)),
        color = Color.Transparent,
        modifier = modifier.size(size)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(CyanSecondary, bankBlue)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AccountBalance,
                contentDescription = "Bank Account logo",
                tint = Color.White,
                modifier = Modifier.size(size * 0.56f)
            )
        }
    }
}

@Composable
fun SimCardLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val simDeepAmber = Color(0xFFB45309)
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.65f)),
        color = Color.Transparent,
        modifier = modifier.size(size)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(AmberGold, simDeepAmber)
                    )
                )
                .padding(size * 0.18f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height
                val notch = w * 0.28f

                // SIM card silhouette with top-right notched corner
                val simPath = Path().apply {
                    moveTo(w * 0.15f, 0f)
                    lineTo(w - notch, 0f)
                    lineTo(w, notch)
                    lineTo(w, h * 0.88f)
                    quadraticTo(w, h, w * 0.85f, h)
                    lineTo(w * 0.15f, h)
                    quadraticTo(0f, h, 0f, h * 0.88f)
                    lineTo(0f, h * 0.12f)
                    quadraticTo(0f, 0f, w * 0.15f, 0f)
                    close()
                }
                drawPath(
                    path = simPath,
                    color = Color.White.copy(alpha = 0.96f),
                    style = Stroke(width = w * 0.1f)
                )

                // Inner gold/white chip rectangle + contact lines
                val chipLeft = w * 0.24f
                val chipTop = h * 0.36f
                val chipW = w * 0.52f
                val chipH = h * 0.42f
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(chipLeft, chipTop),
                    size = Size(chipW, chipH),
                    cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
                )
                drawLine(
                    color = simDeepAmber,
                    start = Offset(chipLeft + chipW * 0.5f, chipTop),
                    end = Offset(chipLeft + chipW * 0.5f, chipTop + chipH),
                    strokeWidth = w * 0.06f
                )
                drawLine(
                    color = simDeepAmber,
                    start = Offset(chipLeft, chipTop + chipH * 0.5f),
                    end = Offset(chipLeft + chipW, chipTop + chipH * 0.5f),
                    strokeWidth = w * 0.06f
                )
            }
        }
    }
}
