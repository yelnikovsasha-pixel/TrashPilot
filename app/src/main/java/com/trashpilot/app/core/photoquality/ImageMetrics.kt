package com.trashpilot.app.core.photoquality

import kotlin.math.pow

fun measureLuminance(
    luminance: IntArray,
    width: Int,
    height: Int,
    thresholds: PhotoQualityThresholds = PhotoQualityThresholds()
): PhotoMetrics? {
    if (width < 3 || height < 3 || luminance.size < width * height) return null
    val values = luminance.take(width * height).map { it.coerceIn(0, 255) }
    val mean = values.average()
    val darkFraction = values.count { it <= thresholds.darkPixelMaximum }.toDouble() / values.size
    val brightFraction = values.count { it >= thresholds.brightPixelMinimum }.toDouble() / values.size
    val laplacian = ArrayList<Double>((width - 2) * (height - 2))
    for (y in 1 until height - 1) for (x in 1 until width - 1) {
        val center = values[y * width + x]
        laplacian += (values[(y - 1) * width + x] + values[(y + 1) * width + x] +
            values[y * width + x - 1] + values[y * width + x + 1] - 4 * center).toDouble()
    }
    val lapMean = laplacian.average()
    val variance = laplacian.sumOf { (it - lapMean).pow(2) } / laplacian.size
    return PhotoMetrics(mean, darkFraction, brightFraction, variance)
}
