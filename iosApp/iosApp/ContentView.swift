import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            mapUIViewController: { (markers: [CIMapMarker], history: [CISessionLocationHistory], dest: CILatLong, radius: KotlinDouble) -> UIViewController in
                return UIHostingController(rootView: MapsView(markers: markers, dest: dest, radius: radius.doubleValue, history: history))
                        }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}



