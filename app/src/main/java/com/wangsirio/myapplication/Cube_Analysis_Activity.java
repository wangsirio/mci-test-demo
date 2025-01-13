package com.wangsirio.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Cube_Analysis_Activity extends AppCompatActivity {
    private ImageView imageViewCube;
    private EditText editTextAnswer;
    private Button buttonNext;
    private TextView textViewProgress;
    private int currentImageIndex = 1;
    private static final int TOTAL_IMAGES = 10;
    private int correctAnswers = 0;
    private UserData userData;
    private static final int[] CORRECT_ANSWERS = {2, 3, 4, 8, 10, 14, 10, 8, 13, 5};
    private static final boolean DEBUG_MODE = true;  // 调试模式开关

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cube_analysis);

        // 获取传递过来的UserData
        userData = getIntent().getParcelableExtra("userData");
        if (userData == null) {
            userData = new UserData("未知", "未知", 0);
        }

        // 初始化视图
        imageViewCube = findViewById(R.id.imageView_cube);
        editTextAnswer = findViewById(R.id.editText_answer);
        buttonNext = findViewById(R.id.button_next);
        textViewProgress = findViewById(R.id.textView_progress);

        // 显示第一张图片
        showCurrentImage();

        // 如果是调试模式，自动填入正确答案
        if (DEBUG_MODE) {
            editTextAnswer.setText(String.valueOf(CORRECT_ANSWERS[currentImageIndex - 1]));
        }

        // 设置按钮点击事件
        buttonNext.setOnClickListener(v -> {
            String answer = editTextAnswer.getText().toString();
            if (answer.isEmpty()) {
                Toast.makeText(this, "请输入答案", Toast.LENGTH_SHORT).show();
                return;
            }

            // 检查答案是否正确
            int userAnswer = Integer.parseInt(answer);
            if (userAnswer == CORRECT_ANSWERS[currentImageIndex - 1]) {
                correctAnswers++;
            }

            if (currentImageIndex < TOTAL_IMAGES) {
                currentImageIndex++;
                showCurrentImage();
                editTextAnswer.setText(""); // 清空输入框
                if (DEBUG_MODE) {
                    editTextAnswer.setText(String.valueOf(CORRECT_ANSWERS[currentImageIndex - 1]));
                }
            } else {
                // 所有题目完成，保存结果并发送数据
                userData.setCubeResult(correctAnswers);
                new SendDataTask().execute(userData);
            }
        });
    }

    private void showCurrentImage() {
        // 更新进度文本
        textViewProgress.setText(String.format("第%d题/共%d题", currentImageIndex, TOTAL_IMAGES));
        
        // 根据当前索引加载对应的图片
        String imageName = "cube" + currentImageIndex;
        int resourceId = getResources().getIdentifier(imageName, "drawable", getPackageName());
        imageViewCube.setImageResource(resourceId);
        
        // 更新按钮文本
        if (currentImageIndex == TOTAL_IMAGES) {
            buttonNext.setText(R.string.finish);
        } else {
            buttonNext.setText(R.string.next_question);
        }
    }

    private class SendDataTask extends AsyncTask<UserData, Void, String> {
        @Override
        protected String doInBackground(UserData... userData) {
            HttpURLConnection conn = null;
            try {
                String serverUrl = getString(R.string.server_url);
                Log.d("NetworkRequest", "Connecting to: " + serverUrl);
                
                URL url = new URL(serverUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(1500);
                conn.setReadTimeout(1500);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setUseCaches(false);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("gender", userData[0].getGender());
                jsonObject.put("education", userData[0].getEducation());
                jsonObject.put("age", userData[0].getAge());
                jsonObject.put("cubeResult", userData[0].getCubeResult());
                
                String jsonString = jsonObject.toString();
                Log.d("NetworkRequest", "Sending data: " + jsonString);

                Log.d("NetworkRequest", "Connecting...");
                conn.connect();
                Log.d("NetworkRequest", "Connected");

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonString.getBytes("UTF-8");
                    os.write(input, 0, input.length);
                    os.flush();
                    Log.d("NetworkRequest", "Data written to output stream");
                }

                int responseCode = conn.getResponseCode();
                Log.d("NetworkRequest", "Response code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        Log.d("NetworkRequest", "Response: " + response.toString());
                    }
                    return "success";
                } else {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream(), "UTF-8"))) {
                        StringBuilder error = new StringBuilder();
                        String errorLine;
                        while ((errorLine = br.readLine()) != null) {
                            error.append(errorLine.trim());
                        }
                        Log.e("NetworkRequest", "Error response: " + error.toString());
                        return "服务器返回错误代码: " + responseCode + ", " + error.toString();
                    }
                }

            } catch (java.net.ConnectException e) {
                Log.e("NetworkRequest", "Connection error: " + e.getMessage(), e);
                return "连接服务器失败，请检查服务器地址是否正确: " + e.getMessage();
            } catch (java.net.SocketTimeoutException e) {
                Log.e("NetworkRequest", "Timeout error: " + e.getMessage(), e);
                return "连接服务器超时: " + e.getMessage();
            } catch (Exception e) {
                Log.e("NetworkRequest", "Error: " + e.getMessage(), e);
                e.printStackTrace();
                return "错误: " + e.getMessage();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if ("success".equals(result)) {
                Toast.makeText(Cube_Analysis_Activity.this, "数据发送成功,应用自动关闭，感谢您的配合", Toast.LENGTH_SHORT).show();
                finishAffinity();
            } else {
                Toast.makeText(Cube_Analysis_Activity.this, result, Toast.LENGTH_LONG).show();
            }
        }
    }
} 