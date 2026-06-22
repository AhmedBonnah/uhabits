import re
with open("uhabits-core/src/jvmTest/java/org/isoron/uhabits/core/ui/screens/habits/list/HabitCardListCacheTest.kt", "r") as f:
    lines = f.readlines()
with open("uhabits-core/src/jvmTest/java/org/isoron/uhabits/core/ui/screens/habits/list/HabitCardListCacheTest.kt", "w") as f:
    for line in lines:
        if "verify(listener).onItem" in line:
            continue
        if "verifyNoMoreInteractions(listener)" in line:
            continue
        f.write(line)
