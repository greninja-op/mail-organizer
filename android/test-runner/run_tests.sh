#!/usr/bin/env bash
set -e
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR"

JAVA="/home/hatch/jdk/jdk-17.0.20.1+1/bin/java"
ROBOLECTRIC_DEPS="/home/hatch/robolectric-deps"
STUBS="/tmp/mo-test-stubs"
CP_FILE="/tmp/mo_full_test_cp.txt"

if [ ! -f "$CP_FILE" ]; then
    echo "Classpath cache not found, generating..."
    # temporary task registration or fallback
    python3 -c "
with open('app/build.gradle.kts') as f:
    text = f.read()
snippet = '''
tasks.register(\"exportTestCp\") {
    dependsOn(\"compileDebugUnitTestKotlin\")
    doLast {
        val cp = tasks.named<Test>(\"testDebugUnitTest\").get().classpath.asPath
        file(\"/tmp/mo_full_test_cp.txt\").writeText(cp)
    }
}
'''
if 'exportTestCp' not in text:
    with open('app/build.gradle.kts', 'a') as f:
        f.write('\n' + snippet)
"
    ./gradlew :app:exportTestCp
    git checkout -- app/build.gradle.kts
fi

TEST_CLASSES=$(python3 -c "
import os
test_dir = 'app/build/tmp/kotlin-classes/debugUnitTest'
classes = []
for root, dirs, files in os.walk(test_dir):
    for f in files:
        if f.endswith('Test.class') and '$' not in f:
            rel = os.path.relpath(os.path.join(root, f), test_dir)
            cls = rel[:-6].replace('/', '.')
            classes.append(cls)
classes.sort()
print(' '.join(classes))
")

echo "Running full unit and integration test suite..."
"$JAVA" \
  -Drobolectric.dependency.dir="$ROBOLECTRIC_DEPS" \
  -cp "$STUBS:$(cat "$CP_FILE")" \
  org.junit.runner.JUnitCore $TEST_CLASSES
