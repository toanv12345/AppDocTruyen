package com.example.appdoctruyen.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.appdoctruyen.R;
import com.example.appdoctruyen.adapter.NovelAdapter;
import com.example.appdoctruyen.object.Chapter;
import com.example.appdoctruyen.object.Novel;
import com.example.appdoctruyen.utils.EmailVerificationChecker;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    GridView gvDSTruyen;
    private DatabaseReference mDatabase;
    NovelAdapter novelAdapter;
    ArrayList<Novel> novelArrayList;
    private Button btnNextPage, btnPrevPage;
    private ViewFlipper imgNen;
    private static final int ITEMS_PER_PAGE = 18;

    private ImageView accountIcon;
    private FirebaseAuth auth;
    private ImageView imgFollow;
    SearchView searchView;
    private Button btnAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        super.setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("truyen");

        init();
        anhXa();
        setUp();
        setClick();
        checkLoginStatus();
        checkAdminStatus();
        startImageSlideshow();

        //createDefaultAdminAccount(); //vì tạo tài khoản admin mặc định nên chỉ cần chạy 1 lần

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        retrieveDataFromFirebase();

        // Set up SearchView listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterNovels(query);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                filterNovels(newText);
                return false;
            }
        });
    }

    private void init() {
        novelArrayList = new ArrayList<>();
        novelAdapter = new NovelAdapter(this, novelArrayList);
    }

    private void anhXa() {
        gvDSTruyen = findViewById(R.id.rvDSTruyen);
        gvDSTruyen.setAdapter(novelAdapter);
        btnNextPage = findViewById(R.id.btnNextPage);
        btnPrevPage = findViewById(R.id.btnPrevPage);
        accountIcon = findViewById(R.id.account_icon);
        searchView = findViewById(R.id.search_view);
        imgFollow = findViewById(R.id.img_follow);
        btnAdd = findViewById(R.id.btn_add);
        imgNen = findViewById(R.id.viewFlipper);
    }

    private void setUp() {
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, novelArrayList.size());
        ArrayList<Novel> pageData = new ArrayList<>(novelArrayList.subList(start, end));

        for(Novel novel : pageData) {
            String latestChapterName = getLatestChapterName(novel);
            novel.setLatestChapter(latestChapterName);
        }

        novelAdapter.updateList(pageData);
    }

    private void setClick() {
        // Thay đổi xử lý click cho accountIcon
        accountIcon.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                // Người dùng đã đăng nhập, hiển thị menu
                showAccountMenu(v);
            } else {
                // Người dùng chưa đăng nhập, chuyển đến màn hình đăng nhập
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddNovelActivity.class);
            startActivity(intent);
        });

        btnNextPage.setOnClickListener(v -> nextPage());
        btnPrevPage.setOnClickListener(v -> prevPage());

        imgFollow.setOnClickListener(v -> {
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                // Người dùng chưa đăng nhập
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Đăng nhập")
                        .setMessage("Bạn cần đăng nhập để có thể theo dõi truyện!")
                        .setPositiveButton("Đăng nhập ngay", (dialog, which) -> {
                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                            startActivity(intent);
                        })
                        .setNegativeButton("Để sau", (dialog, which) -> dialog.dismiss())
                        .show();
            } else {
                // Kiểm tra xem người dùng có phải admin không
                isUserAdmin(currentUser.getUid(), isAdmin -> {
                    if (isAdmin) {
                        // Admin luôn được truy cập danh sách theo dõi
                        Intent intent = new Intent(MainActivity.this, FollowActivity.class);
                        startActivity(intent);
                    } else if (currentUser.isEmailVerified()) {
                        // Người dùng thường đã xác thực email
                        Intent intent = new Intent(MainActivity.this, FollowActivity.class);
                        startActivity(intent);
                    } else {
                        // Người dùng thường chưa xác thực email
                        showEmailVerificationDialog(currentUser);
                    }
                });
            }
        });

        gvDSTruyen.setOnItemClickListener((parent, view, position, id) -> {
            Novel novel = (Novel) novelAdapter.getItem(position);
            Intent intent = new Intent(MainActivity.this, NovelsInfoActivity.class);
            intent.putExtra("novelId", novel.getId());
            intent.putExtra("tomtat", novel.getTomtat());
            startActivity(intent);
        });
    }

    // Phương thức kiểm tra user có phải admin không
    private void isUserAdmin(String userId, EmailVerificationChecker.AdminCheckCallback callback) {
        if (userId == null || callback == null) {
            if (callback != null) {
                callback.onAdminCheck(false);
            }
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId);

        userRef.child("isAdmin").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Boolean isAdmin = dataSnapshot.getValue(Boolean.class);
                callback.onAdminCheck(isAdmin != null && isAdmin);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onAdminCheck(false);
            }
        });
    }

    // Hiển thị menu khi click vào account icon
    private void showAccountMenu(View view) {
        // Tạo menu nhưng chưa hiển thị
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);
        popupMenu.getMenuInflater().inflate(R.menu.account_menu, popupMenu.getMenu());

        // Thiết lập listener xử lý sự kiện click
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            FirebaseUser currentUser = auth.getCurrentUser();

            if (id == R.id.menu_verify_status) {
                showEmailVerificationDialog(currentUser);
                return true;
            } else if (id == R.id.menu_change_password) {
                // Kiểm tra xem người dùng có phải admin không
                if (currentUser != null) {
                    if (currentUser.isEmailVerified()) {
                            // Người dùng thường đã xác thực email
                        Intent intent = new Intent(MainActivity.this, ChangePasswordActivity.class);
                        startActivity(intent);
                    } else {
                        // Người dùng thường chưa xác thực email
                        showEmailVerificationDialog(currentUser);
                    }
                }
                return true;
            } else if (id == R.id.menu_logout) {
                // Xử lý đăng xuất
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Đăng xuất")
                        .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                        .setPositiveButton("Có", (dialog, which) -> {
                            auth.signOut();
                            checkLoginStatus();
                            btnAdd.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Không", null)
                        .show();
                return true;
            }
            return false;
        });

        // Kiểm tra người dùng hiện tại
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            isUserAdmin(currentUser.getUid(), isAdmin -> {
                if (isAdmin) {
                    // Admin không cần xác thực email và không được đổi mật khẩu
                    popupMenu.getMenu().findItem(R.id.menu_verify_status).setVisible(false);
                    popupMenu.getMenu().findItem(R.id.menu_change_password).setVisible(false);
                } else if (currentUser.isEmailVerified()) {
                    // Người dùng thường đã xác thực email
                    popupMenu.getMenu().findItem(R.id.menu_verify_status).setVisible(false);
                    popupMenu.getMenu().findItem(R.id.menu_change_password).setVisible(true);
                } else {
                    // Người dùng thường chưa xác thực email
                    popupMenu.getMenu().findItem(R.id.menu_verify_status).setVisible(true);
                    popupMenu.getMenu().findItem(R.id.menu_change_password).setVisible(true);
                }

                // CHỈ HIỂN THỊ MENU SAU KHI ĐÃ CẬP NHẬT VISIBILITY CỦA CÁC MỤC
                popupMenu.show();
            });
        } else {
            // Nếu không có người dùng đăng nhập, không cần kiểm tra, hiển thị menu mặc định
            popupMenu.show();
        }
    }

    // Hiển thị dialog yêu cầu xác thực email
    private void showEmailVerificationDialog(FirebaseUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xác thực email");
        builder.setMessage("Bạn cần xác thực email để sử dụng đầy đủ tính năng của ứng dụng. Xác thực email cũng giúp bạn khôi phục tài khoản khi quên mật khẩu.");

        builder.setPositiveButton("Gửi lại email xác thực", (dialog, which) -> {
            if (user != null) {
                user.sendEmailVerification().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Đã gửi email xác thực. Vui lòng kiểm tra hộp thư của bạn.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Không thể gửi email xác thực: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        builder.setNeutralButton("Tôi đã xác thực, kiểm tra lại", (dialog, which) -> {
            // Hiển thị trạng thái đang tải
            AlertDialog loadingDialog = new AlertDialog.Builder(this)
                    .setTitle("Đang kiểm tra")
                    .setMessage("Vui lòng đợi...")
                    .setCancelable(false)
                    .show();

            // Tải lại thông tin người dùng để kiểm tra trạng thái xác thực
            user.reload().addOnCompleteListener(task -> {
                loadingDialog.dismiss();
                FirebaseUser reloadedUser = auth.getCurrentUser();
                if (reloadedUser != null && reloadedUser.isEmailVerified()) {
                    // Cập nhật trạng thái xác thực trong database
                    updateEmailVerificationStatus(reloadedUser);

                    new AlertDialog.Builder(this)
                            .setTitle("Xác thực thành công")
                            .setMessage("Email của bạn đã được xác thực!")
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Chưa xác thực")
                            .setMessage("Email của bạn vẫn chưa được xác thực. Vui lòng kiểm tra hộp thư và nhấn vào liên kết xác thực.")
                            .setPositiveButton("OK", null)
                            .show();
                }
            });
        });

        builder.setNegativeButton("Để sau", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // Cập nhật trạng thái xác thực email trong Database
    private void updateEmailVerificationStatus(FirebaseUser user) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
        userRef.child("emailVerified").setValue(true);
    }

    private void nextPage() {
        if ((currentPage + 1) * ITEMS_PER_PAGE < novelArrayList.size()) {
            currentPage++;
            setUp();
            gvDSTruyen.post(() -> gvDSTruyen.smoothScrollToPosition(0));
        }
    }

    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            setUp();
            gvDSTruyen.post(() -> gvDSTruyen.smoothScrollToPosition(0));
        }
    }

    private void retrieveDataFromFirebase() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                novelArrayList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Novel novel = snapshot.getValue(Novel.class);
                    if (novel != null) {
                        novel.setId(snapshot.getKey());
                        novelArrayList.add(novel);
                    }
                }

                // Sắp xếp theo ngày mới nhất định dạng dd/MM/yyyy
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                novelArrayList.sort((novel1, novel2) -> {
                    String dateStr1 = getLatestChapterDate(novel1);
                    String dateStr2 = getLatestChapterDate(novel2);

                    if (dateStr1.isEmpty() && dateStr2.isEmpty()) return 0;
                    if (dateStr1.isEmpty()) return 1;
                    if (dateStr2.isEmpty()) return -1;

                    try {
                        Date date1 = dateFormat.parse(dateStr1);
                        Date date2 = dateFormat.parse(dateStr2);
                        return date2.compareTo(date1); // Ngày mới hơn lên trước
                    } catch (ParseException e) {
                        e.printStackTrace();
                        return 0;
                    }
                });

                setUp();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MainActivity", "Failed to read data", databaseError.toException());
            }
        });
    }

    private void checkLoginStatus() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Người dùng đã đăng nhập → Đổi icon tài khoản thành avatar
            accountIcon.setImageResource(R.drawable.new_img_login_foreground);

            // Kiểm tra xem người dùng có phải admin không
            isUserAdmin(user.getUid(), isAdmin -> {
                if (!isAdmin && !user.isEmailVerified()) {
                    // Hiển thị thông báo nhắc nhở nếu người dùng không phải admin và chưa xác thực email
                    Toast.makeText(MainActivity.this,
                            "Vui lòng xác thực email để sử dụng đầy đủ tính năng của ứng dụng.",
                            Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Chưa đăng nhập → Hiển thị icon mặc định
            accountIcon.setImageResource(R.drawable.img_login_foreground);
        }
    }

    // tim kiem truyen
    private void filterNovels(String query) {
        ArrayList<Novel> filteredList = new ArrayList<>();
        for (Novel novel : novelArrayList) {
            String title = novel.getTentruyen() != null ? novel.getTentruyen().toLowerCase() : "";
            String author = novel.getTacgia() != null ? novel.getTacgia().toLowerCase() : "";
            String status = novel.getTinhtrang() != null ? novel.getTinhtrang().toLowerCase() : "";
            String genre = novel.getTheloai() != null ? novel.getTheloai().toLowerCase() : "";

            if (title.contains(query.toLowerCase()) ||
                    author.contains(query.toLowerCase()) ||
                    status.contains(query.toLowerCase()) ||
                    genre.contains(query.toLowerCase())) {
                filteredList.add(novel);
            }
        }
        novelAdapter.updateList(filteredList);
    }

    // Lấy ngày up của chapter mới nhất có nội dung
    private String getLatestChapterDate(Novel novel) {
        String latestDate = "";
        for (Chapter chapter : novel.getChapter().values()) {
            // Chỉ xét chapter có nội dung
            if (chapter.getNoidung() != null && !chapter.getNoidung().trim().isEmpty()) {
                if (chapter.getNgayup().compareTo(latestDate) > 0) {
                    latestDate = chapter.getNgayup();
                }
            }
        }
        return latestDate;
    }

    private void checkAdminStatus() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Boolean isAdmin = snapshot.child("isAdmin").getValue(Boolean.class);
                        if (isAdmin != null && isAdmin) {
                            btnAdd.setVisibility(View.VISIBLE);
                        } else {
                            btnAdd.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("MainActivity", "Failed to check admin status", error.toException());
                }
            });
        } else {
            btnAdd.setVisibility(View.GONE);
        }
    }

    //thêm tài khoản admin
    private void createDefaultAdminAccount() {
        String defaultAdminEmail = "admin@gmail.com";
        String defaultAdminPassword = "admin123";
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(defaultAdminEmail, defaultAdminPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null) {
                            usersRef.child(user.getUid()).child("isAdmin").setValue(true)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Log.d("MainActivity", "Admin account created successfully");
                                        } else {
                                            Log.e("MainActivity", "Failed to set admin status: " + task1.getException().getMessage());
                                        }
                                    });
                        }
                    } else {
                        Log.e("MainActivity", "Admin account creation failed: " + task.getException().getMessage());
                    }
                });
    }

    private String getLatestChapterName(Novel novel) {
        String latestChapterName = "";
        String latestDate = "";
        for (Chapter chapter : novel.getChapter().values()) {
            if (chapter.getNoidung() != null && !chapter.getNoidung().trim().isEmpty()) {
                if (chapter.getNgayup().compareTo(latestDate) > 0) {
                    latestDate = chapter.getNgayup();
                    latestChapterName = chapter.getTitle();
                }
            }
        }
        return latestChapterName;
    }

    private void startImageSlideshow() {
        // Thiết lập animation lướt sang trái cho ViewFlipper
        Animation in = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        Animation out = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);

        imgNen.setInAnimation(in);
        imgNen.setOutAnimation(out);

        // Thời gian chuyển đổi ảnh
        imgNen.setFlipInterval(4500);

        // Bắt đầu tự động lật
        imgNen.startFlipping();
    }

    // Thêm phần khai báo biến currentPage nếu chưa có
    private int currentPage = 0;

    @Override
    protected void onResume() {
        super.onResume();
        checkLoginStatus(); // Cập nhật trạng thái đăng nhập khi quay lại màn hình
        checkAdminStatus(); // Cập nhật trạng thái admin
    }
}