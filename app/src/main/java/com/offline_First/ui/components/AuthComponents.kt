package com.offline_First.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaTextPrimary
import com.offline_First.ui.theme.EduNovaTextSecondary

private val Ink = EduNovaTextPrimary

@Composable
fun AuthHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(EduNovaPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text("E", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
        }
        Text("EduNova", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(title, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = EduNovaTextSecondary, fontSize = 15.sp)
    }
}

@Composable
fun AuthTextField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    imeAction: ImeAction = ImeAction.Next,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        keyboardOptions = keyboardOptions.copy(imeAction = imeAction),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun AuthPasswordField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    imeAction: ImeAction,
    modifier: Modifier = Modifier
) {
    var visible by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = { visible = !visible }) {
                Text(if (visible) "Hide" else "Show")
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = imeAction),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun PrimaryAuthButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .semantics { contentDescription = text },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}