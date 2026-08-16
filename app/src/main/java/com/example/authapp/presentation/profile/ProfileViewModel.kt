package com.example.authapp.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapp.data.remote.CloudinaryUploader
import com.example.authapp.domain.repository.AuthRepository
import com.example.authapp.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cloudinaryUploader: CloudinaryUploader
) : ViewModel() {


    private val _uiState =
        MutableStateFlow<ProfileUiState>(
            ProfileUiState.Loading
        )

    val uiState =
        _uiState.asStateFlow()


    private val _events =
        Channel<ProfileEvent>(
            Channel.BUFFERED
        )

    val events =
        _events.receiveAsFlow()


    private var currentUser:
            User? =
        null


    fun loadProfile() {

        val uid =
            authRepository
                .getCurrentUid()


        if (
            uid.isNullOrBlank()
        ) {

            _uiState.value =
                ProfileUiState.Error(
                    "Your session has expired. Please sign in again."
                )

            return
        }


        viewModelScope.launch {

            _uiState.value =
                ProfileUiState.Loading


            val result =
                authRepository
                    .getUserFromFirestore(
                        uid
                    )


            if (
                result.isSuccess
            ) {

                val user =
                    result.getOrThrow()


                currentUser =
                    user


                _uiState.value =
                    ProfileUiState.Success(
                        user = user
                    )

            } else {

                _uiState.value =
                    ProfileUiState.Error(
                        result
                            .exceptionOrNull()
                            ?.message
                            ?: "Unable to load your profile."
                    )
            }
        }
    }


    fun saveProfile(
        displayName: String,
        phone: String,
        imageBytes: ByteArray?
    ) {

        val cleanName =
            displayName.trim()


        val cleanPhone =
            phone.trim()


        if (
            cleanName.isBlank()
        ) {

            sendError(
                "Name cannot be empty."
            )

            return
        }


        val uid =
            authRepository
                .getCurrentUid()


        if (
            uid.isNullOrBlank()
        ) {

            sendError(
                "Your session has expired."
            )

            return
        }


        val user =
            currentUser


        if (
            user == null
        ) {

            sendError(
                "Profile is still loading."
            )

            return
        }


        val currentState =
            _uiState.value


        if (
            currentState is ProfileUiState.Success &&
            (
                    currentState.isSaving ||
                            currentState.isLoggingOut
                    )
        ) {

            return
        }


        _uiState.value =
            ProfileUiState.Success(
                user = user,
                isSaving = true
            )


        viewModelScope.launch {

            var imageUrl =
                user.profileImageUrl


            /*
             * Upload a new image only if
             * the user selected one.
             */
            if (
                imageBytes != null
            ) {

                val uploadResult =
                    cloudinaryUploader
                        .uploadImage(
                            imageBytes =
                                imageBytes,

                            folder =
                                "pawconnect/users/$uid"
                        )


                if (
                    uploadResult.isFailure
                ) {

                    _uiState.value =
                        ProfileUiState.Success(
                            user = user
                        )


                    _events.send(
                        ProfileEvent.Error(
                            uploadResult
                                .exceptionOrNull()
                                ?.message
                                ?: "Profile image upload failed."
                        )
                    )


                    return@launch
                }


                imageUrl =
                    uploadResult
                        .getOrThrow()
            }


            val updateResult =
                authRepository
                    .updateUserProfile(
                        uid =
                            uid,

                        displayName =
                            cleanName,

                        phone =
                            cleanPhone,

                        profileImageUrl =
                            imageUrl
                    )


            if (
                updateResult.isFailure
            ) {

                _uiState.value =
                    ProfileUiState.Success(
                        user = user
                    )


                _events.send(
                    ProfileEvent.Error(
                        updateResult
                            .exceptionOrNull()
                            ?.message
                            ?: "Unable to update your profile."
                    )
                )


                return@launch
            }


            val updatedUser =
                user.copy(
                    displayName =
                        cleanName,

                    phoneNumber =
                        cleanPhone,

                    profileImageUrl =
                        imageUrl
                )


            currentUser =
                updatedUser


            _uiState.value =
                ProfileUiState.Success(
                    user =
                        updatedUser
                )


            _events.send(
                ProfileEvent.ProfileSaved
            )
        }
    }


    fun logout() {

        val user =
            currentUser


        if (
            user == null
        ) {

            return
        }


        val currentState =
            _uiState.value


        if (
            currentState is ProfileUiState.Success &&
            (
                    currentState.isSaving ||
                            currentState.isLoggingOut
                    )
        ) {

            return
        }


        _uiState.value =
            ProfileUiState.Success(
                user =
                    user,

                isLoggingOut =
                    true
            )


        viewModelScope.launch {

            val result =
                authRepository
                    .logout()


            if (
                result.isSuccess
            ) {

                _events.send(
                    ProfileEvent.LoggedOut
                )

            } else {

                _uiState.value =
                    ProfileUiState.Success(
                        user =
                            user
                    )


                _events.send(
                    ProfileEvent.Error(
                        result
                            .exceptionOrNull()
                            ?.message
                            ?: "Unable to sign out."
                    )
                )
            }
        }
    }


    private fun sendError(
        message: String
    ) {

        viewModelScope.launch {

            _events.send(
                ProfileEvent.Error(
                    message
                )
            )
        }
    }
}


sealed class ProfileUiState {

    object Loading :
        ProfileUiState()


    data class Success(
        val user: User,
        val isSaving: Boolean = false,
        val isLoggingOut: Boolean = false
    ) : ProfileUiState()


    data class Error(
        val message: String
    ) : ProfileUiState()
}


sealed class ProfileEvent {

    object ProfileSaved :
        ProfileEvent()


    object LoggedOut :
        ProfileEvent()


    data class Error(
        val message: String
    ) : ProfileEvent()
}