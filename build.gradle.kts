plugins {
    application
}

repositories {
    mavenCentral()
}

val gdxVersion = "1.14.2"

dependencies {
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    runtimeOnly("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")

    // FreeType gera a BitmapFont a partir de um .ttf no tamanho exato da janela,
    // em vez de escalar uma fonte pronta e borrar.
    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    runtimeOnly("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")

    // Backend headless: da um Gdx.files funcional ao teste, sem janela nem OpenGL.
    testImplementation("com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // Gradle 9 nao coloca mais o launcher no classpath de teste automaticamente.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

// assets/ tambem entra no jar como resource. Gdx.files.internal procura primeiro no
// diretorio atual e depois no classpath, entao com isso a distribuicao funciona de
// qualquer pasta, nao so de dentro de assets/.
sourceSets.main {
    resources.srcDir("assets")
}

application {
    mainClass = "labs.breakout.DesktopLauncher"
}

// Os arquivos de assets/ sao lidos com Gdx.files.internal("levels/01.txt"), ou seja,
// relativos ao working dir. Por isso rodamos o jogo com assets/ como raiz.
tasks.named<JavaExec>("run") {
    workingDir = file("assets")
}

// Para o installDist: assets/ vai para dentro da distribuicao e o script roda de la.
distributions {
    main {
        contents {
            from("assets") { into("assets") }
        }
    }
}

// O jar ganha Main-Class e Class-Path no manifesto. Com isso ele roda sozinho
// (`java -jar lib/breakout-2d.jar`) e, principalmente, o jpackage consegue montar o
// classpath a partir dele - sem Class-Path o app empacotado abre e morre em
// NoClassDefFoundError.
tasks.jar {
    manifest {
        attributes(
            "Main-Class" to application.mainClass,
            "Class-Path" to configurations.runtimeClasspath.map { classpath ->
                classpath.joinToString(" ") { it.name }
            }
        )
    }
}

// Resolvido na configuracao, fora da task: dentro do bloco de uma Task `extensions`
// seria o da propria task, e tocar em `project` durante a execucao quebra o
// configuration cache (sera erro no Gradle 10).
private val jpackageTool = project.extensions.getByType<JavaToolchainService>()
    .launcherFor(java.toolchain)
    .map { it.metadata.installationPath.file("bin/jpackage").asFile.absolutePath }
private val jpackageInput = layout.buildDirectory.dir("install/${project.name}/lib")
private val jpackageOutput = layout.buildDirectory.dir("jpackage")
private val jpackageMainJar = "${project.name}.jar"
private val jpackageMainClass = application.mainClass

/**
 * Executavel autocontido em build/jpackage/, com JRE embutido: quem receber nao precisa
 * ter Java instalado. `./gradlew packageApp`
 */
tasks.register<Exec>("packageApp") {
    group = "distribution"
    description = "Gera um app autocontido (JRE embutido) em build/jpackage"
    dependsOn(tasks.named("installDist"))

    doFirst {
        // jpackage se recusa a escrever por cima de um destino existente.
        jpackageOutput.get().asFile.deleteRecursively()
        commandLine(
            jpackageTool.get(),
            "--type", "app-image",
            "--name", "Breakout2D",
            "--app-version", "1.0",
            "--input", jpackageInput.get().asFile.absolutePath,
            "--main-jar", jpackageMainJar,
            "--main-class", jpackageMainClass.get(),
            "--dest", jpackageOutput.get().asFile.absolutePath
            // No macOS, LWJGL3 exige ainda: "--java-options", "-XstartOnFirstThread"
        )
    }
}

tasks.test {
    useJUnitPlatform()
    // Mesma raiz do jogo, para que Gdx.files.internal("levels/01.txt") resolva no teste.
    workingDir = file("assets")
    testLogging { events("passed", "failed", "skipped") }
}
