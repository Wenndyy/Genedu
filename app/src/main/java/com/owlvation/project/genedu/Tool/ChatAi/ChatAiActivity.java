package com.owlvation.project.genedu.Tool.ChatAi;

import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.owlvation.project.genedu.Network.NetworkChangeReceiver;
import com.owlvation.project.genedu.R;

import java.util.ArrayList;
import java.util.List;

public class ChatAiActivity extends AppCompatActivity {
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private EditText inputMessage;
    private ImageButton btnSend;
    private ImageView icBack;
    private RelativeLayout layout_bottom;

    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_ai);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        inputMessage = findViewById(R.id.inputMessage);
        btnSend = findViewById(R.id.send_button);
        layout_bottom = findViewById(R.id.bottom_layout);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        networkChangeReceiver = new NetworkChangeReceiver();


        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
        icBack = findViewById(R.id.ic_back);
        icBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSend.setOnClickListener(v -> {
            String userInput = inputMessage.getText().toString().trim();
            if (!userInput.isEmpty()) {
                addChatMessage(userInput, true);
                inputMessage.setText("");
                hideKeyboard();
                getResultFromAI(userInput);
            } else {
                Toast.makeText(this, R.string.error_input_message, Toast.LENGTH_SHORT).show();
            }
        });

        ImageView icClear = findViewById(R.id.ic_clear);
        icClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearChatMessages();
            }
        });

        int currentMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentMode == Configuration.UI_MODE_NIGHT_YES) {
            layout_bottom.setBackgroundColor(getResources().getColor(android.R.color.system_background_dark));
        } else {
            layout_bottom.setBackgroundColor(getResources().getColor(android.R.color.background_light));
        }
    }

    private void addChatMessage(String message, boolean isUser) {
        chatMessages.add(new ChatMessage(message, isUser));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }


    private void getResultFromAI(String inputText) {
        String apiKey = getString(R.string.apiKey);
        GenerativeModel generativeModel = new GenerativeModel("gemini-1.5-flash", apiKey);
        GenerativeModelFutures modelFutures = GenerativeModelFutures.from(generativeModel);

        ChatMessage typingMessage = new ChatMessage(true);
        chatMessages.add(typingMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        Content content = new Content.Builder()
                .addText(inputText)
                .build();

        ListenableFuture<GenerateContentResponse> responseFuture = modelFutures.generateContent(content);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Futures.addCallback(responseFuture, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    chatMessages.remove(typingMessage);
                    chatAdapter.notifyItemRemoved(chatMessages.size());
                    String aiResponse = result.getText().replace("*", "");
                    Log.d("AI_API_Response", "Response: " + aiResponse);
                    if (!aiResponse.isEmpty()) {
                        addChatMessage(aiResponse, false);
                    }
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    chatMessages.remove(typingMessage);
                    chatAdapter.notifyItemRemoved(chatMessages.size());

                    addChatMessage("Error: " + t.getMessage(), false);
                    addChatMessage("Error: " + t.getMessage(), false);
                }
            }, getMainExecutor());
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(inputMessage.getWindowToken(), 0);
        }
    }
    private void clearChatMessages() {
        chatMessages.clear();
        chatAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkChangeReceiver);
    }

}