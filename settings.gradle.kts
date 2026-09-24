rootProject.name = "Thoon"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":shared")
include(":common:init")
include(":common:di")
include(":common:navigation")
include(":feature:chat:api")
include(":feature:chat:impl")
include(":feature:chats-list:api")
include(":feature:chats-list:impl")
include(":feature:settings:api")
include(":feature:settings:impl")
include(":feature:agent-tools")
include(":common:log")
include(":common:ui")
include(":common:markdown")
include(":common:agent:api")
include(":common:agent:tool-api")
include(":common:agent:impl")
include(":common:database")
include(":common:coroutines")
include(":common:serialization")
include(":common:shared-preferences:api")
include(":common:shared-preferences:impl")
include(":common:user-settings:api")
include(":common:user-settings:impl")
include(":common:network")
include(":feature:releaseless-tools:api")
include(":feature:releaseless-tools:impl")
include(":webApp")
