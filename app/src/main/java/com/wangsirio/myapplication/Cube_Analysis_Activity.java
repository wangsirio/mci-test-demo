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
import android.provider.Settings;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

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
    private static final String RESULTS_DIR = "mci_test_result";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    // 将SaveResult类移到这里
    private static class SaveResult {
        final boolean localSaveSuccess;
        final String serverResult;

        SaveResult(boolean localSaveSuccess, String serverResult) {
            this.localSaveSuccess = localSaveSuccess;
            this.serverResult = serverResult;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cube_analysis);

        userData = getIntent().getParcelableExtra("userData");
        if (userData == null) {
            userData = new UserData("未知", "未知", 0);
        }

        imageViewCube = findViewById(R.id.imageView_cube);
        editTextAnswer = findViewById(R.id.editText_answer);
        buttonNext = findViewById(R.id.button_next);
        textViewProgress = findViewById(R.id.textView_progress);

        showCurrentImage();

        if (DEBUG_MODE) {
            editTextAnswer.setText(String.valueOf(CORRECT_ANSWERS[currentImageIndex - 1]));
        }

        buttonNext.setOnClickListener(v -> {
            String answer = editTextAnswer.getText().toString();
            if (answer.isEmpty()) {
                Toast.makeText(this, "请输入答案", Toast.LENGTH_SHORT).show();
                return;
            }

            int userAnswer = Integer.parseInt(answer);
            if (userAnswer == CORRECT_ANSWERS[currentImageIndex - 1]) {
                correctAnswers++;
            }

            if (currentImageIndex < TOTAL_IMAGES) {
                currentImageIndex++;
                showCurrentImage();
                editTextAnswer.setText("");
                if (DEBUG_MODE) {
                    editTextAnswer.setText(String.valueOf(CORRECT_ANSWERS[currentImageIndex - 1]));
                }
            } else {
                userData.setCubeResult(correctAnswers);
                new SendDataTask().execute(userData);
            }
        });


        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11及以上
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                    Toast.makeText(this, "请授予所有文件访问权限", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "请在系统设置中授予所有文件访问权限", Toast.LENGTH_LONG).show();
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0-10
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "需要存储权限才能保存测试结果", Toast.LENGTH_LONG).show();
            }
        }
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

    private class SendDataTask extends AsyncTask<UserData, Void, SaveResult> {
        @Override
        protected SaveResult doInBackground(UserData... userData) {
            boolean localSaveSuccess = saveLocalData(userData[0]);

            HttpURLConnection conn = null;
            try {
                String serverUrl = getString(R.string.server_url);
                Log.d("NetworkRequest", "Connecting to: " + serverUrl);
                
                URL url = new URL(serverUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
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
                    byte[] input = jsonString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    os.flush();
                    Log.d("NetworkRequest", "Data written to output stream");
                }

                int responseCode = conn.getResponseCode();
                Log.d("NetworkRequest", "Response code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        Log.d("NetworkRequest", "Response: " + response);
                    }
                    return new SaveResult(localSaveSuccess, "success");
                } else {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        StringBuilder error = new StringBuilder();
                        String errorLine;
                        while ((errorLine = br.readLine()) != null) {
                            error.append(errorLine.trim());
                        }
                        Log.e("NetworkRequest", "Error response: " + error);
                        return new SaveResult(localSaveSuccess, "服务器返回错误代码: " + responseCode + ", " + error);
                    }
                }

            } catch (java.net.ConnectException e) {
                return new SaveResult(localSaveSuccess, "连接服务器失败，请检查服务器地址是否正确: " + e.getMessage());
            } catch (java.net.SocketTimeoutException e) {
                return new SaveResult(localSaveSuccess, "连接服务器超时: " + e.getMessage());
            } catch (Exception e) {
                return new SaveResult(localSaveSuccess, "错误: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        private boolean saveLocalData(UserData userData) {
            try {
                File baseDir = new File(Environment.getExternalStorageDirectory(), RESULTS_DIR);
                if (!baseDir.exists()) {
                    baseDir.mkdirs();
                }

                String fileName = String.format("%s_%d_%s.json",
                        userData.getGender(),
                        userData.getAge(),
                        userData.getEducation().replace("/", "_"));

                File file = new File(baseDir, fileName);

                JSONObject jsonData = new JSONObject();
                jsonData.put("gender", userData.getGender());
                jsonData.put("education", userData.getEducation());
                jsonData.put("age", userData.getAge());
                jsonData.put("cubeResult", userData.getCubeResult());
                jsonData.put("timestamp", System.currentTimeMillis());

                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(jsonData.toString(4));
                }

                Log.d("LocalStorage", "数据已保存到: " + file.getAbsolutePath());
                return true;
            } catch (Exception e) {
                Log.e("LocalStorage", "本地保存数据失败: " + e.getMessage(), e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(SaveResult result) {
            if (!result.localSaveSuccess) {
                Toast.makeText(Cube_Analysis_Activity.this, 
                    "本地数据保存失败", Toast.LENGTH_SHORT).show();
            }

            if ("success".equals(result.serverResult)) {
                Toast.makeText(Cube_Analysis_Activity.this, 
                    "数据发送成功，应用自动关闭，感谢您的配合", Toast.LENGTH_SHORT).show();
                finishAffinity();
            } else {
                Toast.makeText(Cube_Analysis_Activity.this, result.serverResult, Toast.LENGTH_LONG).show();
            }
        }
    }
} 