/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.vectordrawable.graphics.drawable.tests;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.InflateException;

import androidx.animation.AnimationTestRule;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.vectordrawable.graphics.drawable.Animatable2;
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable;
import androidx.vectordrawable.seekable.test.R;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class SeekableAnimatedVectorDrawableTest {

    @ClassRule
    public static AnimationTestRule animationRule = new AnimationTestRule();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static final float PIXEL_ERROR_THRESHOLD = 0.3f;
    private static final float PIXEL_DIFF_THRESHOLD = 0.03f;
    private static final float PIXEL_DIFF_COUNT_THRESHOLD = 0.1f;

    private static final int IMAGE_WIDTH = 64;
    private static final int IMAGE_HEIGHT = 64;

    @Test
    @UiThreadTest
    public void inflate() throws Throwable {
        final Context context = ApplicationProvider.getApplicationContext();
        final Resources resources = context.getResources();
        final Resources.Theme theme = context.getTheme();
        XmlPullParser parser = resources.getXml(R.drawable.animation_vector_drawable_grouping_1);
        AttributeSet attrs = Xml.asAttributeSet(parser);

        int type;
        do {
            type = parser.next();
        } while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT);
        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        Bitmap bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        final SeekableAnimatedVectorDrawable avd =
                SeekableAnimatedVectorDrawable.createFromXmlInner(resources, parser, attrs, theme);
        avd.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        bitmap.eraseColor(0);
        avd.draw(canvas);
        int sunColor = bitmap.getPixel(IMAGE_WIDTH / 2, IMAGE_HEIGHT / 2);
        int earthColor = bitmap.getPixel(IMAGE_WIDTH * 3 / 4 + 2, IMAGE_HEIGHT / 2);
        assertThat(sunColor).isEqualTo(0xFFFF8000);
        assertThat(earthColor).isEqualTo(0xFF5656EA);
    }

    @Test
    @UiThreadTest
    public void registerCallback() {
        final SeekableAnimatedVectorDrawable avd = createAvd();

        final AtomicBoolean started = new AtomicBoolean(false);
        final AtomicBoolean ended = new AtomicBoolean(false);
        avd.registerAnimationCallback(new Animatable2.AnimationCallback() {
            @Override
            public void onAnimationStart(@NonNull Drawable drawable) {
                started.set(true);
            }

            @Override
            public void onAnimationEnd(@NonNull Drawable drawable) {
                ended.set(true);
            }
        });

        assertThat(started.get()).isFalse();
        assertThat(ended.get()).isFalse();

        avd.start();
        assertThat(started.get()).isTrue();
        assertThat(ended.get()).isFalse();

        animationRule.advanceTimeBy(1000L);
        assertThat(started.get()).isTrue();
        assertThat(ended.get()).isTrue();
    }

    @Test
    @UiThreadTest
    public void clearCallback() {
        final SeekableAnimatedVectorDrawable avd = createAvd();
        avd.registerAnimationCallback(createFailingCallback());
        avd.clearAnimationCallbacks();
        avd.start();
        animationRule.advanceTimeBy(1000L);
    }

    @Test
    @UiThreadTest
    public void unregisterCallback() {
        final SeekableAnimatedVectorDrawable avd = createAvd();
        final Animatable2.AnimationCallback callback = createFailingCallback();
        avd.registerAnimationCallback(callback);
        final boolean removed = avd.unregisterAnimationCallback(callback);
        assertThat(removed).isTrue();
        avd.start();
        animationRule.advanceTimeBy(1000L);
    }

    @Test
    @UiThreadTest
    public void constantState() {
        final SeekableAnimatedVectorDrawable avd = createAvd();
        // SAVD does not support ConstantState.
        assertThat(avd.getConstantState()).isNull();
    }

    @Test
    @UiThreadTest
    public void mutate() {
        final SeekableAnimatedVectorDrawable avd = createAvd();
        final Drawable mutated = avd.mutate();
        // SAVD does not support mutate.
        assertThat(mutated).isSameInstanceAs(avd);
    }

    @Test
    @UiThreadTest
    public void renderCorrectness() {
        final Bitmap bitmap = Bitmap.createBitmap(
                IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888
        );
        final Canvas canvas = new Canvas(bitmap);

        final SeekableAnimatedVectorDrawable avd = SeekableAnimatedVectorDrawable.create(
                ApplicationProvider.getApplicationContext(),
                R.drawable.animation_vector_drawable_circle
        );
        assertThat(avd).isNotNull();
        avd.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        avd.start();

        // First make sure the content is drawn into the bitmap.
        // Then save the first frame as the golden images.
        bitmap.eraseColor(0);
        avd.draw(canvas);
        assertThat(bitmap.getPixel(IMAGE_WIDTH / 2, IMAGE_WIDTH / 2)).isNotEqualTo(0);
        final Bitmap firstFrame = Bitmap.createBitmap(bitmap);

        // Now compare the following frames with the 1st frames. Expect some minor difference like
        // Anti-Aliased edges, so the compare is fuzzy.
        for (int i = 0; i < 5; i++) {
            animationRule.advanceTimeBy(16L);
            bitmap.eraseColor(0);
            avd.draw(canvas);
            compareImages(firstFrame, bitmap);
        }
    }

    @Test
    @UiThreadTest
    public void animateColor() {
        final Bitmap bitmap = Bitmap.createBitmap(
                IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888
        );
        final Canvas canvas = new Canvas(bitmap);

        final SeekableAnimatedVectorDrawable avd = SeekableAnimatedVectorDrawable.create(
                ApplicationProvider.getApplicationContext(),
                R.drawable.animated_color_fill
        );
        assertThat(avd).isNotNull();
        avd.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        avd.draw(canvas);
        assertThat(bitmap.getPixel(0, 0)).isEqualTo(Color.RED);
        assertThat(bitmap.getPixel(IMAGE_WIDTH / 2, IMAGE_HEIGHT / 2)).isEqualTo(Color.RED);

        avd.start();

        final ArrayList<Integer> historicalRed = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            animationRule.advanceTimeBy(100L);
            avd.draw(canvas);
            final int strokeColor = bitmap.getPixel(0, 0);
            assertThat(Color.blue(strokeColor)).isEqualTo(0);
            assertThat(Color.green(strokeColor)).isEqualTo(0);
            final int fillColor = bitmap.getPixel(IMAGE_WIDTH / 2, IMAGE_HEIGHT / 2);
            assertThat(Color.blue(fillColor)).isEqualTo(0);
            assertThat(Color.green(fillColor)).isEqualTo(0);
            historicalRed.add(Color.red(fillColor));
        }
        assertThat(historicalRed).isInOrder(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2 - o1;
            }
        });
    }

    @Test
    @UiThreadTest
    public void pathMorphing() {
        testPathMorphing(R.drawable.animation_path_morphing_rect);
        testPathMorphing(R.drawable.animation_path_morphing_rect2);
        testPathMorphing(R.drawable.animation_path_motion_rect);
    }

    @Test
    @UiThreadTest
    public void pathMorphing_exception() {
        expectedException.expect(InflateException.class);
        SeekableAnimatedVectorDrawable.create(
                ApplicationProvider.getApplicationContext(),
                R.drawable.animation_path_morphing_rect_exception
        );
    }

    private SeekableAnimatedVectorDrawable createAvd() {
        final SeekableAnimatedVectorDrawable avd = SeekableAnimatedVectorDrawable.create(
                ApplicationProvider.getApplicationContext(),
                R.drawable.animation_vector_drawable_grouping_1 // Duration: 50 ms
        );
        assertThat(avd).isNotNull();
        avd.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        return avd;
    }

    private Animatable2.AnimationCallback createFailingCallback() {
        return new Animatable2.AnimationCallback() {
            @Override
            public void onAnimationStart(@NonNull Drawable drawable) {
                fail("This callback should not be invoked.");
            }

            @Override
            public void onAnimationEnd(@NonNull Drawable drawable) {
                fail("This callback should not be invoked.");
            }
        };
    }

    private void testPathMorphing(@DrawableRes int resId) {
        final Bitmap bitmap = Bitmap.createBitmap(
                IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888
        );
        final Canvas canvas = new Canvas(bitmap);

        final SeekableAnimatedVectorDrawable avd = SeekableAnimatedVectorDrawable.create(
                ApplicationProvider.getApplicationContext(),
                resId
        );
        assertThat(avd).isNotNull();
        avd.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        avd.draw(canvas);
        assertThat(bitmap.getPixel(IMAGE_WIDTH / 2, IMAGE_WIDTH / 2)).isEqualTo(Color.RED);

        final AtomicBoolean ended = new AtomicBoolean(false);
        avd.registerAnimationCallback(new Animatable2.AnimationCallback() {
            @Override
            public void onAnimationEnd(@NonNull Drawable drawable) {
                ended.set(true);
            }
        });

        avd.start();
        animationRule.advanceTimeBy(1000L);
        assertThat(ended.get()).isTrue();
        bitmap.eraseColor(0);
        avd.draw(canvas);
        assertThat(bitmap.getPixel(IMAGE_WIDTH / 2, IMAGE_WIDTH / 2)).isEqualTo(Color.TRANSPARENT);
    }

    /**
     * Utility function for fuzzy image comparison between 2 bitmap. Fails if the difference is
     * bigger than a threshold.
     */
    private void compareImages(Bitmap ideal, Bitmap given) {
        int idealWidth = ideal.getWidth();
        int idealHeight = ideal.getHeight();

        assertThat(idealWidth).isEqualTo(given.getWidth());
        assertThat(idealHeight).isEqualTo(given.getHeight());

        int totalDiffPixelCount = 0;
        float totalPixelCount = idealWidth * idealHeight;
        for (int x = 0; x < idealWidth; x++) {
            for (int y = 0; y < idealHeight; y++) {
                int idealColor = ideal.getPixel(x, y);
                int givenColor = given.getPixel(x, y);
                if (idealColor == givenColor) {
                    continue;
                }

                float totalError = 0;
                totalError += Math.abs(Color.red(idealColor) - Color.red(givenColor));
                totalError += Math.abs(Color.green(idealColor) - Color.green(givenColor));
                totalError += Math.abs(Color.blue(idealColor) - Color.blue(givenColor));
                totalError += Math.abs(Color.alpha(idealColor) - Color.alpha(givenColor));

                assertThat(totalError / 1024.f).isAtMost(PIXEL_ERROR_THRESHOLD);

                if ((totalError / 1024.0f) >= PIXEL_DIFF_THRESHOLD) {
                    totalDiffPixelCount++;
                }
            }
        }
        assertThat(totalDiffPixelCount / totalPixelCount).isAtMost(PIXEL_DIFF_COUNT_THRESHOLD);
    }
}