import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            mapUIViewController: { (pinLat: KotlinFloat, pinLong: KotlinFloat, destLat: KotlinFloat, destLong: KotlinFloat, radius: KotlinDouble,markerTitle: String) -> UIViewController in
                return UIHostingController(rootView: MapsView(pinLat: pinLat.floatValue, pinLong: pinLong.floatValue, destLat: destLat.floatValue, destLong: destLong.floatValue, radius: radius.doubleValue, markerTitle: markerTitle))
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



