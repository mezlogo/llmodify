clean:
    ./gradlew clean

test:
    ./gradlew :app:test --rerun

format:
    ./gradlew :app:spotlessApply

compile:
    ./gradlew :app:compileKotlin

build:
    ./gradlew :app:installDist
