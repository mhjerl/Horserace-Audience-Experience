package com.othernewspaper.horseraceaudienceexperience;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.othernewspaper.horseraceaudienceexperience.R;
import com.othernewspaper.horseraceaudienceexperience.app.AppConfig;
import com.othernewspaper.horseraceaudienceexperience.app.AppController;
import com.othernewspaper.horseraceaudienceexperience.app.QuestionItem;
import com.othernewspaper.horseraceaudienceexperience.app.SQLiteHandler;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class QuestionsActivity extends AppCompatActivity {

    private static final String TAG = QuestionsActivity.class.getSimpleName();
    TextView tvTimer, tvQuestion, tvOptionA, tvOptionB, tvOptionC, tvOptionD;
    ArrayList<QuestionItem> questionsList;
    android.os.CountDownTimer count;
    private int currentQuestion = 0;
    private int score = 0;
    private SQLiteHandler db;
    private HashMap user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);
        db = new SQLiteHandler(QuestionsActivity.this);
        user = db.getUserDetails();
        tvTimer = findViewById(R.id.tv_timer);
        tvQuestion = findViewById(R.id.tv_question);
        tvOptionA = findViewById(R.id.tv_option_a);
        tvOptionB = findViewById(R.id.tv_option_b);
        tvOptionC = findViewById(R.id.tv_option_c);
        tvOptionD = findViewById(R.id.tv_option_d);
        tvOptionA.setOnClickListener(new MyClickListener());
        tvOptionB.setOnClickListener(new MyClickListener());
        tvOptionC.setOnClickListener(new MyClickListener());
        tvOptionD.setOnClickListener(new MyClickListener());
        questionsList = new ArrayList<>();
        loadQuestions();

    }

    private void loadQuestions() {
        // Tag used to cancel the request
        String tag_string_req = "req_get_questions";
        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_QUIZ, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    questionsList.clear();
                    // Check for error node in json
                    if (!error) {
                        JSONArray questions = jObj.getJSONArray("questions");
                        for (int i = 0; i < questions.length(); i++) {
                            JSONObject question = (JSONObject) questions.get(i);
                            QuestionItem questionItem = new QuestionItem();
                            questionItem.setId(Integer.parseInt(question.get("id").toString()));
                            questionItem.setQuestion(question.get("question").toString());
                            questionItem.setOption_a(question.get("option_a").toString());
                            questionItem.setOption_b(question.get("option_b").toString());
                            questionItem.setOption_c(question.get("option_c").toString());
                            questionItem.setOption_d(question.get("option_d").toString());
                            questionItem.setTime(question.getInt("question_time"));
                            questionItem.setAns(question.get("ans").toString());
                            questionsList.add(questionItem);
                        }
                        //Collections.shuffle(questionsList);
                        displayQuestion();

                    } else {
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                return params;
            }

        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);

    }

    private void displayQuestion() {
        int time = questionsList.get(currentQuestion).getTime();
        tvQuestion.setText(questionsList.get(currentQuestion).getQuestion());
        tvOptionA.setText(questionsList.get(currentQuestion).getOption_a());
        tvOptionB.setText(questionsList.get(currentQuestion).getOption_b());

        String option_c = questionsList.get(currentQuestion).getOption_c();
        String option_d = questionsList.get(currentQuestion).getOption_d();
        if(option_c.equals("")) {
            tvOptionC.setVisibility(View.GONE);
        } else {
            tvOptionC.setVisibility(View.VISIBLE);
            tvOptionC.setText(option_c);
        }

        if(option_d.equals("")) {
            tvOptionD.setVisibility(View.GONE);
        }  else {
            tvOptionD.setVisibility(View.VISIBLE);
            tvOptionD.setText(option_d);
        }


        count = new android.os.CountDownTimer(time * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                tvTimer.setText("" + millisUntilFinished / 1000);
            }
            public void onFinish() {
                displayNextQuestion();
            }
        };
        count.start();
    }

    private void validateAns(String ans, TextView tvSelected) {
        count.cancel();
        if (ans.equalsIgnoreCase(questionsList.get(currentQuestion).getAns().trim())) {
            //make background green
            tvSelected.setBackgroundColor(getResources().getColor(R.color.green));
            score++;
            updateScore(score);
        } else {
            //make background red
            tvSelected.setBackgroundColor(getResources().getColor(R.color.red));
        }

        tvSelected.setTextColor(Color.WHITE);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Do something after 3s = 3000ms
                tvSelected.setBackgroundColor(Color.WHITE);
                tvSelected.setTextColor(Color.parseColor("#000000"));
                displayNextQuestion();

            }
        }, 500);
    }

    private void updateScore(int score) {
        // Tag used to cancel the request
        String tag_string_req = "req_update_score";
        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_UPDATE_SCORE, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    // Check for error node in json
                    if (!error) {
                        Log.e(TAG, "result updated successfully..");

                    } else {
                        String errorMsg = jObj.getString("error_msg");
                        Log.e(TAG, errorMsg);
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("uid", user.get("uid").toString());
                float finalScore = score / (float) questionsList.size();
                params.put("score", Float.toString(finalScore));
                return params;
            }

        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);

    }

    private void displayNextQuestion() {
        currentQuestion++;
        if (currentQuestion < questionsList.size()) {
            displayQuestion();
        } else {
            //finish quiz...
            AlertDialog alertDialog;
            alertDialog = new AlertDialog.Builder(QuestionsActivity.this).create();
            alertDialog.setMessage("You completed the quiz...");
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Back", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    Intent i = new Intent(QuestionsActivity.this, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);

                }
            });
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.setCancelable(false);
            alertDialog.show();

        }

    }

    private class MyClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.tv_option_a:
                    validateAns("a", tvOptionA);
                    break;
                case R.id.tv_option_b:
                    validateAns("b", tvOptionB);
                    break;
                case R.id.tv_option_c:
                    validateAns("c", tvOptionC);
                    break;
                case R.id.tv_option_d:
                    validateAns("d", tvOptionD);
                    break;
            }

        }
    }

}