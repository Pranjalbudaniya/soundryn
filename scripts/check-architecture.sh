#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

# Rule 1: core/domain/build.gradle.kts has no project( and no Android plugin; core/domain/src has no import android. or import androidx.
if [ -f "core/domain/build.gradle.kts" ]; then
    if grep -q "project(" "core/domain/build.gradle.kts"; then
        echo "ARCH VIOLATION: core/domain/build.gradle.kts must not declare any project() dependencies" >&2
        exit 1
    fi
    if grep -q "com.android" "core/domain/build.gradle.kts"; then
        echo "ARCH VIOLATION: core/domain/build.gradle.kts must not apply any Android plugins" >&2
        exit 1
    fi
fi
if [ -d "core/domain/src" ]; then
    if grep -rn "import android\." core/domain/src 2>/dev/null; then
        echo "ARCH VIOLATION: core/domain/src must not import android.*" >&2
        exit 1
    fi
    if grep -rn "import androidx\." core/domain/src 2>/dev/null; then
        echo "ARCH VIOLATION: core/domain/src must not import androidx.*" >&2
        exit 1
    fi
fi

# Rule 2: No module except :app declares project(":core:data")
VIOLATIONS=$(grep -rn 'project(":core:data")' --include="build.gradle.kts" . | grep -v '^./app/' | grep -v '^app/' || true)
if [ -n "$VIOLATIONS" ]; then
    echo "ARCH VIOLATION: No module except :app may declare project(\":core:data\"): $VIOLATIONS" >&2
    exit 1
fi

# Rule 3: :core:design and any feature/* module: no project(":core:data") and no source import of com.soundryn.music.core.data
for dir in core/design feature/*; do
    if [ -d "$dir" ]; then
        if [ -f "$dir/build.gradle.kts" ] && grep -q 'project(":core:data")' "$dir/build.gradle.kts"; then
            echo "ARCH VIOLATION: $dir must not depend on :core:data" >&2
            exit 1
        fi
        if [ -d "$dir/src" ] && grep -rn "import com.soundryn.music.core.data" "$dir/src" 2>/dev/null; then
            echo "ARCH VIOLATION: $dir/src must not import com.soundryn.music.core.data" >&2
            exit 1
        fi
    fi
done

# Rule 4: :core:data has no project(":core:design"), no feature/*, no :app
if [ -f "core/data/build.gradle.kts" ]; then
    if grep -Eq 'project\("(:core:design|:feature:|:app)' "core/data/build.gradle.kts"; then
        echo "ARCH VIOLATION: :core:data must not depend on :core:design, feature modules, or :app" >&2
        exit 1
    fi
fi

# Rule 5: Suppression budget: 0. Count lines containing @Suppress, @SuppressLint, tools:ignore or lint-baseline under app, core, feature
BUDGET=0
SUPPRESS_COUNT=0
DIRS=()
for d in app core feature; do
    if [ -d "$d" ]; then
        DIRS+=("$d")
    fi
done

if [ ${#DIRS[@]} -gt 0 ]; then
    MATCHES=$(grep -rn --exclude-dir=build --exclude-dir=.gradle -E "@Suppress|@SuppressLint|tools:ignore|lint-baseline" "${DIRS[@]}" 2>/dev/null || true)
    if [ -n "$MATCHES" ]; then
        SUPPRESS_COUNT=$(echo "$MATCHES" | wc -l)
    fi
fi

echo "Suppression count: $SUPPRESS_COUNT (Budget: $BUDGET)"
if [ "$SUPPRESS_COUNT" -gt "$BUDGET" ]; then
    echo "ARCH VIOLATION: Suppression count $SUPPRESS_COUNT exceeds budget $BUDGET" >&2
    exit 1
fi

echo "ARCH OK"
