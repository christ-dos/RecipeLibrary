package com.example.recipelibrary.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.example.recipelibrary.data.FileTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipelibrary.R

@Composable
fun FileImportScreen(
    onFilePicked: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { FileTextExtractor.extract(context, uri) }
                }
                val text = result.getOrNull()
                when {
                    result.exceptionOrNull() is FileTextExtractor.UnsupportedFormatException ->
                        Toast.makeText(context, "Format non pris en charge (TXT, PDF ou DOCX)", Toast.LENGTH_LONG).show()
                    text.isNullOrBlank() ->
                        Toast.makeText(context, "Aucun texte trouvé dans ce fichier", Toast.LENGTH_LONG).show()
                    else -> onFilePicked(text)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.ic_nav_file),
            contentDescription = null,
            modifier = Modifier.size(96.dp)
        )
        Text(
            "Importer un fichier",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4A16C6)
        )
        Text(
            "Importez un fichier depuis votre appareil (PDF, TXT ou DOCX) pour créer une recette.",
            fontSize = 14.sp,
            color = Color(0xFF625B70),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .border(2.dp, Color(0xFFEEEAF7), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_upload),
                    contentDescription = null,
                    tint = Color(0xFF7A2CF6),
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    "Glissez votre fichier ici",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4A16C6)
                )
                Text(
                    "ou parcourez",
                    fontSize = 13.sp,
                    color = Color(0xFF7A2CF6),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Button(
            onClick = { launcher.launch(FileTextExtractor.SUPPORTED_MIME_TYPES) },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF7A2CF6)),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Choisir un fichier", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}
