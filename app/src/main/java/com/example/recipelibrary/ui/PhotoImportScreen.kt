package com.example.recipelibrary.ui

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.recipelibrary.R
import java.io.File

@Composable
fun PhotoImportScreen(
    onGalleryPicked: (Uri) -> Unit
) {
    val context = LocalContext.current

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionDenied by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) onGalleryPicked(uri) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { onGalleryPicked(it) }
        }
        pendingCameraUri = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val dir = File(context.cacheDir, "camera").also { it.mkdirs() }
            val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            showPermissionDenied = true
        }
    }

    fun launchCamera() {
        showPermissionDenied = false
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val headerIcon = painterResource(R.drawable.ic_nav_photo)
        Image(
            painter = headerIcon,
            contentDescription = null,
            // Même hauteur sur les trois pages d'ajout, largeur selon le dessin
            modifier = Modifier.height(96.dp)
                .aspectRatio(headerIcon.intrinsicSize.width / headerIcon.intrinsicSize.height)
        )
        Text(
            "Importer une photo",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTitleViolet
        )
        Text(
            "Prenez ou importez une photo d'une recette pour extraire automatiquement les ingrédients et étapes.",
            fontSize = 14.sp,
            color = ColorInactiveText,
            textAlign = TextAlign.Center
        )

        if (showPermissionDenied) {
            Card(
                backgroundColor = Color(0xFFFFEEEE),
                elevation = 0.dp,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Permission caméra refusée. Activez-la dans les paramètres de l'appli.",
                    modifier = Modifier.padding(10.dp),
                    color = Color(0xFFCC0000),
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = ::launchCamera,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = ColorVioletPrimary),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_camera),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Prendre une photo", color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = { galleryLauncher.launch("image/*") },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorTitleViolet),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, ColorVioletPrimary),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Choisir dans la galerie", fontWeight = FontWeight.SemiBold)
        }
    }
}
