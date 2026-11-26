package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.AdminModerationItem
import com.example.data.remote.AppConfigDoc
import com.example.data.remote.SubmissionStatus
import com.example.data.remote.ViralSubmissionDoc
import com.example.ui.UiState
import com.example.ui.ViralSubmissionUiState
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BgDark
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.LineBorder
import com.example.ui.theme.PanelDark
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun ViralUploadSheet(
    isAdmin: Boolean,
    uiState: ViralSubmissionUiState,
    easypaisaConfigState: UiState<AppConfigDoc?>,
    mySubmissionsState: UiState<List<ViralSubmissionDoc>>,
    adminQueueState: UiState<List<AdminModerationItem>>,
    onUpdateState: ((ViralSubmissionUiState) -> ViralSubmissionUiState) -> Unit,
    onSaveMerchantConfig: (String, String, () -> Unit) -> Unit,
    onCopyMerchantNumber: () -> Unit,
    onOpenEasypaisa: () -> Unit,
    onVerifyDeposit: () -> Unit,
    onPublishViralReel: () -> Unit,
    onBackToDepositStep: () -> Unit,
    onApproveSubmission: (String) -> Unit,
    onRejectSubmission: (String, String) -> Unit,
    onJumpToSubmission: (ViralSubmissionDoc) -> Unit,
    onDeleteSubmission: (ViralSubmissionDoc) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val merchantConfig = (easypaisaConfigState as? UiState.Success)?.data
    val isMerchantConfigured = merchantConfig?.isConfigured == true

    var showMerchantEditor by remember(isAdmin, isMerchantConfigured) {
        mutableStateOf(isAdmin && easypaisaConfigState is UiState.Success && !isMerchantConfigured)
    }
    var merchantTitleDraft by remember(merchantConfig?.accountTitle) {
        mutableStateOf(merchantConfig?.accountTitle.orEmpty())
    }
    var merchantNumberDraft by remember(merchantConfig?.accountNumber) {
        mutableStateOf(merchantConfig?.accountNumber.orEmpty())
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateState { it.copy(pickedVideoUri = uri) }
        }
    }

    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateState { it.copy(receiptUri = uri) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("viral_upload_sheet")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Boost Link & Go Viral",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (isAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldSoft)
                                .border(1.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ADMIN",
                                color = GoldPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                Text(
                    text = "Pay 100 PKR via Easypaisa & submit for admin approval",
                    color = TextDim,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SurfaceCard)
                    .border(1.dp, LineBorder, CircleShape)
                    .testTag("viral_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Boost Link sheet",
                    tint = TextPrimary
                )
            }
        }

        // Step Progress Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StepPill(
                stepNumber = 1,
                label = "100 PKR Easypaisa",
                isActive = uiState.step == 1,
                isCompleted = uiState.isDepositVerified,
                modifier = Modifier.weight(1f)
            )
            StepPill(
                stepNumber = 2,
                label = "Video & Link",
                isActive = uiState.step == 2,
                isCompleted = false,
                isLocked = !uiState.isDepositVerified,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            // Receiving Easypaisa Account Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(PanelDark)
                    .border(1.dp, AccentGreen.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentGreen.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Easypaisa Deposit Account",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Fee: 100 PKR per viral video link",
                                color = AccentGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (isAdmin) {
                        TextButton(
                            onClick = { showMerchantEditor = !showMerchantEditor },
                            modifier = Modifier.testTag("toggle_merchant_editor")
                        ) {
                            Icon(
                                imageVector = if (showMerchantEditor) Icons.Default.Close else Icons.Default.Settings,
                                contentDescription = "Edit Easypaisa receiving account",
                                tint = GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isMerchantConfigured) {
                                    if (showMerchantEditor) "Done" else "Edit"
                                } else {
                                    "Setup"
                                },
                                color = GoldPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                when (easypaisaConfigState) {
                    is UiState.Loading -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = GoldPrimary,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Loading receiving Easypaisa account...",
                                color = TextDim,
                                fontSize = 12.sp
                            )
                        }
                    }
                    is UiState.Error -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = easypaisaConfigState.message,
                            color = DangerRed,
                            fontSize = 12.sp
                        )
                    }
                    is UiState.Success -> {
                        if (isMerchantConfigured && !showMerchantEditor && merchantConfig != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BgDark)
                                    .border(1.dp, LineBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = merchantConfig.accountTitle,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = merchantConfig.accountNumber,
                                        color = GoldPrimary,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = onCopyMerchantNumber,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, LineBorder),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Easypaisa number",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = onOpenEasypaisa,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AccentGreen,
                                            contentColor = Color(0xFF04140D)
                                        ),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = "Open Easypaisa app",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else if (!isMerchantConfigured && !isAdmin) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "The admin has not configured the receiving Easypaisa number yet. Check back shortly.",
                                color = TextDim,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (isAdmin && showMerchantEditor) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Admin Setup: Enter the Easypaisa account where 100 PKR deposits from all users should be sent:",
                        color = TextDim,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = merchantTitleDraft,
                        onValueChange = { merchantTitleDraft = it },
                        label = { Text("Account Title (e.g. Store Owner Name)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("merchant_title_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = viralTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = merchantNumberDraft,
                        onValueChange = { merchantNumberDraft = it },
                        label = { Text("Easypaisa Number (03XXXXXXXXX)", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("merchant_number_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = viralTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onSaveMerchantConfig(merchantTitleDraft, merchantNumberDraft) {
                                showMerchantEditor = false
                            }
                        },
                        enabled = !uiState.isSavingMerchantConfig,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_merchant_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color(0xFF0A0F16)
                        )
                    ) {
                        if (uiState.isSavingMerchantConfig) {
                            CircularProgressIndicator(
                                color = Color(0xFF0A0F16),
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Save Easypaisa Receiving Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Error banner
            if (!uiState.errorMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DangerRed.copy(alpha = 0.14f))
                        .border(1.dp, DangerRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = uiState.errorMessage,
                        color = DangerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ADMIN REVIEW QUEUE (Visible strictly to verified admin)
            if (isAdmin) {
                AdminQueueSection(
                    adminQueueState = adminQueueState,
                    moderatingSubmissionId = uiState.moderatingSubmissionId,
                    onApprove = onApproveSubmission,
                    onReject = onRejectSubmission
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // STEP 1 OR STEP 2
            if (uiState.step == 1 || !uiState.isDepositVerified) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(SurfaceCard)
                        .border(1.dp, LineBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Step 1: Enter 100 PKR Deposit",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(GoldSoft)
                                .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(100.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "100 PKR",
                                color = GoldPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Send 100 PKR to the Easypaisa account above, enter your sender number and Transaction ID (TID), then submit your video & link for admin verification.",
                        color = TextDim,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = uiState.senderPhone,
                        onValueChange = { v -> onUpdateState { it.copy(senderPhone = v) } },
                        label = { Text("Your Easypaisa Number (03XXXXXXXXX)", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("viral_sender_phone_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = viralTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = uiState.transactionId,
                        onValueChange = { v -> onUpdateState { it.copy(transactionId = v) } },
                        label = { Text("Easypaisa Transaction ID (TID / Trx ID)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("viral_tid_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = viralTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            receiptPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("viral_pick_receipt_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LineBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Attach Easypaisa receipt",
                            tint = if (uiState.receiptUri != null) AccentGreen else TextDim,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.receiptUri != null) {
                                "Receipt Screenshot Attached"
                            } else {
                                "Attach Receipt Screenshot (Optional)"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onVerifyDeposit,
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("viral_verify_deposit_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color(0xFF0A0F16)
                        )
                    ) {
                        Text(
                            text = "Continue to Video & Link",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // STEP 2: UPLOAD VIDEO + REFERRAL LINK
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(SurfaceCard)
                        .border(1.dp, LineBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "100 PKR TID ${uiState.transactionId.trim().uppercase()}",
                                color = AccentGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = onBackToDepositStep) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit deposit",
                                tint = TextDim,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", color = TextDim, fontSize = 11.sp)
                        }
                    }

                    Text(
                        text = "Step 2: Add Promo Video & Referral Link",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Video Picker Button
                    Button(
                        onClick = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("viral_pick_video_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.pickedVideoUri != null) {
                                AccentGreen.copy(alpha = 0.2f)
                            } else PanelDark,
                            contentColor = TextPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (uiState.pickedVideoUri != null) {
                                Icons.Default.PlayCircleOutline
                            } else Icons.Default.VideoLibrary,
                            contentDescription = "Select promo video",
                            tint = if (uiState.pickedVideoUri != null) AccentGreen else GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.pickedVideoUri != null) {
                                "Local Promo Video Selected"
                            } else {
                                "Pick Promo Video from Gallery"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (uiState.pickedVideoUri != null) AccentGreen else TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Paste an HTTPS video URL so all users can stream it; gallery videos preview locally on your device.",
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = uiState.externalVideoUrl,
                        onValueChange = { v -> onUpdateState { it.copy(externalVideoUrl = v) } },
                        label = { Text("HTTPS Video URL (For Community Streaming)", fontSize = 12.sp) },
                        placeholder = { Text("https://.../promo.mp4", color = TextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("viral_video_url_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = viralTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = { v -> onUpdateState { it.copy(title = v) } },
                        label = { Text("Offer / App Name *", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("viral_title_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = viralTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = uiState.referralLink,
                        onValueChange = { v -> onUpdateState { it.copy(referralLink = v) } },
                        label = { Text("Your Referral Link (https://...) *", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("viral_link_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = viralTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.bonusOffer,
                            onValueChange = { v -> onUpdateState { it.copy(bonusOffer = v) } },
                            label = { Text("Bonus Text", fontSize = 12.sp) },
                            placeholder = { Text("e.g. 500 PKR Bonus", color = TextMuted, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = viralTextFieldColors()
                        )
                        OutlinedTextField(
                            value = uiState.promoCode,
                            onValueChange = { v -> onUpdateState { it.copy(promoCode = v) } },
                            label = { Text("Promo Code", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = viralTextFieldColors()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { v -> onUpdateState { it.copy(description = v) } },
                        label = { Text("Short Viral Hook / Description", fontSize = 12.sp) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = viralTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onPublishViralReel,
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("viral_publish_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color(0xFF0A0F16)
                        )
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                color = Color(0xFF0A0F16),
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Submit for Admin Approval",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // My Submissions List
            Spacer(modifier = Modifier.height(20.dp))
            MySubmissionsSection(
                mySubmissionsState = mySubmissionsState,
                onJumpToSubmission = onJumpToSubmission,
                onDeleteSubmission = onDeleteSubmission
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AdminQueueSection(
    adminQueueState: UiState<List<AdminModerationItem>>,
    moderatingSubmissionId: String?,
    onApprove: (String) -> Unit,
    onReject: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PanelDark)
            .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .padding(14.dp)
            .testTag("admin_queue_section")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ADMIN DEPOSIT APPROVAL QUEUE",
                color = GoldPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (adminQueueState) {
            is UiState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = GoldPrimary,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Loading pending deposits...", color = TextDim, fontSize = 12.sp)
                }
            }
            is UiState.Error -> {
                Text(
                    text = adminQueueState.message,
                    color = DangerRed,
                    fontSize = 12.sp
                )
            }
            is UiState.Success -> {
                val queue = adminQueueState.data
                if (queue.isEmpty()) {
                    Text(
                        text = "No pending 100 PKR deposits right now.",
                        color = TextDim,
                        fontSize = 12.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        queue.forEach { item ->
                            val sub = item.submission
                            val proof = item.depositProof
                            val isWorking = moderatingSubmissionId == sub.id
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceCard)
                                    .border(1.dp, LineBorder, RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "${sub.title} · by ${sub.authorName}",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = sub.referralLink,
                                    color = TextDim,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (proof != null) {
                                        "Sender: ${proof.easypaisaSenderPhone} · TID: ${proof.easypaisaTid} · ${proof.amountPkr} PKR"
                                    } else {
                                        "Amount: ${sub.amountPkr} PKR (Proof loading...)"
                                    },
                                    color = GoldPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onReject(sub.id, "Deposit TID could not be confirmed") },
                                        enabled = !isWorking,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("admin_reject_${sub.id}"),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.6f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                                    ) {
                                        Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onApprove(sub.id) },
                                        enabled = !isWorking,
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .height(44.dp)
                                            .testTag("admin_approve_${sub.id}"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AccentGreen,
                                            contentColor = Color(0xFF04140D)
                                        )
                                    ) {
                                        if (isWorking) {
                                            CircularProgressIndicator(
                                                color = Color(0xFF04140D),
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = "Approve & Go Live",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MySubmissionsSection(
    mySubmissionsState: UiState<List<ViralSubmissionDoc>>,
    onJumpToSubmission: (ViralSubmissionDoc) -> Unit,
    onDeleteSubmission: (ViralSubmissionDoc) -> Unit
) {
    when (mySubmissionsState) {
        is UiState.Loading -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = GoldPrimary,
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Loading your submissions...", color = TextDim, fontSize = 12.sp)
            }
        }
        is UiState.Error -> {
            Text(
                text = mySubmissionsState.message,
                color = DangerRed,
                fontSize = 12.sp
            )
        }
        is UiState.Success -> {
            val submissions = mySubmissionsState.data
            if (submissions.isNotEmpty()) {
                Text(
                    text = "MY VIRAL SUBMISSIONS (${submissions.size})",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                submissions.forEach { item ->
                    val status = item.parsedStatus
                    val (statusLabel, statusColor) = when (status) {
                        SubmissionStatus.APPROVED -> "Live · ${item.viralShares} shares" to AccentGreen
                        SubmissionStatus.REJECTED -> "Rejected" to DangerRed
                        SubmissionStatus.PENDING_VERIFICATION -> "Pending Admin Approval" to GoldPrimary
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .border(1.dp, LineBorder, RoundedCornerShape(14.dp))
                            .clickable { onJumpToSubmission(item) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(statusColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (status) {
                                    SubmissionStatus.APPROVED -> Icons.Default.PlayCircleOutline
                                    SubmissionStatus.REJECTED -> Icons.Default.Close
                                    SubmissionStatus.PENDING_VERIFICATION -> Icons.Default.HourglassTop
                                },
                                contentDescription = statusLabel,
                                tint = statusColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = statusLabel,
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (status == SubmissionStatus.REJECTED && item.adminNote.isNotBlank()) {
                                Text(
                                    text = item.adminNote,
                                    color = DangerRed,
                                    fontSize = 10.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    text = item.referralLink,
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = { onDeleteSubmission(item) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("delete_submission_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete submission",
                                tint = TextDim,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepPill(
    stepNumber: Int,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean,
    isLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isActive -> GoldSoft
        isCompleted -> AccentGreen.copy(alpha = 0.14f)
        else -> SurfaceCard
    }
    val borderColor = when {
        isActive -> GoldPrimary
        isCompleted -> AccentGreen
        else -> LineBorder
    }
    val textColor = when {
        isActive -> GoldPrimary
        isCompleted -> AccentGreen
        else -> TextDim
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
        } else if (isLocked) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = "$stepNumber. $label",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun viralTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = PanelDark,
    unfocusedContainerColor = PanelDark,
    focusedBorderColor = GoldPrimary,
    unfocusedBorderColor = LineBorder,
    focusedLabelColor = GoldPrimary,
    unfocusedLabelColor = TextDim,
    cursorColor = GoldPrimary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
