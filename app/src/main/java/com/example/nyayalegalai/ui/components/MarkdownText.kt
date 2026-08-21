package com.example.nyayalegalai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    val lines = text.split("\n")
    Column(modifier = modifier) {
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("### ") -> {
                    Text(
                        text = parseMarkdown(trimmed.removePrefix("### ")),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = parseMarkdown(trimmed.removePrefix("## ")),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = parseMarkdown(trimmed.removePrefix("# ")),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                    val content = if (trimmed.startsWith("- ")) trimmed.removePrefix("- ")
                                  else if (trimmed.startsWith("* ")) trimmed.removePrefix("* ")
                                  else trimmed.removePrefix("• ")
                    Row(modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseMarkdown(content),
                            style = MaterialTheme.typography.bodyLarge,
                            color = color
                        )
                    }
                }
                trimmed.isNotBlank() -> {
                    Text(
                        text = parseMarkdown(trimmed),
                        style = MaterialTheme.typography.bodyLarge,
                        color = color,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val nextBold = text.indexOf("**", cursor)
            val nextItalic = text.indexOf("*", cursor)
            
            val nextFormat = when {
                nextBold != -1 && nextItalic != -1 -> minOf(nextBold, nextItalic)
                nextBold != -1 -> nextBold
                nextItalic != -1 -> nextItalic
                else -> -1
            }
            
            if (nextFormat == -1) {
                append(text.substring(cursor))
                break
            }
            
            if (nextFormat > cursor) {
                append(text.substring(cursor, nextFormat))
            }
            
            if (nextFormat == nextBold) {
                val endBold = text.indexOf("**", nextBold + 2)
                if (endBold != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(nextBold + 2, endBold))
                    }
                    cursor = endBold + 2
                } else {
                    append("**")
                    cursor = nextBold + 2
                }
            } else {
                val endItalic = text.indexOf("*", nextItalic + 1)
                if (endItalic != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(nextItalic + 1, endItalic))
                    }
                    cursor = endItalic + 1
                } else {
                    append("*")
                    cursor = nextItalic + 1
                }
            }
        }
    }
}
