package com.simbadev.streamchatappcompose.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simbadev.streamchatappcompose.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.models.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(

    private val client: ChatClient

) : ViewModel() {

    private val _loginEvent = MutableSharedFlow<LoginEvent>()
    val loginEvent = _loginEvent.asSharedFlow()

    private fun isValidateUsername(username: String): Boolean {
        return username.length > Constants.MIN_USERNAME_LENGTH
    }

    fun loginUser(username: String, token: String? = null) {
        val trimmedUsername = username.trim()
        viewModelScope.launch {
            if (isValidateUsername(trimmedUsername) && token != null) {
                loginRegisteresUser(trimmedUsername, token)
            } else if (isValidateUsername(trimmedUsername) && token == null) {
                loginGuestUser(trimmedUsername)
            } else {
                _loginEvent.emit(LoginEvent.ErrorInputTooShort)
            }
        }
    }

    private fun loginRegisteresUser(username: String, token: String) {
        val user = User(id = username, name = username)

        client.connectUser(
            user = user,
            token = token
        ).enqueue() { result ->
            if (result.isSuccess) {
                viewModelScope.launch {
                    _loginEvent.emit(LoginEvent.Success)
                }
            } else {
                viewModelScope.launch {
                    _loginEvent.emit(
                        LoginEvent.ErrorLogin(
                            result.error().message ?: "Unknown Error"
                        )
                    )
                }
            }
        }
    }

    private fun loginGuestUser(username: String) {
        client.connectGuestUser(userId = username,
            username = username).enqueue(){ result->

            if (result.isSuccess) {
                viewModelScope.launch {
                    _loginEvent.emit(LoginEvent.Success)
                }
            } else {
                viewModelScope.launch {
                    _loginEvent.emit(
                        LoginEvent.ErrorLogin(
                            result.error().message ?: "Unknown Error"
                        )
                    )
                }
            }
        }
    }


    sealed class LoginEvent {
        object ErrorInputTooShort : LoginEvent()
        data class ErrorLogin(val error: String) : LoginEvent()
        object Success : LoginEvent()
    }
}