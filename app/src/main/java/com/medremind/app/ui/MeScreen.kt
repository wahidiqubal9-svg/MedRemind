package com.medremind.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.medremind.app.data.PhotoStorage
import java.io.File

private val sexOptions = listOf("Female", "Male", "Other")

@Composable
fun MeScreen(
    modifier: Modifier = Modifier,
    settings: SettingsViewModel,
    onBack: () -> Unit,
    onOpenCaregiver: () -> Unit = {},
    onOpenCaregiving: () -> Unit = {}
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var editing by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) settings.updateProfilePhoto(PhotoStorage.copyToInternal(context, uri))
    }

    val hasProfile = settings.profileName.isNotBlank() ||
        settings.profileAge.isNotBlank() ||
        settings.profileSex.isNotBlank() ||
        settings.profileWeight.isNotBlank() ||
        settings.profileHeight.isNotBlank() ||
        settings.profileDiseases.isNotEmpty() ||
        settings.profilePhoto != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 110.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "YOUR ACCOUNT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Me",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        MedCard(modifier = Modifier.fillMaxWidth()) {
            CaregiverEntryRow(
                icon = Icons.Rounded.Favorite,
                title = "Caregiver",
                subtitle = "Someone who helps me with my medicines",
                onClick = onOpenCaregiver
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Spacer(Modifier.height(4.dp))
            CaregiverEntryRow(
                icon = Icons.Rounded.Group,
                title = "Caregiving",
                subtitle = "People I help with their medicines",
                onClick = onOpenCaregiving
            )
        }

        if (!editing && hasProfile) {
            ProfileCard(
                settings = settings,
                onEdit = { editing = true }
            )
        } else {
            ProfileForm(
                settings = settings,
                showCancel = hasProfile,
                onPickPhoto = {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onSave = { editing = false },
                onCancel = { editing = false }
            )
        }
    }
}

@Composable
private fun ProfileCard(
    settings: SettingsViewModel,
    onEdit: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .background(MedGradients.heroHorizontal())
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AvatarWithBadge(settings = settings, onClick = onEdit, showCamera = false)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = settings.profileName.ifBlank { "Your profile" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(20.dp))
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatBox(
                            label = "Age",
                            value = settings.profileAge.ifBlank { "—" },
                            modifier = Modifier.weight(1f)
                        )
                        StatBox(
                            label = "Sex",
                            value = settings.profileSex.ifBlank { "—" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatBox(
                            label = "Weight",
                            value = settings.profileWeight.let { if (it.isBlank()) "—" else "$it kg" },
                            modifier = Modifier.weight(1f)
                        )
                        StatBox(
                            label = "Height",
                            value = settings.profileHeight.let { if (it.isBlank()) "—" else "$it cm" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (settings.profileDiseases.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Text(
                            "CONDITIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            settings.profileDiseases.joinToString(" \u00b7 "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Surface(
                    onClick = onEdit,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Edit profile", fontWeight = FontWeight.ExtraBold)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun AvatarWithBadge(
    settings: SettingsViewModel,
    onClick: () -> Unit,
    showCamera: Boolean = true
) {
    Box(modifier = Modifier.size(96.dp)) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(4.dp, MaterialTheme.colorScheme.surface),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                val photo = settings.profilePhoto
                if (photo != null) {
                    AsyncImage(
                        model = File(photo),
                        contentDescription = "Profile photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }
        }
        if (showCamera) {
            Surface(
                onClick = onClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoCamera,
                        contentDescription = "Change photo",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun ProfileForm(
    settings: SettingsViewModel,
    showCancel: Boolean,
    onPickPhoto: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    MedCard(modifier = Modifier
        .fillMaxWidth()
        .padding(top = 22.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileAvatar(photoPath = settings.profilePhoto, onClick = onPickPhoto, size = 120.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Tap the photo to change it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(18.dp))

        ProfileField(
            label = "Name",
            value = settings.profileName,
            onValueChange = settings::updateProfileName
        )
        Spacer(Modifier.height(10.dp))
        ProfileField(
            label = "Age",
            value = settings.profileAge,
            onValueChange = settings::updateProfileAge,
            number = true
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Sex",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        MedSegmentedButtons(
            options = sexOptions,
            selectedIndex = sexOptions.indexOf(settings.profileSex),
            onSelect = { settings.updateProfileSex(sexOptions[it]) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        ProfileField(
            label = "Weight (kg)",
            value = settings.profileWeight,
            onValueChange = settings::updateProfileWeight,
            number = true
        )
        Spacer(Modifier.height(10.dp))
        ProfileField(
            label = "Height (cm)",
            value = settings.profileHeight,
            onValueChange = settings::updateProfileHeight,
            number = true
        )

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Conditions",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${settings.profileDiseases.size}/$MAX_DISEASES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        var showDiseaseMenu by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(
                onClick = { showDiseaseMenu = true },
                enabled = settings.profileDiseases.size < MAX_DISEASES,
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add condition")
            }
            DropdownMenu(
                expanded = showDiseaseMenu,
                onDismissRequest = { showDiseaseMenu = false }
            ) {
                commonDiseases.forEach { disease ->
                    val already = settings.profileDiseases.contains(disease)
                    DropdownMenuItem(
                        text = { Text(disease) },
                        enabled = !already,
                        onClick = {
                            settings.addDisease(disease)
                            showDiseaseMenu = false
                        }
                    )
                }
            }
        }
        if (settings.profileDiseases.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                settings.profileDiseases.forEach { disease ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = disease,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { settings.removeDisease(disease) }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Remove $disease",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        GradientPillButton(
            text = "Save",
            onClick = onSave,
            modifier = Modifier.fillMaxWidth()
        )
        if (showCancel) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    photoPath: String?,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (photoPath != null) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = "Profile photo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(2.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.PhotoCamera,
                contentDescription = "Change photo",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(7.dp)
                    .size(16.dp)
            )
        }
    }
}

@Composable
private fun CaregiverEntryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    number: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            onValueChange(if (number) input.filter { it.isDigit() || it == '.' } else input)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = if (number) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        },
        modifier = Modifier.fillMaxWidth()
    )
}

