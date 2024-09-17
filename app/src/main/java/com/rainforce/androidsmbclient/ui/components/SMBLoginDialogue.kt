package com.rainforce.androidsmbclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SMBLoginDialog(
    serverURL: String,
    userName: String,
    password: String,
    onDismiss: () -> Unit,
    onConfirm: (smbServerUrl: String, userName: String, password: String) -> Unit,
) {
    var enteredServerUrl by remember { mutableStateOf(serverURL) }
    var enteredUserName by remember { mutableStateOf(userName) }
    var enteredPassword by remember { mutableStateOf(password) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val isConfirmEnabled =
        enteredServerUrl.isNotEmpty() && enteredUserName.isNotEmpty() && enteredPassword.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(enteredServerUrl, enteredUserName, enteredPassword) },
                enabled = isConfirmEnabled
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Login to Samba Server", style = TextStyle(fontSize = 14.sp))
                Text(text = "* use port number i.e.445 if connecting to internet samba server", style = TextStyle(fontSize = 10.sp))
            }
        },
        text = {
            Column {
                TextField(
                    value = enteredServerUrl,
                    onValueChange = { enteredServerUrl = it },
                    placeholder = {
                        Text(
                            text = "Samba Server Url (starts with smb://)",
                            style = TextStyle(fontSize = 10.sp)
                        )
                    },
                    textStyle = TextStyle(fontSize = 10.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(color = Color.Transparent, shape = RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = enteredUserName,
                    onValueChange = { enteredUserName = it },
                    placeholder = { Text(text = "Username", style = TextStyle(fontSize = 10.sp)) },
                    textStyle = TextStyle(fontSize = 10.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(color = Color.Transparent, shape = RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = enteredPassword,
                    onValueChange = { enteredPassword = it },
                    placeholder = { Text(text = "Password", style = TextStyle(fontSize = 10.sp)) },
                    textStyle = TextStyle(fontSize = 10.sp),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        val image =
                            if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = "Toggle Password Visibility"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(color = Color.Transparent, shape = RoundedCornerShape(8.dp))
                        .border(width = 1.dp, color = Color.Gray)
                )
            }
        }
    )
}
