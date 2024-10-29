## CheckIn (name WIP)
CheckInApp aims to recreate the iOS Check In experience (exclusive to iPhone users) for everybody with Kotlin Multiplatform.

CheckInApp is very unstable at the moment. For instance, if it cannot connect to the server, it will crash. This is an easy fix, but I just have not gotten around to it yet.

Speaking of the server, it is NOT in the `server/` directory. I was initially planning to write the server with Ktor using the built-in template with KMP but I found it sort of complicated, and I have more experience with Swift & Vapor. The server source can be found at [outbitapps/CheckInServer](https://github.com/outbitapps/CheckInServer/). If you do not want to host the server yourself, you can connect to the main instance at [check.paytondev.cloud](). To change the server you're using, change the URL at [controllers/CIManager.kt:44](https://github.com/outbitapps/CheckInApp/blob/main/composeApp/src/commonMain/kotlin/com/paytondeveloper/checkintest/controllers/CIManager.kt#L443) in the baseURL variable
