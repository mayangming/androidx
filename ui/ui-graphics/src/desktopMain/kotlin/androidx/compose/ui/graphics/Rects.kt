/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect

fun Rect.toSkijaRect(): org.jetbrains.skija.Rect {
    return org.jetbrains.skija.Rect.makeLTRB(
        left,
        top,
        right,
        bottom
    )
}

fun org.jetbrains.skija.Rect.toComposeRect() =
    androidx.compose.ui.geometry.Rect(left, top, right, bottom)

fun RoundRect.toSkijaRRect(): org.jetbrains.skija.RRect {
    val radii = FloatArray(8)

    radii[0] = topLeftRadiusX
    radii[1] = topLeftRadiusY

    radii[2] = topRightRadiusX
    radii[3] = topRightRadiusY

    radii[4] = bottomRightRadiusX
    radii[5] = bottomRightRadiusY

    radii[6] = bottomLeftRadiusX
    radii[7] = bottomLeftRadiusY

    return org.jetbrains.skija.RRect.makeComplexLTRB(left, top, right, bottom, radii)
}