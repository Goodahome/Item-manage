# Generates project.pbxproj for ItemReminderTool target (run on any OS).
import hashlib
from pathlib import Path

FILES = [
    "ItemReminderToolApp.swift",
    "ContentView.swift",
    "AppRoute.swift",
    "MainShellView.swift",
    "Theme.swift",
    "AppSettingsStore.swift",
    "ModelContainer.swift",
    "PersistenceModels.swift",
    "Repositories.swift",
    "NetworkLayer.swift",
    "SyncManager.swift",
    "NotificationScheduler.swift",
    "BackgroundTaskManager.swift",
    "BarcodeScannerService.swift",
    "TensorFlowLiteService.swift",
    "WebDAVClient.swift",
    "ExcelService.swift",
    "OAuthAuthManager.swift",
    "Monetization.swift",
    "ScreensCore.swift",
    "ScreensSettings.swift",
]

ASSETS = "Assets.xcassets"


def uid(prefix: str) -> str:
    h = hashlib.md5(prefix.encode()).hexdigest().upper()
    return h[:8] + h[8:16] + h[16:24]


def main() -> None:
    root = uid("root")
    proj = uid("project")
    target = uid("target")
    sources_phase = uid("sources_phase")
    fw_phase = uid("fw_phase")
    res_phase = uid("res_phase")
    main_group = uid("main_group")
    prod_group = uid("prod_group")
    app_ref = uid("app_product")
    assets_ref = uid(ASSETS)
    assets_build = uid("build:" + ASSETS)
    proj_debug = uid("proj_debug")
    proj_release = uid("proj_release")
    tgt_debug = uid("tgt_debug")
    tgt_release = uid("tgt_release")
    proj_cfg_list = uid("projCfgList")
    tgt_cfg_list = uid("targetCfgList")

    file_refs = {f: uid("ref:" + f) for f in FILES}
    build_files = {f: uid("build:" + f) for f in FILES}

    lines = [
        "// !$*UTF8*$!",
        "{",
        "\tarchiveVersion = 1;",
        "\tclasses = {",
        "\t};",
        "\tobjectVersion = 56;",
        "\tobjects = {",
    ]

    lines.append("/* Begin PBXBuildFile section */")
    for f in FILES:
        lines.append(f"\t\t{build_files[f]} /* {f} in Sources */ = {{isa = PBXBuildFile; fileRef = {file_refs[f]} /* {f} */; }};")
    lines.append(f"\t\t{assets_build} /* {ASSETS} in Resources */ = {{isa = PBXBuildFile; fileRef = {assets_ref} /* {ASSETS} */; }};")
    lines.append("/* End PBXBuildFile section */")

    lines.append("/* Begin PBXFileReference section */")
    lines.append(f"\t\t{app_ref} /* ItemReminderTool.app */ = {{isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = ItemReminderTool.app; sourceTree = BUILT_PRODUCTS_DIR; }};")
    for f in FILES:
        lines.append(f"\t\t{file_refs[f]} /* {f} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = {f}; sourceTree = \"<group>\"; }};")
    lines.append(
        f"\t\t{assets_ref} /* {ASSETS} */ = {{isa = PBXFileReference; lastKnownFileType = folder.assetcatalog; path = {ASSETS}; sourceTree = \"<group>\"; }};"
    )
    lines.append("/* End PBXFileReference section */")

    # PBXFrameworksBuildPhase
    lines.append("/* Begin PBXFrameworksBuildPhase section */")
    lines.append(
        f"\t\t{fw_phase} /* Frameworks */ = {{isa = PBXFrameworksBuildPhase; buildActionMask = 2147483647; files = ( ); runOnlyForDeploymentPostprocessing = 0; }};"
    )
    lines.append("/* End PBXFrameworksBuildPhase section */")

    # PBXGroup
    lines.append("/* Begin PBXGroup section */")
    child_lines = "\n\t\t\t".join([f"{file_refs[f]} /* {f} */," for f in FILES])
    lines.append(
        f"\t\t{main_group} = {{isa = PBXGroup; children = (\n\t\t\t{child_lines}\n\t\t\t{assets_ref} /* {ASSETS} */,\n\t\t); path = ItemReminderTool; sourceTree = \"<group>\"; }};"
    )
    lines.append(f"\t\t{prod_group} /* Products */ = {{isa = PBXGroup; children = ({app_ref} /* ItemReminderTool.app */, ); name = Products; sourceTree = \"<group>\"; }};")
    lines.append(
        f"\t\t{root} = {{isa = PBXGroup; children = ({main_group} /* ItemReminderTool */, {prod_group} /* Products */, ); sourceTree = \"<group>\"; }};"
    )
    lines.append("/* End PBXGroup section */")

    # PBXNativeTarget
    lines.append("/* Begin PBXNativeTarget section */")
    lines.append(
        f"\t\t{target} /* ItemReminderTool */ = {{isa = PBXNativeTarget; buildConfigurationList = {tgt_cfg_list} /* Build configuration list for PBXNativeTarget \"ItemReminderTool\" */; buildPhases = ({sources_phase} /* Sources */, {fw_phase} /* Frameworks */, {res_phase} /* Resources */); buildRules = (); dependencies = (); name = ItemReminderTool; productName = ItemReminderTool; productReference = {app_ref} /* ItemReminderTool.app */; productType = \"com.apple.product-type.application\"; }};"
    )
    lines.append("/* End PBXNativeTarget section */")

    # PBXProject
    lines.append("/* Begin PBXProject section */")
    lines.append(
        f"\t\t{proj} /* Project object */ = {{isa = PBXProject; attributes = {{BuildIndependentTargetsInParallel = 1; LastSwiftUpdateCheck = 1600; LastUpgradeCheck = 1600; TargetAttributes = {{{target} = {{CreatedOnToolsVersion = 16.0; }}; }}; }}; buildConfigurationList = {proj_cfg_list} /* Build configuration list for PBXProject \"ItemReminderTool\" */; compatibilityVersion = \"Xcode 14.0\"; developmentRegion = en; hasScannedForEncodings = 0; knownRegions = (en, \"zh-Hans\", Base, ); mainGroup = {root}; productRefGroup = {prod_group} /* Products */; projectDirPath = \"\"; projectRoot = \"\"; targets = ({target} /* ItemReminderTool */, ); }};"
    )
    lines.append("/* End PBXProject section */")

    # PBXResourcesBuildPhase
    lines.append("/* Begin PBXResourcesBuildPhase section */")
    lines.append(
        f"\t\t{res_phase} /* Resources */ = {{isa = PBXResourcesBuildPhase; buildActionMask = 2147483647; files = ({assets_build} /* {ASSETS} in Resources */, ); runOnlyForDeploymentPostprocessing = 0; }};"
    )
    lines.append("/* End PBXResourcesBuildPhase section */")

    # PBXSourcesBuildPhase
    lines.append("/* Begin PBXSourcesBuildPhase section */")
    src_files = ", ".join([f"{build_files[f]} /* {f} in Sources */" for f in FILES])
    lines.append(
        f"\t\t{sources_phase} /* Sources */ = {{isa = PBXSourcesBuildPhase; buildActionMask = 2147483647; files = ({src_files}); runOnlyForDeploymentPostprocessing = 0; }};"
    )
    lines.append("/* End PBXSourcesBuildPhase section */")

    lines.append("/* Begin XCBuildConfiguration section */")
    lines.append(
        f"\t\t{proj_debug} /* Debug */ = {{isa = XCBuildConfiguration; buildSettings = {{ALWAYS_SEARCH_USER_PATHS = NO; ASSETCATALOG_COMPILER_GENERATE_SWIFT_ASSET_SYMBOL_EXTENSIONS = YES; CLANG_ANALYZER_NONNULL = YES; CLANG_ENABLE_MODULES = YES; CLANG_ENABLE_OBJC_ARC = YES; COPY_PHASE_STRIP = NO; DEBUG_INFORMATION_FORMAT = dwarf; ENABLE_STRICT_OBJC_MSGSEND = YES; ENABLE_TESTABILITY = YES; GCC_DYNAMIC_NO_PIC = NO; GCC_OPTIMIZATION_LEVEL = 0; GCC_PREPROCESSOR_DEFINITIONS = (\"DEBUG=1\", $(inherited), ); IPHONEOS_DEPLOYMENT_TARGET = 17.0; MTL_ENABLE_DEBUG_INFO = INCLUDE_SOURCE; ONLY_ACTIVE_ARCH = YES; SDKROOT = iphoneos; SWIFT_ACTIVE_COMPILATION_CONDITIONS = DEBUG; SWIFT_OPTIMIZATION_LEVEL = \"-Onone\"; }}; name = Debug; }};"
    )
    lines.append(
        f"\t\t{proj_release} /* Release */ = {{isa = XCBuildConfiguration; buildSettings = {{ALWAYS_SEARCH_USER_PATHS = NO; ASSETCATALOG_COMPILER_GENERATE_SWIFT_ASSET_SYMBOL_EXTENSIONS = YES; CLANG_ANALYZER_NONNULL = YES; CLANG_ENABLE_MODULES = YES; CLANG_ENABLE_OBJC_ARC = YES; COPY_PHASE_STRIP = NO; DEBUG_INFORMATION_FORMAT = dwarf-with-dsym; ENABLE_NS_ASSERTIONS = NO; ENABLE_STRICT_OBJC_MSGSEND = YES; IPHONEOS_DEPLOYMENT_TARGET = 17.0; MTL_ENABLE_DEBUG_INFO = NO; SDKROOT = iphoneos; SWIFT_COMPILATION_MODE = wholemodule; VALIDATE_PRODUCT = YES; }}; name = Release; }};"
    )
    lines.append(
        f"\t\t{tgt_debug} /* Debug */ = {{isa = XCBuildConfiguration; buildSettings = {{ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon; CODE_SIGN_STYLE = Automatic; CURRENT_PROJECT_VERSION = 1; DEVELOPMENT_TEAM = \"\"; ENABLE_PREVIEWS = YES; GENERATE_INFOPLIST_FILE = YES; INFOPLIST_KEY_BGTaskSchedulerPermittedIdentifiers = (\"com.itemremindertool.sync\"); INFOPLIST_KEY_CFBundleDisplayName = \"盒记\"; INFOPLIST_KEY_LSApplicationCategoryType = \"public.app-category.productivity\"; INFOPLIST_KEY_NSCameraUsageDescription = \"用于扫描条码与拍摄物品照片\"; INFOPLIST_KEY_NSFaceIDUsageDescription = \"用于解锁敏感信息\"; INFOPLIST_KEY_NSPhotoLibraryAddUsageDescription = \"用于保存导出文件\"; INFOPLIST_KEY_NSPhotoLibraryUsageDescription = \"用于选择物品图片\"; INFOPLIST_KEY_UIApplicationSceneManifest_Generation = YES; INFOPLIST_KEY_UIApplicationSupportsIndirectInputEvents = YES; INFOPLIST_KEY_UIBackgroundModes = (fetch, processing); INFOPLIST_KEY_UILaunchScreen_Generation = YES; INFOPLIST_KEY_UIStatusBarStyle = UIStatusBarStyleDefault; INFOPLIST_KEY_UISupportedInterfaceOrientations = UIInterfaceOrientationPortrait; INFOPLIST_KEY_UISupportedInterfaceOrientations_iPad = \"UIInterfaceOrientationPortrait UIInterfaceOrientationPortraitUpsideDown UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight\"; INFOPLIST_KEY_UISupportedInterfaceOrientations_iPhone = \"UIInterfaceOrientationPortrait UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight\"; INFOPLIST_KEY_NSUserNotificationsUsageDescription = \"用于在到期与库存提醒时发送通知\"; LD_RUNPATH_SEARCH_PATHS = (\"$(inherited)\", \"@executable_path/Frameworks\"); MARKETING_VERSION = 2.0.0; PRODUCT_BUNDLE_IDENTIFIER = com.example.itemremindertool.ios; PRODUCT_NAME = \"$(TARGET_NAME)\"; SWIFT_EMIT_LOC_STRINGS = YES; SWIFT_VERSION = 5.0; TARGETED_DEVICE_FAMILY = \"1,2\"; }}; name = Debug; }};"
    )
    lines.append(
        f"\t\t{tgt_release} /* Release */ = {{isa = XCBuildConfiguration; buildSettings = {{ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon; CODE_SIGN_STYLE = Automatic; CURRENT_PROJECT_VERSION = 1; DEVELOPMENT_TEAM = \"\"; ENABLE_PREVIEWS = YES; GENERATE_INFOPLIST_FILE = YES; INFOPLIST_KEY_BGTaskSchedulerPermittedIdentifiers = (\"com.itemremindertool.sync\"); INFOPLIST_KEY_CFBundleDisplayName = \"盒记\"; INFOPLIST_KEY_LSApplicationCategoryType = \"public.app-category.productivity\"; INFOPLIST_KEY_NSCameraUsageDescription = \"用于扫描条码与拍摄物品照片\"; INFOPLIST_KEY_NSFaceIDUsageDescription = \"用于解锁敏感信息\"; INFOPLIST_KEY_NSPhotoLibraryAddUsageDescription = \"用于保存导出文件\"; INFOPLIST_KEY_NSPhotoLibraryUsageDescription = \"用于选择物品图片\"; INFOPLIST_KEY_UIApplicationSceneManifest_Generation = YES; INFOPLIST_KEY_UIApplicationSupportsIndirectInputEvents = YES; INFOPLIST_KEY_UIBackgroundModes = (fetch, processing); INFOPLIST_KEY_UILaunchScreen_Generation = YES; INFOPLIST_KEY_UIStatusBarStyle = UIStatusBarStyleDefault; INFOPLIST_KEY_UISupportedInterfaceOrientations = UIInterfaceOrientationPortrait; INFOPLIST_KEY_UISupportedInterfaceOrientations_iPad = \"UIInterfaceOrientationPortrait UIInterfaceOrientationPortraitUpsideDown UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight\"; INFOPLIST_KEY_UISupportedInterfaceOrientations_iPhone = \"UIInterfaceOrientationPortrait UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight\"; INFOPLIST_KEY_NSUserNotificationsUsageDescription = \"用于在到期与库存提醒时发送通知\"; LD_RUNPATH_SEARCH_PATHS = (\"$(inherited)\", \"@executable_path/Frameworks\"); MARKETING_VERSION = 2.0.0; PRODUCT_BUNDLE_IDENTIFIER = com.example.itemremindertool.ios; PRODUCT_NAME = \"$(TARGET_NAME)\"; SWIFT_EMIT_LOC_STRINGS = YES; SWIFT_VERSION = 5.0; TARGETED_DEVICE_FAMILY = \"1,2\"; }}; name = Release; }};"
    )
    lines.append("/* End XCBuildConfiguration section */")

    # XCConfigurationList
    lines.append("/* Begin XCConfigurationList section */")
    lines.append(
        f"\t\t{proj_cfg_list} /* Build configuration list for PBXProject \"ItemReminderTool\" */ = {{isa = XCConfigurationList; buildConfigurations = ({proj_debug} /* Debug */, {proj_release} /* Release */, ); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release; }};"
    )
    lines.append(
        f"\t\t{tgt_cfg_list} /* Build configuration list for PBXNativeTarget \"ItemReminderTool\" */ = {{isa = XCConfigurationList; buildConfigurations = ({tgt_debug} /* Debug */, {tgt_release} /* Release */, ); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release; }};"
    )
    lines.append("/* End XCConfigurationList section */")

    lines.append("\t};")
    lines.append(f"\trootObject = {proj} /* Project object */;")
    lines.append("}")

    out = Path(__file__).parent / "ItemReminderTool.xcodeproj" / "project.pbxproj"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("\n".join(lines).replace("\t", "\t"), encoding="utf-8")
    print("Wrote", out)


if __name__ == "__main__":
    main()
