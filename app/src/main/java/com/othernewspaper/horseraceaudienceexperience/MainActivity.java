package com.othernewspaper.horseraceaudienceexperience;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.othernewspaper.horseraceaudienceexperience.R;
import com.othernewspaper.horseraceaudienceexperience.app.AppConfig;
import com.othernewspaper.horseraceaudienceexperience.app.AppController;
import com.othernewspaper.horseraceaudienceexperience.app.SQLiteHandler;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_IMAGE = 100;
    private static final String TAG = MainActivity.class.getSimpleName();
    private CircleImageView ivProfilePic;
    private CircleImageView ivEditPic;
    private AppCompatEditText etNickname;
    private AppCompatButton btnSubmit;
    private AppCompatTextView tvUsername;

    private boolean isQuizReady = false;

    private ProgressDialog pDialog;
    private SQLiteHandler db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pDialog = new ProgressDialog(MainActivity.this);
        pDialog.setCancelable(false);
        db = new SQLiteHandler(MainActivity.this);
        ivProfilePic = findViewById(R.id.iv_profile_pic);
        ivEditPic = findViewById(R.id.iv_edit_pic);
        etNickname = findViewById(R.id.et_nickname);
        btnSubmit = findViewById(R.id.btn_submit);
        tvUsername = findViewById(R.id.tv_username);
        ButterKnife.bind(this);
        loadProfileDefault();
        // Clearing older images from cache directory
        // don't call this line if you want to choose multiple images in the same activity
        // call this once the bitmap(s) usage is over
        ImagePickerActivity.clearCache(this);
        ivEditPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dexter.withActivity(MainActivity.this).withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE).withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            showImagePickerOptions();
                        }
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
            }
        });
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String btnAction = btnSubmit.getText().toString().trim();
                if (btnAction.equalsIgnoreCase("save")) {
                    getSessionStatus();
                } else {
                    getQuizStatus();
                }
            }
        });

    }

    private void uploadToServer(String nickname, String image) {
// Tag used to cancel the request
        String tag_string_req = "req_register";
        pDialog.setMessage("Registering...");
        showDialog();
        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_REGISTER_USER, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                hideDialog();
                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    // Check for error node in json
                    if (!error) {
                        db.deleteUsers();
                        JSONObject user = jObj.getJSONObject("user");
                        String uid = user.getString("id");
                        String name = user.getString("name");
                        String created_at = user.getString("created_at");
                        db.addUser(name, uid, created_at);
                        btnSubmit.setText("Start Quiz");
                        tvUsername.setText(name);
                        etNickname.setVisibility(View.GONE);
                        tvUsername.setVisibility(View.VISIBLE);

                    } else {
                        Log.e(TAG, jObj.getString("error_msg"));
                        Toast.makeText(getApplicationContext(), jObj.getString("error_msg"), Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage());
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, error.getMessage());
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("nickname", nickname);
                params.put("profile_pic", image);
                return params;
            }

        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);

    }


    private void getQuizStatus() {
        // Tag used to cancel the request
        String tag_string_req = "req_get_quiz_status";
        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_PRESENTER, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    // Check for error node in json
                    if (!error) {
                        int status = jObj.getInt("status");
                        if (status == 1) {
                            Intent i = new Intent(MainActivity.this, QuestionsActivity.class);
                            startActivity(i);
                            finish();
                        } else {
                            Toast.makeText(getApplicationContext(), "Quiz is not yet ready. Have some patience!", Toast.LENGTH_LONG).show();
                        }

                    } else {
                    }
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), "Please check your internet connection!", Toast.LENGTH_LONG).show();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "Please check your internet connection!", Toast.LENGTH_LONG).show();

            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("action", "get_quiz_status");
                return params;
            }

        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);

    }

    private void getSessionStatus() {
        // Tag used to cancel the request
        String tag_string_req = "req_get_session_status";
        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_PRESENTER, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jObj = new JSONObject(response);
                    Log.e(TAG, response);
                    boolean error = jObj.getBoolean("error");
                    // Check for error node in json
                    if (!error) {
                        int status = jObj.getInt("status");
                        if (status == 1) {
                            String nickname = etNickname.getText().toString().trim();
                            Bitmap bitmap = ((BitmapDrawable) ivProfilePic.getDrawable()).getBitmap();
                            if (!nickname.isEmpty() && bitmap != null) {
                                uploadToServer(nickname, bitmapToString(bitmap));
                            } else {
                                Toast.makeText(MainActivity.this, "Image or Nickname cannot be blank.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "Session is not yet ready. Have some patience!", Toast.LENGTH_LONG).show();
                        }

                    } else {
                        String errorMsg = jObj.getString("error_msg");
                        Log.e(TAG, errorMsg);
                        Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {

                    Log.e(TAG, e.getMessage());
                    Toast.makeText(getApplicationContext(), "Please check your internet connection!", Toast.LENGTH_LONG).show();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, error.getMessage());
                Toast.makeText(getApplicationContext(), "Please check your internet connection!", Toast.LENGTH_LONG).show();

            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("action", "get_session_status");
                return params;
            }

        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);

    }

    private String bitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imgBytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imgBytes, Base64.DEFAULT);
    }

    private void loadProfile(String url) {
        Log.d(TAG, "Image cache path: " + url);
        Glide.with(this).load(url).into(ivProfilePic);
        ivProfilePic.setColorFilter(ContextCompat.getColor(this, android.R.color.transparent));
    }

    private void loadProfileDefault() {
        Glide.with(this).load(R.drawable.ic_sharp_account_circle_24).into(ivProfilePic);
        ivProfilePic.setColorFilter(ContextCompat.getColor(this, R.color.profile_default_tint));
    }

    private void showImagePickerOptions() {
        ImagePickerActivity.showImagePickerOptions(this, new ImagePickerActivity.PickerOptionListener() {
            @Override
            public void onTakeCameraSelected() {
                launchCameraIntent();
            }

            @Override
            public void onChooseGallerySelected() {
                launchGalleryIntent();
            }
        });
    }

    private void launchCameraIntent() {
        Intent intent = new Intent(MainActivity.this, ImagePickerActivity.class);
        intent.putExtra(ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION, ImagePickerActivity.REQUEST_IMAGE_CAPTURE);
        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true);
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1); // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1);
        // setting maximum bitmap width and height
        intent.putExtra(ImagePickerActivity.INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT, true);
        intent.putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_WIDTH, 1000);
        intent.putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_HEIGHT, 1000);
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    private void launchGalleryIntent() {
        Intent intent = new Intent(MainActivity.this, ImagePickerActivity.class);
        intent.putExtra(ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION, ImagePickerActivity.REQUEST_GALLERY_IMAGE);
        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true);
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1); // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1);
        startActivityForResult(intent, REQUEST_IMAGE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getParcelableExtra("path");
            // loading profile image from local cache
            loadProfile(uri.toString());
        } else {
            Toast.makeText(MainActivity.this, "Error While choosing image. Please Try again!", Toast.LENGTH_LONG).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Showing Alert Dialog with Settings option
     * Navigates user to app settings
     * NOTE: Keep proper title and message depending on your app
     */
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getString(R.string.dialog_permission_title));
        builder.setMessage(getString(R.string.dialog_permission_message));
        builder.setPositiveButton(getString(R.string.go_to_settings), (dialog, which) -> {
            dialog.cancel();
            openSettings();
        });
        builder.setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> dialog.cancel());
        builder.show();

    }

    // navigating user to app settings
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }

    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

}