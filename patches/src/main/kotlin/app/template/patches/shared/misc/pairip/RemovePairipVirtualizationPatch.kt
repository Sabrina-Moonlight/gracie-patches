package app.template.patches.shared.misc.pairip

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.util.returnEarly
import app.template.patches.shared.inputStreamFromBundledResource
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private val GLASS_WIDGETS = Compatibility(
    name = "Glass Widgets",
    packageName = "com.jndapp.glass.widgets",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x00BCD4,
    targets = listOf(AppTarget(version = "1.0.2", versionCode = 12)),
)

private const val PAIRIP_APPLICATION_CLASS = "Lcom/pairip/application/Application;"

private var isPairipPacked = false

/**
 * Detect Pairip native runtime and delete libpairipcore.so so the process
 * is not aborted by native signature checks after re-signing.
 */
private val removePairipNativeRuntimePatch = rawResourcePatch {
    execute {
        isPairipPacked = false
        val libRoot = get("lib")
        if (!libRoot.exists()) return@execute

        libRoot.listFiles()?.filter { it.isDirectory }?.forEach { architecture ->
            val runtime = architecture.resolve("libpairipcore.so")
            if (runtime.exists()) {
                runtime.delete()
                isPairipPacked = true
            }
        }
    }
}

private fun pairipBuildLines(packageName: String, versionCode: String, extension: String): List<String> {
    val name = "$packageName-$versionCode.$extension"
    val stream = inputStreamFromBundledResource("pairip", name)
        ?: throw PatchException("Missing pairip/$name under resources")
    return stream.bufferedReader().use { it.readLines().filter { line -> line.isNotBlank() } }
}

/**
 * Neutralises Pairip virtualization for Glass Widgets:
 * - blocks VMRunner static init and invoke
 * - strips attachBaseContext from Pairip Application wrapper
 * - replaces methods that only forward to the VM with safe empty bodies
 *
 * Full body recovery from embedded dex tables can be layered on later.
 */
val removePairipVirtualizationPatch = bytecodePatch(
    name = "Remove Pairip Virtualization",
    description = "Skips the local VM runtime and neutralises virtualized method stubs.",
    default = true,
) {
    compatibleWith(GLASS_WIDGETS)

    dependsOn(removePairipNativeRuntimePatch)

    execute {
        // Always try to neutralise VM entry points when Pairip classes exist.
        if (classDefByOrNull("Lcom/pairip/VMRunner;") != null) {
            runCatching { VMRunnerStaticInitializerFingerprint.method.returnEarly() }
            runCatching { VMRunnerInvokeFingerprint.method.returnEarly(null) }
        }

        if (classDefByOrNull(PAIRIP_APPLICATION_CLASS) != null) {
            val appClass = mutableClassDefBy(PAIRIP_APPLICATION_CLASS)
            appClass.virtualMethods.removeIf { it.name == "attachBaseContext" }
        }

        val listedMethods = runCatching {
            pairipBuildLines(
                packageMetadata.packageName,
                packageMetadata.versionCode.toString(),
                "stubs",
            ).toSet()
        }.getOrElse { emptySet() }

        // Replace any method that still calls into VMRunner with a safe return.
        classDefForEach { classDef ->
            if (classDef.type.startsWith("Lcom/pairip/")) return@classDefForEach
            val mutable = mutableClassDefBy(classDef.type)
            classDef.virtualizedMethods().forEach { method ->
                val descriptor = methodDescriptor(method)
                if (listedMethods.isNotEmpty() && descriptor !in listedMethods) return@forEach

                val target = mutable.methods.singleOrNull {
                    it.name == method.name &&
                        it.parameterTypes == method.parameterTypes &&
                        it.returnType == method.returnType
                } ?: return@forEach

                val impl = target.implementation ?: return@forEach
                val size = impl.instructions.size
                if (size > 0) {
                    target.removeInstructions(0, size)
                }

                val isStatic = AccessFlags.STATIC.isSet(target.accessFlags)
                when (target.returnType) {
                    "V" -> target.addInstructions(0, "return-void")
                    "Z", "B", "S", "C", "I" -> target.addInstructions(
                        0,
                        """
                            const/4 v0, 0x0
                            return v0
                        """.trimIndent(),
                    )
                    "J" -> target.addInstructions(
                        0,
                        """
                            const-wide/16 v0, 0x0
                            return-wide v0
                        """.trimIndent(),
                    )
                    "F" -> target.addInstructions(
                        0,
                        """
                            const/4 v0, 0x0
                            return v0
                        """.trimIndent(),
                    )
                    "D" -> target.addInstructions(
                        0,
                        """
                            const-wide/16 v0, 0x0
                            return-wide v0
                        """.trimIndent(),
                    )
                    else -> target.addInstructions(
                        0,
                        """
                            const/4 v0, 0x0
                            return-object v0
                        """.trimIndent(),
                    )
                }
            }
        }
    }
}
