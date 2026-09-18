package com.medremind.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
private fun shimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = MedMotion.Decelerate),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(x, 0f),
        end = Offset(x + 600f, 0f)
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    corner: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(shimmerBrush())
    )
}

@Composable
fun SkeletonLine(
    width: Dp,
    height: Dp = 14.dp,
    modifier: Modifier = Modifier
) = SkeletonBox(modifier = modifier.width(width).height(height), corner = 7.dp)

@Composable
fun DoseCardSkeleton(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBox(modifier = Modifier.size(38.dp), corner = 13.dp)
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonLine(width = 90.dp, height = 16.dp)
                    SkeletonLine(width = 120.dp, height = 12.dp)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBox(modifier = Modifier.size(46.dp), corner = 50.dp)
                Spacer(Modifier.width(14.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkeletonLine(width = 150.dp, height = 15.dp)
                    SkeletonLine(width = 110.dp, height = 12.dp)
                }
                SkeletonBox(modifier = Modifier.width(64.dp).height(30.dp), corner = 50.dp)
            }
        }
    }
}

@Composable
fun TodaySkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SkeletonLine(width = 90.dp, height = 12.dp)
                    SkeletonLine(width = 180.dp, height = 20.dp)
                    SkeletonLine(width = 130.dp, height = 12.dp)
                }
                Spacer(Modifier.width(12.dp))
                SkeletonBox(modifier = Modifier.size(80.dp), corner = 50.dp)
            }
        }
        repeat(2) { DoseCardSkeleton() }
    }
}

@Composable
fun ListSkeleton(rows: Int = 4, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(rows) { index ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBox(
                        modifier = Modifier.size(46.dp),
                        corner = if (index % 2 == 0) 50.dp else 14.dp
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkeletonLine(width = (120 + index * 18).dp, height = 15.dp)
                        SkeletonLine(width = 90.dp, height = 12.dp)
                    }
                    SkeletonBox(modifier = Modifier.width(58.dp).height(26.dp), corner = 50.dp)
                }
            }
        }
    }
}

@Composable
fun AvatarSkeleton(modifier: Modifier = Modifier, size: Dp = 88.dp) {
    SkeletonBox(modifier = modifier.size(size), corner = 50.dp)
}

@Composable
fun CircleSkeleton(size: Dp) = SkeletonBox(modifier = Modifier.size(size), corner = size / 2)
