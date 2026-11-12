package com.developer_rahul.meetmind_ai.core.designsystem

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window Width Category following Material 3 Adaptive design specifications.
 */
enum class WindowWidthSize {
    COMPACT, // < 600dp (standard portrait phones)
    MEDIUM,  // 600dp - 839dp (tablets in portrait, foldables unfolded)
    EXPANDED // >= 840dp (tablets in landscape, desktop, large tablets)
}

/**
 * Window Height Category following Material 3 Adaptive design specifications.
 */
enum class WindowHeightSize {
    COMPACT, // < 480dp (landscape phones)
    MEDIUM,  // 480dp - 899dp (standard portrait phones)
    EXPANDED // >= 900dp (tall phones, tablets)
}

/**
 * Physical Device Form Factor inferred dynamically from real-time screen dimensions.
 */
enum class DeviceFormFactor {
    SMALL_PHONE,              // Width < 360dp (compact phones, split screen, compact mode)
    PHONE,                    // Width 360dp - 599dp (standard smartphones)
    TABLET,                   // Width 600dp - 839dp (7"-10" tablets, unfolded foldables)
    DESKTOP_OR_LARGE_TABLET   // Width >= 840dp (11"+ tablets, Chromebooks, Dex, desktop)
}

/**
 * Dynamic Responsive Layout specifications that reactively adapt UI parameters to device dimensions.
 */
data class ResponsiveLayoutInfo(
    val screenWidthDp: Dp,
    val screenHeightDp: Dp,
    val widthCategory: WindowWidthSize,
    val heightCategory: WindowHeightSize,
    val formFactor: DeviceFormFactor,
    val isLandscape: Boolean
) {
    val isSmallPhone: Boolean get() = formFactor == DeviceFormFactor.SMALL_PHONE
    val isPhone: Boolean get() = widthCategory == WindowWidthSize.COMPACT
    val isTablet: Boolean get() = widthCategory == WindowWidthSize.MEDIUM
    val isExpanded: Boolean get() = widthCategory == WindowWidthSize.EXPANDED
    val isTabletOrLarger: Boolean get() = isTablet || isExpanded

    /**
     * Recommended horizontal screen margins tailored to screen width.
     */
    val horizontalPadding: Dp
        get() = when {
            isSmallPhone -> 12.dp
            isPhone -> 16.dp
            isTablet -> 24.dp
            else -> 32.dp
        }

    /**
     * Recommended vertical item spacing for cards and sections.
     */
    val itemSpacing: Dp
        get() = when {
            isSmallPhone -> 8.dp
            isPhone -> 12.dp
            else -> 16.dp
        }

    /**
     * Recommended maximum width for forms and single-column cards to prevent stretching on large screens.
     */
    val formMaxWidth: Dp
        get() = when {
            isSmallPhone -> 360.dp
            isPhone -> 480.dp
            else -> 540.dp
        }

    /**
     * Recommended maximum width for dashboards and feeds.
     */
    val dashboardMaxWidth: Dp
        get() = when {
            isExpanded -> 1080.dp
            isTablet -> 840.dp
            else -> Dp.Unspecified
        }

    /**
     * Dynamic columns for dashboard Bento quick action tiles.
     */
    val bentoGridColumns: Int
        get() = when {
            isExpanded -> 4
            isTablet || (isLandscape && !isSmallPhone) -> 3
            else -> 2
        }

    /**
     * Dynamic columns for meeting video participant grid based on participant count and screen width.
     */
    fun calculateVideoGridColumns(participantCount: Int): Int {
        if (participantCount <= 1) return 1
        if (participantCount == 2) {
            return if (isLandscape) 2 else 1
        }
        return when {
            isExpanded -> if (participantCount >= 6) 3 else 2
            isTablet -> if (participantCount >= 5) 3 else 2
            isLandscape -> if (participantCount >= 4) 3 else 2
            else -> 2
        }
    }
}

val LocalResponsiveLayout = compositionLocalOf {
    ResponsiveLayoutInfo(
        screenWidthDp = 360.dp,
        screenHeightDp = 640.dp,
        widthCategory = WindowWidthSize.COMPACT,
        heightCategory = WindowHeightSize.MEDIUM,
        formFactor = DeviceFormFactor.PHONE,
        isLandscape = false
    )
}

/**
 * Computes and provides the dynamic [ResponsiveLayoutInfo] for the current device and orientation.
 */
@Composable
fun rememberResponsiveLayoutInfo(): ResponsiveLayoutInfo {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.dp
    val heightDp = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    return remember(widthDp, heightDp, isLandscape) {
        val widthCat = when {
            widthDp < 600.dp -> WindowWidthSize.COMPACT
            widthDp < 840.dp -> WindowWidthSize.MEDIUM
            else -> WindowWidthSize.EXPANDED
        }

        val heightCat = when {
            heightDp < 480.dp -> WindowHeightSize.COMPACT
            heightDp < 900.dp -> WindowHeightSize.MEDIUM
            else -> WindowHeightSize.EXPANDED
        }

        val formFactor = when {
            widthDp < 360.dp -> DeviceFormFactor.SMALL_PHONE
            widthDp < 600.dp -> DeviceFormFactor.PHONE
            widthDp < 840.dp -> DeviceFormFactor.TABLET
            else -> DeviceFormFactor.DESKTOP_OR_LARGE_TABLET
        }

        ResponsiveLayoutInfo(
            screenWidthDp = widthDp,
            screenHeightDp = heightDp,
            widthCategory = widthCat,
            heightCategory = heightCat,
            formFactor = formFactor,
            isLandscape = isLandscape
        )
    }
}

/**
 * Centered responsive container that automatically limits maximum content width and
 * dynamically adapts horizontal margins according to device category.
 */
@Composable
fun ResponsiveContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = Dp.Unspecified,
    contentAlignment: Alignment = Alignment.TopCenter,
    content: @Composable BoxScope.(ResponsiveLayoutInfo) -> Unit
) {
    val layoutInfo = LocalResponsiveLayout.current
    val targetMaxWidth = if (maxWidth != Dp.Unspecified) maxWidth else layoutInfo.dashboardMaxWidth

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = contentAlignment
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (targetMaxWidth != Dp.Unspecified) Modifier.widthIn(max = targetMaxWidth)
                    else Modifier
                )
                .fillMaxWidth(),
            content = { content(layoutInfo) }
        )
    }
}

