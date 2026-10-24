// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.utils;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;

public final class JavaScript {
    private JavaScript() { }

    public static void compileOrThrow(final String function) {
        try (Context context = Context.enter()) {
            context.setInterpretedMode(true);
            context.compileString(function, null, 1, null);
        }
    }

    public static String run(final String function,
                             final String functionName,
                             final String... parameters) {
        try (Context context = Context.enter()) {
            context.setInterpretedMode(true);
            final ScriptableObject scope = context.initSafeStandardObjects();
            context.evaluateString(scope, function, functionName, 1, null);
            final Function callable = (Function) scope.get(functionName, scope);
            final Object result = callable.call(context, scope, scope, parameters);
            return result.toString();
        }
    }
}
