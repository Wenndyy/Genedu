package com.owlvation.project.genedu.Note;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.owlvation.project.genedu.R;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class CreateNoteActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int REQUEST_CAPTURE_IMAGE = 1;
    EditText mCreateTitleNote, mCreateDescNote;
    ImageView mImageViewNote, icBack;
    ImageButton mCapturePhotoNote, mPickPhotoNote;
    FloatingActionButton mSaveNote;
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    FirebaseFirestore firebaseFirestore;
    StorageReference storageReference;
    Uri selectedImageUri;
    String noteId;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);

        mSaveNote = findViewById(R.id.saveFabNote);
        mCreateDescNote = findViewById(R.id.etDescCreateNote);
        mCreateTitleNote = findViewById(R.id.etTitleCreateNote);
        mImageViewNote = findViewById(R.id.imageViewNote);
        mCapturePhotoNote = findViewById(R.id.capturePhotoNote);
        mPickPhotoNote = findViewById(R.id.pickPhotoNote);
        icBack = findViewById(R.id.ic_back);
        icBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                startActivity(new Intent(CreateNoteActivity.this, NoteActivity.class));
                finish();
            }
        });

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        storageReference = FirebaseStorage.getInstance().getReference();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.please_wait));
        progressDialog.setCancelable(false);

        mPickPhotoNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickPhotoIntent.setType("image/*");
                startActivityForResult(pickPhotoIntent, REQUEST_PICK_IMAGE);
            }
        });

        mCapturePhotoNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent capturePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(capturePhotoIntent, REQUEST_CAPTURE_IMAGE);
            }
        });

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("noteId")) {
            noteId = intent.getStringExtra("noteId");
            mCreateTitleNote.setText(intent.getStringExtra("title"));
            mCreateDescNote.setText(intent.getStringExtra("content"));
            String imageUrl = intent.getStringExtra("imageUrl");
            if (!TextUtils.isEmpty(imageUrl)) {
                Picasso.get().load(imageUrl).into(mImageViewNote);
                mImageViewNote.setVisibility(View.VISIBLE);
            }
        }


        mSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = mCreateTitleNote.getText().toString().trim();
                String content = mCreateDescNote.getText().toString().trim();

                if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
                    Toast.makeText(CreateNoteActivity.this, getString(R.string.please_fill_in_all_fields), Toast.LENGTH_SHORT).show();
                } else {
                    if (noteId != null) {
                        updateNoteInFirestore(title, content);
                    } else {
                        if (selectedImageUri != null) {
                            progressDialog.show();
                            uploadImage(title, content);
                        } else {
                            progressDialog.show();
                            saveNoteWithoutImage(title, content, null);
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            mImageViewNote.setImageURI(selectedImageUri);
            mImageViewNote.setVisibility(View.VISIBLE);
        } else if (requestCode == REQUEST_CAPTURE_IMAGE && resultCode == RESULT_OK && data != null) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            mImageViewNote.setImageBitmap(photo);
            mImageViewNote.setVisibility(View.VISIBLE);
            selectedImageUri = saveImageToExternalStorage(photo);
        }
    }

    private Uri saveImageToExternalStorage(Bitmap bitmap) {
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "NoteImage", null);
        return Uri.parse(path);
    }

    private void uploadImage(String title, String content) {
        StorageReference imageRef = storageReference.child("images/" + firebaseUser.getUid() + "/" + System.currentTimeMillis() + ".jpg");
        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String imageUrl = uri.toString();
                                saveNoteWithoutImage(title, content, imageUrl);
                                progressDialog.dismiss();
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(CreateNoteActivity.this, getString(R.string.failed_to_upload_image) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveNoteWithoutImage(String title, String content, String imageUrl) {
        DocumentReference documentReference = firebaseFirestore.collection("notes").document(firebaseUser.getUid()).collection("myNotes").document();
        Map<String, Object> note = new HashMap<>();
        note.put("title", title);
        note.put("content", content);
        if (imageUrl != null) {
            note.put("imageUrl", imageUrl);
        }
        note.put("timestamp", FieldValue.serverTimestamp());

        documentReference.set(note).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                progressDialog.dismiss();
                Toast.makeText(CreateNoteActivity.this, getString(R.string.note_created_successfully), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(CreateNoteActivity.this, NoteActivity.class));
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(CreateNoteActivity.this, getString(R.string.failed_to_create_note) + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateNoteInFirestore(String title, String content) {
        if (selectedImageUri != null) {
            progressDialog.show();
            uploadImageAndUpdateNote(title, content);
        } else {
            progressDialog.show();
            updateNoteWithoutImage(title, content);
        }
    }

    private void uploadImageAndUpdateNote(String title, String content) {
        StorageReference imageRef = storageReference.child("images/" + firebaseUser.getUid() + "/" + System.currentTimeMillis() + ".jpg");
        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String imageUrl = uri.toString();
                                updateNoteWithImage(title, content, imageUrl);
                                progressDialog.dismiss();
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(CreateNoteActivity.this, getString(R.string.failed_to_upload_image) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateNoteWithImage(String title, String content, String imageUrl) {
        firebaseFirestore.collection("notes").document(firebaseUser.getUid()).collection("myNotes")
                .document(noteId)
                .update("title", title, "content", content, "imageUrl", imageUrl)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressDialog.dismiss();
                        Toast.makeText(CreateNoteActivity.this, getString(R.string.note_updated_successfully), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(CreateNoteActivity.this, NoteActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(CreateNoteActivity.this, getString(R.string.failed_to_update_note) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateNoteWithoutImage(String title, String content) {
        firebaseFirestore.collection("notes").document(firebaseUser.getUid()).collection("myNotes")
                .document(noteId)
                .update("title", title, "content", content)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressDialog.dismiss();
                        Toast.makeText(CreateNoteActivity.this, getString(R.string.note_updated_successfully), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(CreateNoteActivity.this, NoteActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(CreateNoteActivity.this, getString(R.string.failed_to_update_note) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        progressDialog.show();
        startActivity(new Intent(CreateNoteActivity.this, NoteActivity.class));
        finish();
    }
}
