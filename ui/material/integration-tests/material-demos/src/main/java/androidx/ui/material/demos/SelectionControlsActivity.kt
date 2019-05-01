/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.demos

import android.app.Activity
import android.os.Bundle
import androidx.ui.core.CraneWrapper
import androidx.ui.material.MaterialTheme
import com.google.r4a.composer
import com.google.r4a.setContent

open class SelectionControlsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                MaterialTheme {
                    SelectionsControlsDemo()
                }
            }
        }
    }
}
