filler_cp="$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)"
/home/user/.jdks/axiomjdk-25.0.1/bin/javac --release 24 -cp "$filler_cp" -d /tmp/seeker-step15-3-fillers.HXdOad/classes /tmp/seeker-step15-3-fillers.HXdOad/Step153FillersProbe.java
/home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -cp "/tmp/seeker-step15-3-fillers.HXdOad/classes:$filler_cp" ru.homyakin.seeker.game.battle.Step153FillersProbe /tmp/seeker-step15-3-fillers.HXdOad/screen
