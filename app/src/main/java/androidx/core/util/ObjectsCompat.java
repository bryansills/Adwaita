/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.core.util;

import android.support.annotation.Nullable;

public class ObjectsCompat {
    /**
     * Returns the result of calling {@code toString} on the first argument if the first argument
     * is not {@code null} and returns the second argument otherwise.
     *
     * @param o an object
     * @param nullDefault string to return if the first argument is {@code null}
     * @return the result of calling {@code toString} on the first argument if it is not {@code
     * null} and the second argument otherwise.
     */
    public static @Nullable String toString(@Nullable Object o, @Nullable String nullDefault) {
        return (o != null) ? o.toString() : nullDefault;
    }
}
