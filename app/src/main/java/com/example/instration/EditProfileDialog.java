package com.example.instration;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.instration.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

public class EditProfileDialog extends DialogFragment {

    private EditText displayNameEditText;
    private EditText emailEditText;
    private ImageView photoImageView;
    private Button saveButton;
    private Button changePhotoButton;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.edit_profile_dialog, container, false);

        displayNameEditText = view.findViewById(R.id.displayNameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        photoImageView = view.findViewById(R.id.photoImageView);
        saveButton = view.findViewById(R.id.saveButton);
        changePhotoButton = view.findViewById(R.id.changePhotoButton);

        // Configurar los campos de edición de perfil con los valores actuales del usuario
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        displayNameEditText.setText(user.getDisplayName());
        emailEditText.setText(user.getEmail());
        Glide.with(this).load(user.getPhotoUrl()).into(photoImageView);

        // Configurar el botón de guardar
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Obtener los nuevos valores de nombre y correo electrónico
                String displayName = displayNameEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();

                // Verificar si los campos de nombre y correo electrónico están vacíos
                if (displayName.isEmpty()) {
                    displayNameEditText.setError("Display name is required");
                    return;
                }

                if (email.isEmpty()) {
                    emailEditText.setError("Email is required");
                    return;
                }

                // Actualizar los datos del usuario en Firebase Authentication
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                user.updateEmail(email)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    // Actualización exitosa
                                    user.updateProfile(new UserProfileChangeRequest.Builder()
                                                    .setDisplayName(displayName)
                                                    .setPhotoUri(imageUri) // Actualizar la foto de perfil
                                                    .build())
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        dismiss(); // Cerrar el diálogo
                                                    } else {
                                                        // Error al actualizar la foto de perfil
                                                        Toast.makeText(getActivity(), "Failed to update profile photo. Please try again.", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                } else {
                                    // Error al actualizar el correo electrónico
                                    Toast.makeText(getActivity(), "Failed to update email. Please try again.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        // Configurar el botón para cambiar la foto de perfil
        changePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageChooser();
            }
        });

        return view;
    }

    // Método para abrir el selector de imágenes
    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).into(photoImageView);
        }
    }
}
