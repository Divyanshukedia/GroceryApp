package com.app.fresy;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.core.widget.TextViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.fresy.adapter.AdapterHome;
import com.app.fresy.connection.API;
import com.app.fresy.connection.RestAdapter;
import com.app.fresy.data.AppConfig;
import com.app.fresy.data.Constant;
import com.app.fresy.data.SharedPref;
import com.app.fresy.data.ThisApp;
import com.app.fresy.fragment.FragmentDetails;
import com.app.fresy.model.Attribute;
import com.app.fresy.model.Category;
import com.app.fresy.model.CategorySection;
import com.app.fresy.model.Product;
import com.app.fresy.model.Slider;
import com.app.fresy.model.Sorting;
import com.app.fresy.model.SortingSection;
import com.app.fresy.model.User;
import com.app.fresy.model.Variation;
import com.app.fresy.room.AppDatabase;
import com.app.fresy.room.DAO;
import com.app.fresy.room.table.CartEntity;
import com.app.fresy.utils.NetworkCheck;
import com.app.fresy.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityMain extends AppCompatActivity {

    private Call<List<Product>> callbackProducts = null;
    private Call<List<Category>> callbackCategories = null;
    private Call<List<Slider>> callbackSliders = null;

    public static final int REQUEST_CHECKOUT = 5000;

    // class variable
    List<Category> categories;
    List<Slider> sliders;
    List<Product> products;
    private Category category = null;
    private DAO db = null;
    private AdapterHome mAdapter;
    private User user = new User();
    private FragmentManager manager;
    private SharedPref sharedPref;
    private Sorting sorting;

    private BottomSheetBehavior sheetBehavior;

    // view variable
    private View parent_view;
    private View notif_badge = null;
    private SwipeRefreshLayout swipe_refresh;
    private ActionBar actionBar;
    private Toolbar toolbar;
    private AppBarLayout appbar_layout;
    private View lyt_cart_sheet, lyt_main_content;
    private TextView tv_total_cart;
    private EditText et_search;
    private AppCompatButton counter_badge, btn_continue;
    private ImageView bt_clear;
    private DrawerLayout drawer;
    private RecyclerView recyclerView;
    private ShimmerFrameLayout shimmer;
    private boolean is_product_load_all = false;
    private boolean is_request_product_finish = false;
    private boolean is_request_category_finish = false;
    private boolean is_request_slider_finish = false;
    private int page_no = -1;
    private boolean from_category = false;
    private boolean isSearchBarHide = false;
    private int notification_count = -1;
    private boolean is_login = false;

    // variation sheet variable
    Map<String, String> selectedVariationMap = new HashMap<>();
    Map<String, Button> selectedVariationButton = new HashMap<>();
    String variationKey[];
    Variation selectedVariation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getDb(this).get();
        manager = getSupportFragmentManager();
        sharedPref = new SharedPref(this);
        sorting = ThisApp.get().getSortingList().get(0);

        initToolbar();
        initComponent();
        initDrawerMenu();
        Tools.hideKeyboard(this);

        mAdapter.resetListData();
        // request action will be here
        requestAction(1, true);
        Tools.RTLMode(getWindow());

        // launch instruction when first launch
        if (sharedPref.isFirstLaunch()) {
            startActivity(new Intent(this, ActivityIntro.class));
            sharedPref.setFirstLaunch(false);
        }
    }

    private void initToolbar() {
        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_menu);
        toolbar.getNavigationIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Tools.setSystemBarColor(this);
    }

    private void initComponent() {
        parent_view = findViewById(android.R.id.content);
        swipe_refresh = findViewById(R.id.swipe_refresh);
        shimmer = findViewById(R.id.shimmer_main);
        appbar_layout = findViewById(R.id.appbar_layout);
        lyt_cart_sheet = findViewById(R.id.lyt_cart_sheet);
        lyt_main_content = findViewById(R.id.lyt_main_content);
        tv_total_cart = findViewById(R.id.tv_total_cart);
        bt_clear = findViewById(R.id.bt_clear);
        et_search = findViewById(R.id.et_search);
        recyclerView = findViewById(R.id.recycler_view_main);
        counter_badge = findViewById(R.id.counter_badge);
        btn_continue = findViewById(R.id.btn_continue);

        et_search.setFocusableInTouchMode(true);
        lyt_cart_sheet.setVisibility(View.GONE);
        swipe_refresh.setProgressViewOffset(false, 0, Tools.dpToPx(this, 75));
        recyclerView.setLayoutManager(new GridLayoutManager(this, Tools.getGridSpanCount(this)));
        recyclerView.setHasFixedSize(true);
        //set data and list adapter
        mAdapter = new AdapterHome(this, recyclerView);
        recyclerView.setAdapter(mAdapter);
        refreshCartSheet();

        sheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));

        mAdapter.setOnItemClickListener(new AdapterHome.OnItemClickListener() {
            @Override
            public void onProductClick(View view, Product obj, CartEntity cart, AdapterHome.ActionType actionType, int position) {
                onProductActionClick(obj, cart, actionType, position);
            }

            @Override
            public void onCategoryClick(View view, Category obj, int position) {
                category = obj;
                productFilterAction();
            }

            @Override
            public void onSliderClick(View view, Slider obj, int position) {
                Tools.directLinkCustomTab(ActivityMain.this, obj.url);
            }

            @Override
            public void onOtherAction(View view, AdapterHome.OtherActionType actionType, int position) {
                if(actionType == AdapterHome.OtherActionType.SORTING){
                    showSortingSelect();
                } else if(actionType == AdapterHome.OtherActionType.CATEGORY_ALL) {
                    mAdapter.showAllCategory(categories);
                } else if(actionType == AdapterHome.OtherActionType.CATEGORY_LESS) {
                    mAdapter.showLessCategory();
                }
            }
        });

        // detect when scroll reach bottom
        mAdapter.setOnLoadMoreListener(new AdapterHome.OnLoadMoreListener() {
            @Override
            public void onLoadMore(int current_page) {
                if (!is_product_load_all && current_page != 0) {
                    int next_page = current_page + 1;
                    requestAction(next_page, next_page == 1);
                } else {
                    mAdapter.setLoaded();
                }
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 5) { // down / show
                    animateSearchBar(true);
                } else if (dy < -5) { // up / hide
                    animateSearchBar(false);
                }
            }
        });

        swipe_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (callbackProducts != null && callbackProducts.isExecuted())
                    callbackProducts.cancel();
                if (callbackCategories != null && callbackCategories.isExecuted())
                    callbackCategories.cancel();
                if (callbackSliders != null && callbackSliders.isExecuted())
                    callbackSliders.cancel();
                is_product_load_all = false;
                from_category = false;
                requestAction(1, true);
            }
        });

        et_search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    productFilterAction();
                    return true;
                }
                return false;
            }
        });

        et_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence c, int i, int i1, int i2) {
                if (c.toString().trim().length() == 0) {
                    bt_clear.setVisibility(View.GONE);
                } else {
                    bt_clear.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence c, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        bt_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                et_search.setText("");
                productFilterAction();
            }
        });

        btn_continue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityCart.navigate(ActivityMain.this, REQUEST_CHECKOUT);
            }
        });
    }

    public void onProductActionClick(Product obj, CartEntity cart, AdapterHome.ActionType actionType, int position) {
        if (actionType == AdapterHome.ActionType.NORMAL) {
            showFragmentDetails(obj, position);
            return;
        }
        if (actionType == AdapterHome.ActionType.BUY) {
            if (Tools.checkEmptyPrice(ActivityMain.this, obj)) return;
            if (Tools.checkOutOfStockSnackBar(ActivityMain.this, obj, 1)) return;
            if (obj.type.equals("variable")) {
                showVariationSheet(position, obj);
            } else {
                CartEntity c = CartEntity.entity(obj);
                c.setAmount(1);
                c.setSaved_date(System.currentTimeMillis());
                db.insertCart(c);
            }

        } else if (actionType == AdapterHome.ActionType.PLUS) {
            long new_amount = cart.getAmount() + 1;
            if (Tools.checkOutOfStockSnackBar(ActivityMain.this, obj, new_amount)) return;
            cart.setAmount(new_amount);
            db.updateCart(cart);

        } else if (actionType == AdapterHome.ActionType.MINUS) {
            if (cart.getAmount() == 1) {
                db.deleteCart(cart.getId());
            } else {
                cart.setAmount(cart.getAmount() - 1);
                db.updateCart(cart);
            }
        }
        mAdapter.notifyItemChanged(position);
        refreshCartSheet();
    }

    private void showVariationSheet(int position, Product obj) {
        if (sheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
        BottomSheetDialog mBottomSheetDialog = new BottomSheetDialog(this);
        Tools.RTLMode(mBottomSheetDialog.getWindow());

        final View view = getLayoutInflater().inflate(R.layout.sheet_variation, null);
        ((TextView) view.findViewById(R.id.title)).setText(obj.name);
        TextView tv_price = view.findViewById(R.id.price);
        TextView tv_stock = view.findViewById(R.id.stock);
        tv_price.setText(Html.fromHtml(obj.price_html));

        view.findViewById(R.id.btn_buy).setOnClickListener(view1 -> {
            if (selectedVariation == null) return;
            selectedVariation.parent = obj;
            Product p = selectedVariation.getProduct();
            if (Tools.checkOutOfStockToast(ActivityMain.this, p, 1)) return;
            CartEntity c = CartEntity.entity(p);
            c.setAmount(1);
            c.setSaved_date(System.currentTimeMillis());
            db.insertCart(c);
            mBottomSheetDialog.dismiss();
            refreshCartSheet();
            mAdapter.notifyItemChanged(position);
        });

        LinearLayout lyt_container = view.findViewById(R.id.lyt_container);
        View progress_bar = view.findViewById(R.id.progress_bar);
        lyt_container.removeAllViews();

        Iterator<Attribute> it = obj.attributes.iterator();
        while (it.hasNext()) {
            Attribute a = it.next();
            if (!a.variation) it.remove();
        }

        Collections.sort(obj.attributes, new Comparator<Attribute>() {
            @Override
            public int compare(Attribute item1, Attribute item2) {
                return item1.position - item2.position;
            }
        });

        variationKey = new String[obj.attributes.size()];
        int index = 0;

        for (Attribute a : obj.attributes) {
            TextView tv = new TextView(this);
            TextViewCompat.setTextAppearance(tv, R.style.Base_TextAppearance_AppCompat_Caption);
            tv.setTextColor(getResources().getColor(R.color.grey_40));
            int paddingHorizontal = getResources().getDimensionPixelOffset(R.dimen.spacing_xmedium);
            tv.setPadding(paddingHorizontal, paddingHorizontal, paddingHorizontal, 0);
            tv.setText(a.name);
            lyt_container.addView(tv);

            FlexboxLayout flexboxLayout = new FlexboxLayout(this);
            flexboxLayout.setFlexWrap(FlexWrap.WRAP);
            for (String cat : a.options) {
                String key = a.name + "|" + cat;
                Button bt = new Button(new ContextThemeWrapper(this, R.style.Base_Widget_AppCompat_Button_Borderless), null, R.style.Base_Widget_AppCompat_Button_Borderless);
                // set margin
                int marginHorizontal = getResources().getDimensionPixelOffset(R.dimen.spacing_medium);
                int marginVertical = getResources().getDimensionPixelOffset(R.dimen.spacing_medium);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical);
                bt.setLayoutParams(params);
                bt.setMinimumHeight(0);
                bt.setMinHeight(0);

                // set padding
                int paddingHorizontalButton = getResources().getDimensionPixelOffset(R.dimen.spacing_large);
                bt.setPadding(paddingHorizontalButton, 0, paddingHorizontalButton, 0);
                bt.setGravity(Gravity.CENTER);
                bt.setAllCaps(false);
                TextViewCompat.setTextAppearance(bt, R.style.Base_TextAppearance_AppCompat_Caption);
                bt.setTextColor(getResources().getColor(R.color.grey_80));

                // set font and color
                bt.setBackgroundResource(R.drawable.button_variation);
                bt.setText(cat);
                int finalIndex = index;

                bt.setOnClickListener(v -> {
                    if (selectedVariationMap.get(a.name) != null) {
                        ((Button) selectedVariationButton.get(selectedVariationMap.get(a.name))).setSelected(false);
                        ((Button) selectedVariationButton.get(selectedVariationMap.get(a.name))).setTextColor(getResources().getColor(R.color.grey_80));
                    }
                    ((Button) v).setSelected(true);
                    ((Button) v).setTextColor(Color.WHITE);
                    selectedVariationMap.put(a.name, key);

                    variationKey[finalIndex] = a.name + cat;
                    boolean selectedAll = true;
                    String combinedKey = "";
                    for (String k : variationKey) {
                        if (k == null) selectedAll = false;
                        combinedKey = combinedKey + k;
                    }
                    if (selectedAll) {
                        selectedVariation = obj.variationsMap.get(combinedKey);
                        if (selectedVariation != null) {
                            tv_price.setText(Tools.getPrice(selectedVariation.price));
                            String stock_text = getString(R.string.available);
                            if (selectedVariation.stock_status.equals("instock")) {
                                if (selectedVariation.manage_stock) {
                                    stock_text = stock_text + " " + selectedVariation.stock_quantity;
                                }
                            } else {
                                stock_text = getString(R.string.out_of_stock);
                            }
                            tv_stock.setText(stock_text);
                        }
                    }
                });

                //textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.txt_small));
                selectedVariationButton.put(key, bt);
                flexboxLayout.addView(bt);
            }
            lyt_container.addView(flexboxLayout);
            index++;
        }

        API api = RestAdapter.createAPI();
        Callback<List<Variation>> callback = new Callback<List<Variation>>() {
            @Override
            public void onResponse(Call<List<Variation>> call, Response<List<Variation>> response) {
                List<Variation> resp = response.body();
                progress_bar.setVisibility(View.GONE);
                if (resp == null || resp.size() == 0) {
                    view.findViewById(R.id.lyt_failed).setVisibility(View.VISIBLE);
                    ((TextView) view.findViewById(R.id.failed_message)).setText(R.string.failed_load_variation);
                } else {
                    lyt_container.setVisibility(View.VISIBLE);
                    obj.variationsMap = new HashMap<>();
                    for (Variation v : resp) {
                        String combinedKey = "";
                        for (Attribute attr : v.attributes) {
                            combinedKey = combinedKey + attr.name + attr.option;
                        }
                        obj.variationsMap.put(combinedKey, v);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Variation>> call, Throwable t) {
                progress_bar.setVisibility(View.GONE);
                view.findViewById(R.id.lyt_failed).setVisibility(View.VISIBLE);
                ((TextView) view.findViewById(R.id.failed_message)).setText(R.string.failed_load_variation);
            }
        };

        Call<List<Variation>> callbackVariation = api.listProductVariations(obj.id);
        progress_bar.setVisibility(View.GONE);
        if (obj.variationsMap.isEmpty()) {
            lyt_container.setVisibility(View.INVISIBLE);
            progress_bar.setVisibility(View.VISIBLE);
            callbackVariation.enqueue(callback);
        }

        (view.findViewById(R.id.failed_retry)).setOnClickListener(view12 -> {
            progress_bar.setVisibility(View.VISIBLE);
            view.findViewById(R.id.lyt_failed).setVisibility(View.GONE);
            callbackVariation.clone().enqueue(callback);
        });

        mBottomSheetDialog.setContentView(view);
        mBottomSheetDialog.show();
    }

    private void requestListProduct(final int page_no) {
        Long category_id = category != null ? category.id : null;
        String search = et_search.getText().toString();
        API api = RestAdapter.createAPI();
        callbackProducts = api.listProduct(
            page_no, Constant.PRODUCT_PER_REQUEST, category_id, search,
            sorting.orderby, sorting.order
        );
        callbackProducts.enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                List<Product> resp = response.body();
                is_request_product_finish = true;
                if (resp == null) return;
                if (resp.size() == 0) {
                    showProgress(false);
                    mAdapter.setLoaded();
                } else {
                    if (resp.size() < Constant.PRODUCT_PER_REQUEST) is_product_load_all = true;
                    products = resp;
                    displayApiResult(resp);
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }
        });
    }

    private void requestAllCategory() {
        API api = RestAdapter.createAPI();
        callbackCategories = api.allCategory();
        callbackCategories.enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                is_request_category_finish = true;
                if (response.body() != null && response.body().size() > 0) {
                    categories = Tools.getSortedCategory(response.body());
                    categories.get(0).selected = true;
                    category = categories.get(0);
                    displayApiResult(null);
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }
        });
    }

    private void requestAllSlider() {
        API api = RestAdapter.createAPI();
        callbackSliders = api.allSlider();
        callbackSliders.enqueue(new Callback<List<Slider>>() {
            @Override
            public void onResponse(Call<List<Slider>> call, Response<List<Slider>> response) {
                sliders = response.body();
                is_request_slider_finish = true;
                if (sliders != null && sliders.size() > 0) {
                    displayApiResult(null);
                }
            }

            @Override
            public void onFailure(Call<List<Slider>> call, Throwable t) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }
        });
    }

    private void productFilterAction() {
        finishFragmentDetails();
        if (callbackProducts != null && callbackProducts.isExecuted()) callbackProducts.cancel();
        is_product_load_all = false;
        from_category = true;
        mAdapter.clearListProduct();
        requestAction(1, false);
        Tools.hideKeyboard(this);
    }

    private void requestAction(final int page_no, boolean load_header) {
        showFailedView(false, "", R.drawable.logo_small);
        this.page_no = page_no;
        if (page_no == 1 && load_header) {
            mAdapter.resetListData();
            requestAllSlider();
            requestAllCategory();
            showProgress(true);
        } else {
            mAdapter.setLoading();
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                requestListProduct(page_no);
            }
        }, 500);
    }

    private void displayApiResult(final List<Product> items) {
        if (page_no == 1 && !from_category && is_request_product_finish && is_request_category_finish && is_request_slider_finish) {
            mAdapter.resetListData();
            if (sliders != null && sliders.size() > 0 && AppConfig.SHOW_SLIDER_HOME) {
                mAdapter.setSlider(sliders);
            }
            if (categories != null && categories.size() > 0) {
                mAdapter.setCategorySection(new CategorySection());
                int limit_category = Math.min(AppConfig.MAX_CATEGORY_HOME, categories.size());
                List<Category> categoriesFeatured = categories.subList(0, limit_category);
                mAdapter.setCategory(categoriesFeatured);
            }
            mAdapter.setProductSection(new SortingSection(sorting));
            mAdapter.insertData(products);
        } else if (items != null) {
            mAdapter.insertData(items);
        }
        showProgress(false);
    }

    private void showProgress(final boolean show) {
        swipe_refresh.post(new Runnable() {
            @Override
            public void run() {
                swipe_refresh.setRefreshing(show);
            }
        });
        if (!show) {
            shimmer.setVisibility(View.GONE);
            shimmer.stopShimmer();
            lyt_main_content.setVisibility(View.VISIBLE);
            return;
        }
        lyt_main_content.setVisibility(View.INVISIBLE);
        shimmer.setVisibility(View.VISIBLE);
        shimmer.startShimmer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);

        final MenuItem menuItem = menu.findItem(R.id.action_notification);
        View actionView = MenuItemCompat.getActionView(menuItem);
        notif_badge = actionView.findViewById(R.id.notif_badge);

        setupBadge();

        actionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(menuItem);
            }
        });

        return true;
    }

    private void setupBadge() {
        if (notif_badge == null) return;
        notif_badge.setVisibility(notification_count == 0 ? View.INVISIBLE : View.VISIBLE);
    }

    private void onFailRequest(int page_no) {
        mAdapter.setLoaded();
        showProgress(false);
        if (NetworkCheck.isConnect(this)) {
            showFailedView(true, getString(R.string.failed_text), R.drawable.img_failed);
        } else {
            showFailedView(true, getString(R.string.no_internet_text), R.drawable.img_no_internet);
        }
    }

    private void showFailedView(boolean show, String message, @DrawableRes int icon) {
        View lyt_failed = findViewById(R.id.lyt_failed);

        ((ImageView) findViewById(R.id.failed_icon)).setImageResource(icon);
        ((TextView) findViewById(R.id.failed_message)).setText(message);
        if (show) {
            recyclerView.setVisibility(View.INVISIBLE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        (findViewById(R.id.failed_retry)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestAction(page_no, category == null);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menu_id = item.getItemId();
        if (menu_id == android.R.id.home) {
            if (!drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.openDrawer(GravityCompat.START);
            } else {
                drawer.openDrawer(GravityCompat.END);
            }
        } else if (menu_id == R.id.action_notification) {
            ActivityNotification.navigate(this);

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        et_search.clearFocus();
        mAdapter.notifyDataSetChanged();
        refreshCartSheet();

        is_login = ThisApp.get().isLogin();
        user = ThisApp.get().getUser();
        initDrawerMenu();

        int new_notif_count = db.getNotificationUnreadCount();
        if (new_notif_count != notification_count) {
            notification_count = new_notif_count;
            invalidateOptionsMenu();
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Tools.checkGooglePlayUpdate(this);
    }

    static boolean active = false;

    @Override
    public void onDestroy() {
        if (callbackProducts != null && !callbackProducts.isCanceled()) callbackProducts.cancel();
        if (callbackCategories != null && !callbackCategories.isCanceled())
            callbackCategories.cancel();
        if (callbackSliders != null && !callbackSliders.isCanceled()) callbackSliders.cancel();
        if (shimmer != null) shimmer.stopShimmer();
        active = false;
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        active = true;
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        if (manager.getBackStackEntryCount() > 0) {
            super.onBackPressed();
        } else {
            exitConfirmation();
        }
    }

    private void exitConfirmation() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setIcon(Tools.tintDrawable(this, R.drawable.ic_info_outline, R.color.colorPrimary));
        builder.setTitle(R.string.exit_app_confirmation);
        builder.setCancelable(true);
        builder.setNegativeButton(R.string.NO, null);
        builder.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.show();
    }

    private void animateSearchBar(final boolean hide) {
        if (isSearchBarHide && hide || !isSearchBarHide && !hide) return;
        isSearchBarHide = hide;
        int moveY = hide ? -(2 * appbar_layout.getHeight()) : 0;
        appbar_layout.animate().translationY(moveY).setStartDelay(100).setDuration(300).start();
    }

    private void refreshCartSheet() {
        List<CartEntity> cartEntities = db.getAllCart();
        if (cartEntities.size() == 0) {
            lyt_cart_sheet.setVisibility(View.GONE);
            return;
        }
        lyt_cart_sheet.setVisibility(View.VISIBLE);
        double price = 0d;
        long counter = 0;
        for (CartEntity c : cartEntities) {
            price = price + (Double.parseDouble(c.getPrice()) * c.getAmount());
            counter = counter + c.getAmount();
        }
        tv_total_cart.setText(Tools.getPrice(price));

        // animate badge
        ScaleAnimation fade_in = new ScaleAnimation(0.0f, 1f, 0.0f, 1f, 1, 0.5f, 1, 0.5f);
        fade_in.setDuration(200);
        fade_in.setFillAfter(true);
        counter_badge.startAnimation(fade_in);
        String counter_text = counter > 99 ? "99+" : counter + "";
        counter_badge.setText(counter_text);
    }

    private void initDrawerMenu() {
        NavigationView nav_view = findViewById(R.id.nav_view);
        drawer = findViewById(R.id.drawer_layout);
        View lyt_user = nav_view.findViewById(R.id.lyt_user);
        View tv_app_title = nav_view.findViewById(R.id.tv_app_title);
        TextView login_logout = nav_view.findViewById(R.id.tv_login_logout);
        TextView name = nav_view.findViewById(R.id.name);
        TextView email = nav_view.findViewById(R.id.email);
        View lyt_user_menu = nav_view.findViewById(R.id.lyt_user_menu);
        if (is_login) {
            login_logout.setText(getString(R.string.logout_title));
            name.setText(user.getName());
            email.setText(user.email);
            lyt_user.setVisibility(View.VISIBLE);
            tv_app_title.setVisibility(View.GONE);
            lyt_user_menu.setVisibility(View.VISIBLE);
        } else {
            login_logout.setText(getString(R.string.login_title));
            name.setText("");
            lyt_user.setVisibility(View.GONE);
            tv_app_title.setVisibility(View.VISIBLE);
            lyt_user_menu.setVisibility(View.GONE);
        }

        login_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginLogout();
            }
        });

        drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //showInterstitial();
            }
        });
    }


    public void onDrawerMenuClick(View view) {
        int menu_id = view.getId();
        if (menu_id == R.id.nav_menu_home) {
            drawer.closeDrawers();
        } else if (menu_id == R.id.nav_menu_address) {
            ActivityAddress.navigate(this);
        } else if (menu_id == R.id.nav_menu_profile) {
            ActivityRegisterProfile.navigate(ActivityMain.this, user);
        } else if (menu_id == R.id.nav_menu_history) {
            ActivityOrderHistory.navigate(this);
        } else if (menu_id == R.id.nav_menu_settings) {
            ActivitySettings.navigate(this);
        } else if (menu_id == R.id.nav_menu_contact) {
            Tools.directLinkToBrowser(this, Constant.CONTACT_US_URL);
        } else if (menu_id == R.id.nav_menu_about) {
            Tools.openInAppBrowser(this, Constant.ABOUT_US_URL, false);
        } else if (menu_id == R.id.nav_menu_privacy) {
            Tools.openInAppBrowser(this, Constant.PRIVACY_POLICY_URL, false);
        } else if (menu_id == R.id.nav_menu_intro) {
            startActivity(new Intent(this, ActivityIntro.class));
        } else if (menu_id == R.id.nav_menu_notif) {
            ActivityNotification.navigate(this);
        }
    }

    private void showSortingSelect() {
        List<Sorting> sortingList = ThisApp.get().getSortingList();
        String[] sortingArr = new String[sortingList.size()];
        int selected = 0;
        for (int i = 0; i < sortingList.size(); i++) {
            sortingArr[i] = sortingList.get(i).title;
            if (sorting.title.equalsIgnoreCase(sortingArr[i])) {
                selected = i;
            }
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.sorting);
        builder.setSingleChoiceItems(sortingArr, selected, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sorting = sortingList.get(which);
                mAdapter.updateSortingText(sorting);
                productFilterAction();
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void showDialogLogout() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setIcon(Tools.tintDrawable(this, R.drawable.ic_info_outline, R.color.colorPrimary));
        builder.setTitle(R.string.logout_confirmation_text);
        builder.setNegativeButton(R.string.CANCEL, null);
        builder.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ThisApp.get().logout();
                onResume();
            }
        });
        builder.setCancelable(true);
        builder.show();
    }

    public void loginLogout() {
        if (is_login) {
            showDialogLogout();
        } else {
            ActivityLogin.navigate(this);
        }
    }

    public void showFragmentDetails(Product product, int position) {
        final View frame_content_details = findViewById(R.id.frame_content_details);
        recyclerView.setVisibility(View.GONE);
        swipe_refresh.setEnabled(false);
        frame_content_details.setVisibility(View.VISIBLE);

        animateSearchBar(false);

        FragmentTransaction transaction = manager.beginTransaction();
        FragmentDetails fragmentDetails = FragmentDetails.newInstance(product);
        fragmentDetails.setCommunicator(new FragmentDetails.Communicator() {

            @Override
            public void onActionClick(Product obj, CartEntity cart, AdapterHome.ActionType actionType) {
                onProductActionClick(obj, cart, actionType, position);
            }

            @Override
            public void fragmentDetached() {
                recyclerView.setVisibility(View.VISIBLE);
                frame_content_details.setVisibility(View.GONE);
                swipe_refresh.setEnabled(true);
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onScroll(boolean down) {
                animateSearchBar(down);
            }

        });
        transaction.add(R.id.frame_content_details, fragmentDetails, product.id.toString());
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void finishFragmentDetails() {
        if (manager != null && manager.popBackStackImmediate()) {
            recyclerView.setVisibility(View.VISIBLE);
            findViewById(R.id.frame_content_details).setVisibility(View.GONE);
            swipe_refresh.setEnabled(true);
            mAdapter.notifyDataSetChanged();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQUEST_CHECKOUT && resultCode == RESULT_OK) {
                finishFragmentDetails();
            }
        } catch (Exception ex) {

        }
    }

}
