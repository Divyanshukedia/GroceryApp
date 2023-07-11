package com.app.fresy.adapter;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.app.fresy.R;
import com.app.fresy.data.AppConfig;
import com.app.fresy.data.Constant;
import com.app.fresy.data.ThisApp;
import com.app.fresy.model.Category;
import com.app.fresy.model.CategoryList;
import com.app.fresy.model.CategorySection;
import com.app.fresy.model.Product;
import com.app.fresy.model.Setting;
import com.app.fresy.model.Slider;
import com.app.fresy.model.SliderList;
import com.app.fresy.model.Sorting;
import com.app.fresy.model.SortingSection;
import com.app.fresy.room.AppDatabase;
import com.app.fresy.room.DAO;
import com.app.fresy.room.table.CartEntity;
import com.app.fresy.utils.Tools;
import com.app.fresy.widget.SpacingItemDecoration;
import com.app.fresy.widget.SpacingItemHorizontal;

import java.util.ArrayList;
import java.util.List;

public class AdapterHome extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public enum ActionType {
        NORMAL, BUY, PLUS, MINUS
    }

    public enum OtherActionType {
        CATEGORY_ALL, CATEGORY_LESS, SORTING
    }

    private int index_slider = 0;
    private int index_category_section = 0, index_category = 0;
    private int index_product_section = 0, index_product = 0;

    private final int VIEW_PROGRESS = 0;
    private final int VIEW_ITEM_PRODUCT = 100;
    private final int VIEW_ITEM_SLIDER = 200;
    private final int VIEW_ITEM_CATEGORY = 300;
    private final int VIEW_ITEM_SORTING_SECTION = 400;
    private final int VIEW_ITEM_CATEGORY_SECTION = 500;

    private List items = new ArrayList<>();
    private CategoryList categoryList = new CategoryList();

    private boolean category_show_all = false;
    private boolean loading;
    private OnLoadMoreListener onLoadMoreListener;

    private Activity ctx;
    private Setting setting;
    private DAO db;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onProductClick(View view, Product obj, CartEntity cart, ActionType actionType, int position);

        void onSliderClick(View view, Slider obj, int position);

        void onCategoryClick(View view, Category obj, int position);

        void onOtherAction(View view, OtherActionType actionType, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.onItemClickListener = mItemClickListener;
    }

    public AdapterHome(Activity context, RecyclerView view) {
        ctx = context;
        setting = ThisApp.get().getSetting();
        db = AppDatabase.getDb(context).get();
        lastItemViewDetector(view);
    }

    public class ProductViewHolder extends RecyclerView.ViewHolder {
        public TextView name, brief, price, cart_count, original_price, sale_badge;
        public ImageView image;
        public AppCompatButton btn_buy;
        public ImageButton bt_cart_minus, bt_cart_plus;
        public View lyt_parent, lyt_counter, lyt_plus_minus;

        public ProductViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.name);
            brief = v.findViewById(R.id.brief);
            price = v.findViewById(R.id.price);
            original_price = v.findViewById(R.id.original_price);
            image = v.findViewById(R.id.image);
            sale_badge = v.findViewById(R.id.sale_badge);
            btn_buy = v.findViewById(R.id.btn_buy);
            lyt_counter = v.findViewById(R.id.lyt_counter);
            lyt_plus_minus = v.findViewById(R.id.lyt_plus_minus);
            bt_cart_minus = v.findViewById(R.id.bt_cart_minus);
            bt_cart_plus = v.findViewById(R.id.bt_cart_plus);
            cart_count = v.findViewById(R.id.cart_count);
            lyt_parent = v.findViewById(R.id.lyt_parent);
        }
    }

    public class CategoryViewHolder extends RecyclerView.ViewHolder {
        public RecyclerView recycler_view_category;

        public CategoryViewHolder(View v) {
            super(v);
            recycler_view_category = v.findViewById(R.id.recycler_view_category);
        }
    }

    public class SliderViewHolder extends RecyclerView.ViewHolder {
        public View lyt_parent;
        public ViewPager view_pager;
        public LinearLayout layout_dots;

        public SliderViewHolder(View v) {
            super(v);
            lyt_parent = v.findViewById(R.id.lyt_parent);
            layout_dots = v.findViewById(R.id.layout_dots);
            view_pager = v.findViewById(R.id.view_pager);
        }
    }

    public class SortingViewHolder extends RecyclerView.ViewHolder {
        public View lyt_parent;
        public TextView tv_sorting;

        public SortingViewHolder(View v) {
            super(v);
            lyt_parent = v.findViewById(R.id.lyt_parent);
            tv_sorting = v.findViewById(R.id.tv_sorting);
        }
    }

    public class CategorySectionViewHolder extends RecyclerView.ViewHolder {
        public View lyt_parent;
        public TextView tv_see_all;

        public CategorySectionViewHolder(View v) {
            super(v);
            lyt_parent = v.findViewById(R.id.lyt_parent);
            tv_see_all = v.findViewById(R.id.tv_see_all);
        }
    }


    public static class ProgressViewHolder extends RecyclerView.ViewHolder {
        public ProgressViewHolder(View v) {
            super(v);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        RecyclerView.ViewHolder vh;
        if (viewType == VIEW_ITEM_PRODUCT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
            vh = new ProductViewHolder(v);
        } else if (viewType == VIEW_ITEM_SLIDER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_horizontal_slider, parent, false);
            vh = new SliderViewHolder(v);
        } else if (viewType == VIEW_ITEM_CATEGORY) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_horizontal_category, parent, false);
            vh = new CategoryViewHolder(v);
        } else if (viewType == VIEW_ITEM_SORTING_SECTION) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_section_sorting, parent, false);
            vh = new SortingViewHolder(v);
        } else if (viewType == VIEW_ITEM_CATEGORY_SECTION) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_section_category, parent, false);
            vh = new CategorySectionViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading, parent, false);
            vh = new ProgressViewHolder(v);
        }
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position_old) {
        int position = holder.getBindingAdapterPosition();
        Object obj = items.get(position);

        if (holder instanceof ProgressViewHolder) {

        } else if (holder instanceof ProductViewHolder) {
            final Product product = (Product) obj;
            boolean variable = product.type.equals("variable");
            final CartEntity cart = variable ? db.getCartParent(product.id) : db.getCart(product.id);
            ProductViewHolder v = (ProductViewHolder) holder;
            v.name.setText(product.name);
            v.brief.setText(Tools.fromHtml(product.short_description));
            if (variable) {
                v.price.setText(Tools.fromHtml(product.price_html.trim()));
            } else {
                v.price.setText(Tools.getPrice(product.price));
            }
            if (product.sale_price.equals("")) {
                v.original_price.setVisibility(View.GONE);
                v.sale_badge.setVisibility(View.GONE);
            } else {
                v.original_price.setVisibility(View.VISIBLE);
                v.original_price.setText(Tools.getPrice(product.regular_price));
                v.original_price.setPaintFlags(v.original_price.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                v.sale_badge.setVisibility(View.VISIBLE);
                String percentage = Tools.getSalePercentage(ctx, product) + " " + ctx.getString(R.string.OFF);
                if (AppConfig.SHOW_SALE_PERCENTAGE) {
                    v.sale_badge.setText(percentage);
                }
            }

            if (product.images != null && product.images.size() > 0) {
                Tools.displayImage(ctx, v.image, product.images.get(0).src);
            }

            v.lyt_counter.setVisibility(cart == null ? View.GONE : View.VISIBLE);
            v.lyt_plus_minus.setVisibility(cart == null || variable ? View.GONE : View.VISIBLE);
            v.btn_buy.setVisibility(cart == null || variable ? View.VISIBLE : View.GONE);
            if (cart != null && !variable) {
                v.cart_count.setVisibility(View.VISIBLE);
                v.cart_count.setText(cart.getAmount() + "");
            } else {
                v.cart_count.setVisibility(View.GONE);
            }

            v.lyt_parent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onProductClick(view, product, cart, ActionType.NORMAL, position);
                }
            });

            v.btn_buy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onProductClick(view, product, cart, ActionType.BUY, position);
                }
            });

            v.bt_cart_minus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onProductClick(view, product, cart, ActionType.MINUS, position);
                }
            });

            v.bt_cart_plus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onProductClick(view, product, cart, ActionType.PLUS, position);
                }
            });

        } else if (holder instanceof SliderViewHolder) {
            SliderList sliderList = (SliderList) obj;
            final SliderViewHolder v = (SliderViewHolder) holder;
            final AdapterImageSlider adapterSlider = new AdapterImageSlider(ctx, new ArrayList<Slider>());
            adapterSlider.setItems(sliderList.sliders);
            v.view_pager.setAdapter(adapterSlider);

            v.view_pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int pos, float positionOffset, int positionOffsetPixels) {
                }

                @Override
                public void onPageSelected(int pos) {
                    addBottomDots(v.layout_dots, adapterSlider.getCount(), pos);
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }
            });

            adapterSlider.setOnItemClickListener(new AdapterImageSlider.OnItemClickListener() {
                @Override
                public void onItemClick(View view, Slider obj) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onSliderClick(view, obj, position);
                    }
                }
            });

            addBottomDots(v.layout_dots, adapterSlider.getCount(), 0);
            startAutoSlider(v.view_pager, adapterSlider.getCount());

        } else if (holder instanceof CategoryViewHolder) {
            CategoryList categoryList = (CategoryList) obj;
            CategoryViewHolder v = (CategoryViewHolder) holder;

            Tools.resetItemDecoration(v.recycler_view_category);
            if (AppConfig.CATEGORY_GRID) {
                v.recycler_view_category.addItemDecoration(new SpacingItemDecoration(Tools.getGridSpanCountCategory(ctx), Tools.dpToPx(ctx, 10), false, 0));
                v.recycler_view_category.setLayoutManager(new GridLayoutManager(ctx, Tools.getGridSpanCountCategory(ctx)));
                v.recycler_view_category.setHasFixedSize(true);
                v.recycler_view_category.setPadding(0, 0, 0, 0);
            } else {
                v.recycler_view_category.addItemDecoration(new SpacingItemHorizontal(Tools.dpToPx(ctx, 10)));
                v.recycler_view_category.setLayoutManager(new LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false));
            }
            //set data and list adapter
            AdapterCategory mAdapter = new AdapterCategory(ctx, categoryList.categories);
            v.recycler_view_category.setAdapter(mAdapter);

            mAdapter.setOnItemClickListener(new AdapterCategory.OnItemClickListener() {
                @Override
                public void onItemClick(View view, Category obj, int position) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onCategoryClick(view, obj, position);
                    }
                }
            });

        } else if (holder instanceof SortingViewHolder) {
            SortingSection sortingSection = (SortingSection) obj;
            SortingViewHolder v = (SortingViewHolder) holder;
            v.tv_sorting.setText(sortingSection.sorting.title);
            v.tv_sorting.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onOtherAction(v, OtherActionType.SORTING, position);
                    }
                }
            });
        } else if (holder instanceof CategorySectionViewHolder) {
            CategorySectionViewHolder v = (CategorySectionViewHolder) holder;
            v.tv_see_all.setText(category_show_all ? R.string.see_less : R.string.see_all);
            v.tv_see_all.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null) {
                        category_show_all = !category_show_all;
                        onItemClickListener.onOtherAction(v, category_show_all ? OtherActionType.CATEGORY_ALL : OtherActionType.CATEGORY_LESS, position);
                        notifyItemChanged(position);
                    }
                }
            });
        }

    }

    private void onProductClick(View v, Product p, CartEntity c, ActionType act, int pos) {
        if (onItemClickListener == null) return;
        onItemClickListener.onProductClick(v, p, c, act, pos);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (runnable != null) handler.removeCallbacks(runnable);
    }

    private Runnable runnable = null;
    private Handler handler = new Handler();

    private void startAutoSlider(final ViewPager viewPager, final int count) {
        //if(runnable != null) return;
        runnable = new Runnable() {
            @Override
            public void run() {
                int pos = viewPager.getCurrentItem();
                pos = pos + 1;
                if (pos >= count) pos = 0;
                viewPager.setCurrentItem(pos);
                handler.postDelayed(runnable, 3000);
            }
        };
        handler.postDelayed(runnable, 3000);
    }

    private void addBottomDots(LinearLayout layout_dots, int size, int current) {
        ImageView[] dots = new ImageView[size];
        layout_dots.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new ImageView(ctx);
            int width_height = 15;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(new ViewGroup.LayoutParams(width_height, width_height));
            params.setMargins(6, 0, 6, 0);
            dots[i].setLayoutParams(params);
            dots[i].setImageResource(R.drawable.shape_circle_outline);
            dots[i].setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
            layout_dots.addView(dots[i]);
        }

        if (dots.length > 0) {
            dots[current].setImageResource(R.drawable.shape_circle);
            dots[current].setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        Object obj = items.get(position);
        if (obj instanceof Product) {
            return VIEW_ITEM_PRODUCT;
        } else if (obj instanceof SliderList) {
            return VIEW_ITEM_SLIDER;
        } else if (obj instanceof CategoryList) {
            return VIEW_ITEM_CATEGORY;
        } else if (obj instanceof SortingSection) {
            return VIEW_ITEM_SORTING_SECTION;
        } else if (obj instanceof CategorySection) {
            return VIEW_ITEM_CATEGORY_SECTION;
        } else {
            return VIEW_PROGRESS;
        }
    }

    public void setCategory(List<Category> items) {
        index_category = this.items.size();
        categoryList = new CategoryList(items);
        this.items.add(categoryList);
    }

    public void setSlider(List<Slider> items) {
        index_slider = this.items.size();
        this.items.add(new SliderList(items));
    }

    public void setCategorySection(CategorySection object) {
        index_category_section = this.items.size();
        this.items.add(object);
    }

    public void setProductSection(SortingSection object) {
        index_product_section = this.items.size();
        this.items.add(object);
    }

    public void showAllCategory(List<Category> items) {
        if (this.items.get(index_category) instanceof CategoryList) {
            this.items.set(index_category, new CategoryList(items));
            notifyItemChanged(index_category);
        }
    }

    public void showLessCategory() {
        if (this.items.get(index_category) instanceof CategoryList) {
            this.items.set(index_category, categoryList);
            notifyItemChanged(index_category);
        }
    }

    public void updateSortingText(Sorting sorting) {
        if (this.items.get(index_product_section) instanceof SortingSection) {
            ((SortingSection) this.items.get(index_product_section)).sorting = sorting;
            notifyItemChanged(index_product_section);
        }
    }

    public void insertData(List<Product> items) {
        if (index_product_section == 0) index_product_section = this.items.size();
        setLoaded();
        int positionStart = getItemCount();
        int itemCount = items.size();
        this.items.addAll(items);
        notifyItemRangeInserted(positionStart, itemCount);
    }

    public void setLoaded() {
        loading = false;
        for (int i = 0; i < getItemCount(); i++) {
            if (items.get(i) == null) {
                items.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    public void setLoading() {
        this.items.add(null);
        notifyItemInserted(getItemCount() - 1);
        loading = true;
    }

    public void resetListData() {
        this.items = new ArrayList<>();
        notifyDataSetChanged();
    }

    public void clearListProduct() {
        int product_pos = 0;
        for (int i = 0; i < this.items.size(); i++) {
            if (this.items.get(i) instanceof Product) {
                product_pos = i;
                break;
            }
        }
        int count = items.size();
        items.subList(product_pos, count).clear();
        notifyItemRangeRemoved(product_pos, count);
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    private void lastItemViewDetector(RecyclerView recyclerView) {
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
            final GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    int lastPos = layoutManager.findLastVisibleItemPosition();
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) return;
                    boolean bottom = lastPos >= getItemCount() - Constant.PRODUCT_PER_REQUEST;
                    if (!loading && bottom && onLoadMoreListener != null) {
                        int current_page = getItemCount() / Constant.PRODUCT_PER_REQUEST;
                        onLoadMoreListener.onLoadMore(current_page);
                        loading = true;
                    }
                }
            });

            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    int type = getItemViewType(position);
                    int spanCount = layoutManager.getSpanCount();
                    return type == VIEW_ITEM_PRODUCT ? 1 : spanCount;
                }
            });
        }
    }

    public interface OnLoadMoreListener {
        void onLoadMore(int current_page);
    }
}