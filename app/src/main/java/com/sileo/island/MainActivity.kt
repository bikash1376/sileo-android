package com.sileo.island

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sileo.island.ui.SileoHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DemoScreen() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DemoScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F5)),
    ) {
        // The playground controls.
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Sileo · Android",
                color = Color(0xFF18181B),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Tap any type to fire it live.",
                color = Color(0xFF71717A),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Chip("Success") {
                    Sileo.success("Changes Saved", "Changes saved successfully to the database. Please refresh the page to see the changes.")
                }
                Chip("Error") {
                    Sileo.error("Something Went Wrong", "We're having trouble saving your changes to the server. Please try again in a few minutes.")
                }
                Chip("Warning") {
                    Sileo.warning("Storage Almost Full", "You've used 95% of your storage. Consider upgrading your plan or removing files.")
                }
                Chip("Info") {
                    Sileo.info("New Update Available", "Version 0.2.0 is ready to install with performance improvements and bug fixes.")
                }
                Chip("Action") {
                    Sileo.action("Invitation Sent", "Aaryan invited you to collaborate on the Sileo project.", actionLabel = "Accept")
                }
                Chip("Promise") {
                    Sileo.promise("Saving changes…", "Syncing your edits to the cloud.")
                }
            }
        }

        // The notification overlay — paint it on top of everything.
        SileoHost()
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clickable(onClick = onClick)
            .background(Color.White, RoundedCornerShape(50))
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(label, color = Color(0xFF27272A), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
