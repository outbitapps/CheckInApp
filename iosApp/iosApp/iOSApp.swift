import SwiftUI
import ComposeApp
import FirebaseCore
import FirebaseMessaging
import MapKit
import ActivityKit
class AppDelegate: NSObject, UIApplicationDelegate {

  func application(_ application: UIApplication,
                   didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {

      FirebaseApp.configure() //important
      
      //By default showPushNotification value is true.
      //When set showPushNotification to false foreground push  notification will not be shown.
      //You can still get notification content using #onPushNotification listener method.
      NotifierManager.shared.initialize(configuration: NotificationPlatformConfigurationIos(
            showPushNotification: true,
            askNotificationPermissionOnStart: true,
            notificationSoundName: nil)
      )
      
    return true
  }

  func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
      
  }
    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any]) async -> UIBackgroundFetchResult {
        NotifierManager.shared.onApplicationDidReceiveRemoteNotification(userInfo: userInfo)
              return UIBackgroundFetchResult.newData
    }
    let rinku = RinkuIos.init(deepLinkFilter: nil, deepLinkMapper: nil)
        func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
            print(url)
            rinku.onDeepLinkReceived(url: url.absoluteString)
            return true
        }
        
        func application(_ application: UIApplication, continue userActivity: NSUserActivity, restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {
            if userActivity.activityType == NSUserActivityTypeBrowsingWeb, let url = userActivity.webpageURL {
                let urlString = url.absoluteString
                rinku.onDeepLinkReceived(userActivity: userActivity)
            }
            return true
        }
    
}


@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    var body: some Scene {
        WindowGroup {
            ContentView().onOpenURL { url in
                let rinku = RinkuIos.init(deepLinkFilter: nil, deepLinkMapper: nil)
                print(url)
                rinku.onDeepLinkReceived(url: url.absoluteString)
            }
        }
    }
}



struct MapsView: View {
    var markers: [CIMapMarker]
    var dest: CILatLong
    var radius: Double
    var history: [CISessionLocationHistory]
    var points = [CLLocationCoordinate2D]()
    init(markers: [CIMapMarker], dest: CILatLong, radius: Double, history: [CISessionLocationHistory]) {
        self.markers = markers
        self.dest = dest
        self.radius = radius
        self.history = history
        history.forEach { info in
            print(info)
            points.append(CLLocationCoordinate2D(latitude: info.location.latitude, longitude: info.location.longitude))
        }
        
    }
    var body: some View {
            Map {
//                Marker(markerTitle, coordinate: CLLocationCoordinate2D(latitude: Double(pinLat), longitude: Double(pinLong)))
                ForEach(markers, id: \.self) { marker in
//                    Marker(marker.title, monogram: Text(marker.subtitle), coordinate: )
                    Annotation("", coordinate: CLLocationCoordinate2D(latitude: marker.loc.latitude, longitude: marker.loc.longitude)) {
                        MarkerAnnotationView(title: marker.title, subtitle: marker.subtitle)
                    }
                }
                MapPolyline(coordinates: points).stroke(.blue, lineWidth: 2.0)
                MapCircle(MKCircle(center: CLLocationCoordinate2D(latitude: dest.latitude, longitude: dest.longitude), radius: CLLocationDistance(Int(radius)))).foregroundStyle(Color.blue.opacity(0.5)).stroke(Color.blue)
            }
    }
}

struct MarkerAnnotationView: View {
    var title: String
    var subtitle: String
    @State var showingContent = false
    var body: some View {
//        if !showingContent {
//            VStack {
//                
//            }.frame(width: 200, height: 200).onTapGesture {
//                withAnimation(.spring) {
//                    showingContent = true
//                }
//            }
//        } else {
            ZStack {
                RoundedRectangle(cornerRadius: 15.0).foregroundStyle(.thinMaterial)
                VStack(alignment: .leading) {
                    Text(title).fontWeight(.semibold)
                    Text(subtitle)
                }.multilineTextAlignment(.leading).font(.caption2).padding(5)
            }.onTapGesture {
                withAnimation(.spring) {
                    showingContent = false
                }
            }.frame(width: 100)
//        }
    }
}
