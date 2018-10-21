/*
 * This file is part of apt-creator, licensed under the MIT License (MIT).
 *
 * Copyright (c) Kenzie Togami <https://octyl.net>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import java.lang.Integer;
import java.lang.NullPointerException;
import java.lang.String;
import javax.annotation.Generated;
import javax.inject.Inject;
import javax.inject.Provider;

@Generated(
        value = "net.octyl.aptcreator.processor.AptCreatorGenerator",
        comments = "https://github.com/kenzierocks/apt-creator"
)
public final class MultiConstructorCreator {
    private final Provider<String> providedStrProvider;
    private final Provider<Integer> providedIntProvider;
    @Inject
    public MultiConstructorCreator(Provider<String> providedStrProvider, Provider<Integer> providedIntProvider) {
        this.providedStrProvider = checkNotNull(providedStrProvider, 0);
        this.providedIntProvider = checkNotNull(providedIntProvider, 1);
    }

    public MultiConstructor create() {
        return new MultiConstructor();
    }

    public MultiConstructor create(String constrTwo) {
        return new MultiConstructor(checkNotNull(constrTwo, 0));
    }

    public MultiConstructor create(double extra) {
        return new MultiConstructor(checkNotNull(providedStrProvider.get(), 0), extra);
    }

    public MultiConstructor create(int extra) {
        return new MultiConstructor(checkNotNull(providedIntProvider.get(), 0), extra);
    }

    public MultiConstructor create(long diff) {
        return new MultiConstructor(checkNotNull(providedIntProvider.get(), 0),
                checkNotNull(providedStrProvider.get(), 1),
                diff);
    }

    private static <T> T checkNotNull(T arg, int argIndex) {
        if (arg == null) {
            throw new NullPointerException("@GenerateCreator class was passed null to a non-null argument. Index: " + argIndex)
        }

        return arg;
    }
}
