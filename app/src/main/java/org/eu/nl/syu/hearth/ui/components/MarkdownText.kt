/*
 * Hearth: An offline AI chat application for Android.
 * Copyright (C) 2026 syulze <me@syu.nl.eu.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.eu.nl.syu.hearth.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.CodeBlockStyle
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    smallFontSize: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val fontSize = if (smallFontSize) 14.sp else 16.sp
    val lineHeight = if (smallFontSize) 20.sp else 24.sp

    ProvideTextStyle(
        value = TextStyle(
            fontSize = fontSize,
            lineHeight = lineHeight,
            color = textColor,
            letterSpacing = 0.2.sp,
        )
    ) {
        RichText(
            modifier = modifier,
            style = RichTextStyle(
                codeBlockStyle = CodeBlockStyle(
                    textStyle = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp,
                    )
                ),
                stringStyle = RichTextStringStyle(
                    linkStyle = TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
                ),
            ),
        ) {
            Markdown(content = text)
        }
    }
}
