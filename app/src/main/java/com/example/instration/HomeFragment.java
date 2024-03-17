package com.example.instration;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class HomeFragment extends Fragment {
    NavController navController;
    public AppViewModel appViewModel;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        view.findViewById(R.id.gotoNewPostFragmentButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.fragment_new_post);
            }
        });

        // RecyclerView
        RecyclerView postsRecyclerView = view.findViewById(R.id.postsRecyclerView);
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        Query query = FirebaseFirestore.getInstance().collection("posts").orderBy("timeStamp", Query.Direction.DESCENDING).limit(50);

        FirestoreRecyclerOptions<Post> options = new FirestoreRecyclerOptions.Builder<Post>()
                .setQuery(query, Post.class)
                .setLifecycleOwner(this)
                .build();

        postsRecyclerView.setAdapter(new PostsAdapter(options));

        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
    }



    public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {
        private List<Comment> comments;

        public CommentsAdapter(List<Comment> comments) {
            this.comments = comments;
        }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comment_item, parent, false);
            return new CommentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            Comment comment = comments.get(position);
            holder.bind(comment);

        }

        @Override
        public int getItemCount() {
            return comments.size();
        }


        // Definir la clase ViewHolder para los comentarios
        public class CommentViewHolder extends RecyclerView.ViewHolder {
            TextView commentTextView;

            public CommentViewHolder(@NonNull View itemView) {
                super(itemView);
                commentTextView = itemView.findViewById(R.id.commentTextView);
            }

            public void bind(Comment comment) {
                commentTextView.setText(comment.getCommentText());
            }


        }
    }


    class PostsAdapter extends FirestoreRecyclerAdapter<Post, PostsAdapter.PostViewHolder> {


        public PostsAdapter(@NonNull FirestoreRecyclerOptions<Post> options) {super(options);}

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PostViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_post, parent, false));
        }

        @Override
        protected void onBindViewHolder(@NonNull PostViewHolder holder, int position, @NonNull final Post post) {


            if (post.authorPhotoUrl==null){
                holder.authorPhotoImageView.setImageResource(R.drawable.user);
            }else {
                Glide.with(getContext()).load(post.authorPhotoUrl).circleCrop().into(holder.authorPhotoImageView);
            }
            holder.authorTextView.setText(post.author);
            holder.contentTextView.setText(post.content);


            // Configurar el RecyclerView de comentarios
            CommentsAdapter commentsAdapter = new CommentsAdapter(post.getComments() != null ? post.getComments() : new ArrayList<>());
            holder.commentsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            holder.commentsRecyclerView.setAdapter(commentsAdapter);



            // Gestion de likes
            final String postKey = getSnapshots().getSnapshot(position).getId();
            final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if(post.likes.containsKey(uid))
                holder.likeImageView.setImageResource(R.drawable.like_on);
            else
                holder.likeImageView.setImageResource(R.drawable.like_off);
            holder.numLikesTextView.setText(String.valueOf(post.likes.size()));
            holder.likeImageView.setOnClickListener(view -> {
                FirebaseFirestore.getInstance().collection("posts")
                        .document(postKey)
                        .update("likes."+uid, post.likes.containsKey(uid) ?
                                FieldValue.delete() : true);
            });
            //Fecha
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(post.timeStamp);

            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm");
            String fecha = format.format(calendar.getTime());

            holder.dateTextView.setText(fecha);




            // Miniatura de media
            if (post.mediaUrl != null) {
                holder.mediaImageView.setVisibility(View.VISIBLE);
                if ("audio".equals(post.mediaType)) {
                    Glide.with(requireView()).load(R.drawable.audio).centerCrop().into(holder.mediaImageView);
                } else {
                    Glide.with(requireView()).load(post.mediaUrl).centerCrop().into(holder.mediaImageView);
                }
                holder.mediaImageView.setOnClickListener(view -> {
                    appViewModel.postSeleccionado.setValue(post);
                    navController.navigate(R.id.mediaFragment);
                });
            } else {
                holder.mediaImageView.setVisibility(View.GONE);
            }


            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null && post.uid.equals(currentUser.getUid())) {
                holder.deletePostImageView.setVisibility(View.VISIBLE);
            } else {
                holder.deletePostImageView.setVisibility(View.GONE);
            }

            holder.deletePostImageView.setOnClickListener(view -> {
                if (currentUser != null && post.uid.equals(currentUser.getUid())) {
                    // Eliminar archivo multimedia asociado al post desde Firebase Storage
                    if (post.mediaUrl != null) {
                        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(post.mediaUrl);
                        storageReference.delete().addOnSuccessListener(aVoid -> {
                            Log.d("HomeFragment", "Archivo multimedia eliminado correctamente");
                        }).addOnFailureListener(e -> {
                            Log.e("HomeFragment", "Error al eliminar archivo multimedia", e);
                        });
                    }
                    FirebaseFirestore.getInstance().collection("posts")
                            .document(postKey)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show();
                                notifyItemRemoved(position);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(requireContext(), "Error deleting post", Toast.LENGTH_SHORT).show();
                                Log.e("HomeFragment", "Error deleting post", e);
                            });
                } else {
                    Toast.makeText(requireContext(), "You can't delete this post", Toast.LENGTH_SHORT).show();
                }
            });




            holder.addCommentIView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                    builder.setTitle("Add Comment");

                    View viewInflated = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.add_comentario, (ViewGroup) v.getParent(), false);

                    final EditText input = viewInflated.findViewById(R.id.input);
                    builder.setView(viewInflated);


                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            String commentText = input.getText().toString().trim();


                            if (!TextUtils.isEmpty(commentText)) {

                                Comment comment = new Comment(commentText, FirebaseAuth.getInstance().getCurrentUser().getUid());


                                DocumentReference postRef = FirebaseFirestore.getInstance().collection("posts").document(postKey);

                                postRef.update("comments", FieldValue.arrayUnion(comment))
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(holder.itemView.getContext(), "Comment added successfully", Toast.LENGTH_SHORT).show();

                                                // Obtener la lista de comentarios actualizada después de agregar un nuevo comentario
                                                postRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                    @Override
                                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                        Post updatedPost = documentSnapshot.toObject(Post.class);
                                                        if (updatedPost != null && updatedPost.getComments() != null) {
                                                            // Crear un nuevo adaptador de comentarios con la lista actualizada de comentarios
                                                            CommentsAdapter updatedCommentsAdapter = new CommentsAdapter(updatedPost.getComments());
                                                            // Establecer el adaptador actualizado en el RecyclerView de comentarios
                                                            holder.commentsRecyclerView.setAdapter(updatedCommentsAdapter);
                                                        }
                                                    }
                                                });
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(holder.itemView.getContext(), "Failed to add comment. Please try again.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    });

                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel(); // Cancelar el cuadro de diálogo si el usuario presiona Cancelar
                        }
                    });

                    // Mostrar el cuadro de diálogo
                    builder.show();
                }
            });



            // Configurar el RecyclerView de comentarios
            if (post.getComments() != null && !post.getComments().isEmpty()) {
                Log.d("HomeFragment", "La lista de comentarios no está vacía");
                commentsAdapter = new CommentsAdapter(post.getComments());
                holder.commentsRecyclerView.setAdapter(commentsAdapter);
            } else {
                Log.d("HomeFragment", "La lista de comentarios está vacía");
            }

        }

        class PostViewHolder extends RecyclerView.ViewHolder {
            ImageView deletePostImageView;
            RecyclerView commentsRecyclerView;
            ImageView addCommentIView;
            ImageView authorPhotoImageView, likeImageView, mediaImageView;
            TextView authorTextView, contentTextView, numLikesTextView, dateTextView;

            PostViewHolder(@NonNull View itemView) {
                super(itemView);

                authorPhotoImageView = itemView.findViewById(R.id.authorPhotoImageView);
                likeImageView = itemView.findViewById(R.id.likeImageView);
                mediaImageView = itemView.findViewById(R.id.mediaImage);
                authorTextView = itemView.findViewById(R.id.authorTextView);
                contentTextView = itemView.findViewById(R.id.contentTextView);
                numLikesTextView = itemView.findViewById(R.id.numLikesTextView);
                dateTextView = itemView.findViewById(R.id.dateTextView);
                deletePostImageView = itemView.findViewById(R.id.deletePostImageView);
                addCommentIView = itemView.findViewById(R.id.addCommentIView);
                commentsRecyclerView = itemView.findViewById(R.id.commentsRecyclerView);

            }
        }
    }

}
