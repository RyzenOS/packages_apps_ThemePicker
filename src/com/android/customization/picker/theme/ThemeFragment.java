/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.customization.picker.theme;

import static androidx.core.view.ViewCompat.LAYOUT_DIRECTION_RTL;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;

import com.android.customization.model.theme.ThemeBundle;
import com.android.customization.model.theme.ThemeManager;
import com.android.customization.widget.OptionSelectorController;
import com.android.customization.widget.PreviewPager;
import com.android.wallpaper.R;
import com.android.wallpaper.picker.ToolbarFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that contains the main UI for selecting and applying a ThemeBundle.
 */
public class ThemeFragment extends ToolbarFragment {

    public static ThemeFragment newInstance(CharSequence title, ThemeManager manager) {
        ThemeFragment fragment = new ThemeFragment();
        fragment.setManager(manager);
        fragment.setArguments(ToolbarFragment.createArguments(title));
        return fragment;
    }

    private RecyclerView mOptionsContainer;
    private OptionSelectorController mOptionsController;
    private ThemeManager mThemeManager;
    private ThemeBundle mSelectedTheme;
    private PagerAdapter mAdapter;
    private PreviewPager mPreviewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.fragment_theme_picker, container, /* attachToRoot */ false);
        setUpToolbar(view);
        mPreviewPager = view.findViewById(R.id.theme_preview_pager);
        mOptionsContainer = view.findViewById(R.id.options_container);
        setUpOptions();

        return view;
    }

    private void setManager(ThemeManager manager) {
        mThemeManager = manager;
    }

    private void createAdapter() {
        mAdapter = new ThemePreviewAdapter(mSelectedTheme);
        mPreviewPager.setAdapter(mAdapter);
    }

    private void setUpOptions() {
        mThemeManager.fetchOptions(options -> {
            mOptionsController = new OptionSelectorController(mOptionsContainer, options);

            mOptionsController.addListener(selected -> {
                mSelectedTheme = (ThemeBundle) selected;
                createAdapter();
            });
            mOptionsController.initOptions();
            for (ThemeBundle theme : options) {
                if (theme.isCurrentlySet()) {
                    mSelectedTheme = theme;
                }
            }
            // For development only, as there should always be a theme set.
            if (mSelectedTheme == null) {
                mSelectedTheme = options.get(0);
            }
            mOptionsController.setSelectedOption(mSelectedTheme);
        });
        createAdapter();
    }

    /**
     * Adapter class for mPreviewPager.
     * This is a ViewPager as it allows for a nice pagination effect (ie, pages snap on swipe,
     * we don't want to just scroll)
     */
    private class ThemePreviewAdapter extends PagerAdapter {

        private abstract class PreviewPage {
            @StringRes final int nameResId;
            @DrawableRes final int iconSrc;
            @LayoutRes final int contentLayoutRes;
            @ColorInt final int accentColor;

            CardView card;

            private PreviewPage(@StringRes int titleResId, @DrawableRes int iconSrc,
                    @LayoutRes int contentLayoutRes, @ColorInt int accentColor) {
                this.nameResId = titleResId;
                this.iconSrc = iconSrc;
                this.contentLayoutRes = contentLayoutRes;
                this.accentColor = accentColor;
            }

            public void setCard(CardView card) {
                this.card = card;
            }

            abstract void bindPreviewContent();
        }

        List<PreviewPage> mPages = new ArrayList<>();

        ThemePreviewAdapter(ThemeBundle theme) {
            mPages.add(new PreviewPage(R.string.preview_name_font, R.drawable.ic_font,
                    R.layout.preview_card_font_content, theme.getPreviewInfo().colorAccentLight) {
                @Override
                void bindPreviewContent() {
                    TextView title = card.findViewById(R.id.font_card_title);
                    title.setTypeface(theme.getPreviewInfo().headlineFontFamily);
                    TextView body = card.findViewById(R.id.font_card_body);
                    body.setTypeface(theme.getPreviewInfo().bodyFontFamily);
                }
            });
        }

        @Override
        public int getCount() {
            return mPages.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == ((PreviewPage)object).card;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            if (ViewCompat.getLayoutDirection(container) == LAYOUT_DIRECTION_RTL) {
                position = mPages.size() - 1 - position;
            }
            LayoutInflater inflater = LayoutInflater.from(getContext());
            CardView card = (CardView) inflater.inflate(R.layout.theme_preview_card,
                    container, false);
            PreviewPage page = mPages.get(position);

            TextView header = card.findViewById(R.id.theme_preview_card_header);
            header.setText(page.nameResId);
            header.setCompoundDrawablesWithIntrinsicBounds(0, page.iconSrc, 0, 0);
            header.setCompoundDrawableTintList(ColorStateList.valueOf(page.accentColor));

            ViewGroup body = card.findViewById(R.id.theme_preview_card_body_container);
            inflater.inflate(page.contentLayoutRes, body, true);

            page.setCard(card);
            page.bindPreviewContent();
            if (card.getParent() != null) {
                container.removeView(card);
            }
            container.addView(card);
            return page;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position,
                @NonNull Object object) {
            ((PreviewPage) object).card = null;
        }
    }
}
